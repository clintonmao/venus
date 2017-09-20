/*
 * Copyright 2008-2108 amoeba.meidusa.com 
 * 
 * 	This program is free software; you can redistribute it and/or modify it under the terms of 
 * the GNU AFFERO GENERAL PUBLIC LICENSE as published by the Free Software Foundation; either version 3 of the License, 
 * or (at your option) any later version. 
 * 
 * 	This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU AFFERO GENERAL PUBLIC LICENSE for more details. 
 * 	You should have received a copy of the GNU AFFERO GENERAL PUBLIC LICENSE along with this program; 
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package com.meidusa.venus.client.factory.xml;

import com.meidusa.toolkit.common.bean.BeanContext;
import com.meidusa.toolkit.common.bean.BeanContextBean;
import com.meidusa.toolkit.common.bean.config.ConfigurationException;
import com.meidusa.venus.RpcException;
import com.meidusa.venus.VenusContext;
import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.client.factory.ServiceFactory;
import com.meidusa.venus.client.factory.xml.config.ClientRemoteConfig;
import com.meidusa.venus.client.factory.xml.config.ServiceConfig;
import com.meidusa.venus.client.factory.xml.config.VenusClientConfig;
import com.meidusa.venus.client.factory.xml.support.ClientBeanContext;
import com.meidusa.venus.client.factory.xml.support.ClientBeanUtilsBean;
import com.meidusa.venus.client.factory.xml.support.ServiceDefinedBean;
import com.meidusa.venus.client.factory.xml.support.ServiceFactoryBean;
import com.meidusa.venus.client.proxy.InvokerInvocationHandler;
import com.meidusa.venus.digester.DigesterRuleParser;
import com.meidusa.venus.exception.*;
import com.meidusa.venus.io.packet.PacketConstant;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.RegisterContext;
import com.meidusa.venus.util.FileWatchdog;
import com.meidusa.venus.util.NetUtil;
import com.meidusa.venus.util.VenusBeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.RuleSet;
import org.apache.commons.digester.xmlrules.FromXmlRuleSet;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于xml配置服务工厂
 */
public class XmlServiceFactory implements ServiceFactory,ApplicationContextAware, InitializingBean, BeanFactoryPostProcessor {

    private static Logger logger = LoggerFactory.getLogger(ServiceFactory.class);

    /**
     * 配置文件列表
     */
    private Resource[] configFiles;

    /**
     * 服务配置映射表
     */
    private Map<Class<?>, ServiceConfig> serviceConfigMap = new HashMap<Class<?>, ServiceConfig>();

    /**
     * 服务实例映射表
     */
    private Map<Class<?>, ServiceDefinedBean> serviceMap = new HashMap<Class<?>, ServiceDefinedBean>();

    /**
     * 服务实例映射表
     */
    private Map<String, ServiceDefinedBean> serviceNameMap = new HashMap<String, ServiceDefinedBean>();

    private boolean shutdown = false;

    private boolean needPing = false;

    private boolean inited = false;

    private VenusExceptionFactory venusExceptionFactory;

    private ApplicationContext applicationContext;

    private BeanContext beanContext;

    /**
     * 注册中心
     */
    private Register register;

    //private boolean enableReload = false;
    //private int asyncExecutorSize = 10;
    //private boolean enableAsync = true;
    //private ResourceLoader resourceLoader = new DefaultResourceLoader();
    //private Timer reloadTimer = new Timer();

    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> t) {
        if (shutdown) {
            throw new IllegalStateException("service factory has been shutdown");
        }
        ServiceDefinedBean object = serviceMap.get(t);
        
        if(object == null){
        	throw new ServiceNotFoundException(t.getName() +" not defined");
        }
        
        return (T) object.getService();
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getService(String name,Class<T> t) {
        if (shutdown) {
            throw new IllegalStateException("service factory has been shutdown");
        }
        ServiceDefinedBean object = serviceNameMap.get(name);
        
        if(object == null){
        	throw new ServiceNotFoundException(t.getName() +" not defined");
        }
        
        return (T) object.getService();
    }


    @SuppressWarnings("unchecked")
    public void afterPropertiesSet() throws Exception {
    	if(inited){
    		return;
    	}
    	inited = true;

    	//初始化应用上下文
    	initContext();

        //初始化注册中心
        initRegister();

        //初始化配置
        initConfiguration();

        /*__RELOAD:
        {
            if (enableReload) {
                File[] files = new File[this.configFiles.length];
                for (int i = 0; i < this.configFiles.length; i++) {
                    try {
                        files[i] = ResourceUtils.getFile(configFiles[i].trim());
                    } catch (FileNotFoundException e) {
                        logger.warn(e.getMessage());
                        enableReload = false;
                        logger.warn("venus serviceFactory configuration reload disabled!");
                        break __RELOAD;
                    }
                }
                VenusFileWatchdog dog = new VenusFileWatchdog(files);
                dog.setDelay(1000 * 10);
                dog.start();
            }
        }*/
    }

    /**
     * 初始化应用上下文
     */
    void initContext(){
        logger.trace("current Venus Client id=" + PacketConstant.VENUS_CLIENT_ID);

        if (venusExceptionFactory == null) {
            XmlVenusExceptionFactory xmlVenusExceptionFactory = new XmlVenusExceptionFactory();
            //3.0.8版本将采用自动扫描的方式获得 exception 相关的配置
            //xmlVenusExceptionFactory.setConfigFiles(new String[] { "classpath:com/meidusa/venus/exception/VenusSystemException.xml" });
            xmlVenusExceptionFactory.init();
            this.venusExceptionFactory = xmlVenusExceptionFactory;
        }

        if(applicationContext != null){
            VenusContext.getInstance().setApplicationContext(applicationContext);
        }
        beanContext = new ClientBeanContext(applicationContext!= null ?applicationContext.getAutowireCapableBeanFactory(): null);
        BeanContextBean.getInstance().setBeanContext(beanContext);
        if(beanContext != null){
            VenusContext.getInstance().setBeanContext(beanContext);
        }
        VenusBeanUtilsBean.setInstance(new ClientBeanUtilsBean(new ConvertUtilsBean(), new PropertyUtilsBean(), beanContext));
    }

    /**
     * 获取注册中心
     * @return
     */
    void initRegister(){
        Register register = RegisterContext.getInstance().getRegister();//MysqlRegister.getInstance(true,null);
        if(register == null){
            throw new RpcException("init register failed.");
        }
        this.register = register;
    }

    /**
     * 初始化配置
     * @throws Exception
     */
    private synchronized void initConfiguration() throws Exception {
        //加载客户端配置信息
        VenusClientConfig venusClientConfig = parseClientConfig();

        //初始化service实例
        for (ServiceConfig serviceConfig : venusClientConfig.getServiceConfigs()) {
            initService(serviceConfig,venusClientConfig);
        }
        /*
        for (Map.Entry<Class<?>, ServiceDefinedBean> entry : serviceMap.entrySet()) {
            Class<?> key = entry.getKey();
            ServiceDefinedBean source = entry.getValue();
            ServiceDefinedBean target = this.serviceMap.get(key);
            if (target != null) {
                //target.getMessageHandler().setBioConnPool(source.getMessageHandler().getBioConnPool());
                //target.getMessageHandler().setNioConnPool(source.getMessageHandler().getNioConnPool());
                //target.getMessageHandler().setSerializeType((byte) source.getMessageHandler().getSerializeType());
            } else {
                this.serviceMap.put(key, source);
            }
        }

        this.serviceConfigMap = serviceConfigMap;
        //reloadTimer.schedule(new ClosePoolTask(oldPools), 1000 * 30);
        */
    }

    /**
     * 初始化service
     * @param serviceConfig
     */
    void initService(ServiceConfig serviceConfig, VenusClientConfig venusClientConfig) {
        //初始化服务代理
        initServiceProxy(serviceConfig,venusClientConfig);

        //订阅服务
        subscribleService(serviceConfig);
    }

    /**
     * 初始化服务代理
     * @param serviceConfig
     * @param venusClientConfig
     */
    void initServiceProxy(ServiceConfig serviceConfig, VenusClientConfig venusClientConfig) {
        //创建服务代理invocation
        InvokerInvocationHandler invocationHandler = new InvokerInvocationHandler();
        invocationHandler.setServiceInterface(serviceConfig.getType());
        //若配置静态地址，以静态为先
        if(StringUtils.isNotEmpty(serviceConfig.getRemote()) || StringUtils.isNotEmpty(serviceConfig.getIpAddressList())){
            ClientRemoteConfig remoteConfig = getRemoteConfig(serviceConfig,venusClientConfig);
            invocationHandler.setRemoteConfig(remoteConfig);
        }else{
            invocationHandler.setRegister(register);
        }
        invocationHandler.setVenusExceptionFactory(this.getVenusExceptionFactory());
        invocationHandler.setServiceFactory(this);
        //TODO 确认相关属性功能
        /*
        invocationHandler.setNioConnPool(tuple.right);
        invocationHandler.setBioConnPool(tuple.left);
        invocationHandler.setConnector(this.connector);
        invocationHandler.setMessageHandler(this.handler);
        invocationHandler.setContainer(this.container);
        if (remoteConfig != null && remoteConfig.getAuthenticator() != null) {
            invocationHandler.setSerializeType(remoteConfig.getAuthenticator().getSerializeType());
        }
        */

        //创建服务代理
        Object object = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{serviceConfig.getType()}, invocationHandler);

        for (Method method : serviceConfig.getType().getMethods()) {
            Endpoint endpoint = method.getAnnotation(Endpoint.class);
            if (endpoint != null) {
                Class[] eclazz = method.getExceptionTypes();
                for (Class clazz : eclazz) {
                    if (venusExceptionFactory != null && CodedException.class.isAssignableFrom(clazz)) {
                        venusExceptionFactory.addException(clazz);
                    }
                }
            }
        }

        serviceConfigMap.put(serviceConfig.getType(), serviceConfig);
        ServiceDefinedBean defined = new ServiceDefinedBean(serviceConfig.getBeanName(),serviceConfig.getType(),object, invocationHandler);
        if (serviceConfig.getBeanName() != null) {
            serviceNameMap.put(serviceConfig.getBeanName(), defined);
        }else{
            serviceMap.put(serviceConfig.getType(), defined);
        }
    }

    /**
     * 订阅服务
     */
    void subscribleService(ServiceConfig serviceConfig){
        //若配置静态地址，则跳过
        if(StringUtils.isNotEmpty(serviceConfig.getRemote()) || StringUtils.isNotEmpty(serviceConfig.getIpAddressList())){
            return;
        }
        String serviceInterfaceName = null;
        if(serviceConfig.getType() != null){
            serviceInterfaceName = serviceConfig.getType().getName();
        }
        String serivceName = serviceConfig.getBeanName();//TODO serviceName从注释中获取
        String version = "0.0.0";//TODO
        String consumerHost = NetUtil.getLocalIp();

        String subscribleUrl = String.format(
                "subscrible://%s/%s?version=%s&host=%s",
                serviceInterfaceName,
                serivceName,
                version,
                consumerHost
                );
        com.meidusa.venus.URL url = com.meidusa.venus.URL.parse(subscribleUrl);
        logger.info("sbuscrible service:{}",url);
        register.subscrible(url);
    }


    /**
     * 获取静态地址配置信息
     * @param serviceConfig
     * @param venusClientConfig
     * @return
     */
    ClientRemoteConfig getRemoteConfig(ServiceConfig serviceConfig, VenusClientConfig venusClientConfig){
        if(StringUtils.isNotEmpty(serviceConfig.getRemote())){
            ClientRemoteConfig remoteConfig = venusClientConfig.getRemoteConfigMap().get(serviceConfig.getRemote());
            if(remoteConfig == null){
                throw new ConfigurationException(String.format("remoteConfig %  not found.",serviceConfig.getRemote()));
            }
            return remoteConfig;
        }
        if(StringUtils.isNotEmpty(serviceConfig.getIpAddressList())){
            ClientRemoteConfig remoteConfig = ClientRemoteConfig.newInstace(serviceConfig.getIpAddressList());
            return remoteConfig;
        }
        return null;
    }

    /**
     * 解析客户端配置信息
     * @return
     */
    VenusClientConfig parseClientConfig(){
        VenusClientConfig clientConfig = new VenusClientConfig();
        for (Resource configFile : configFiles) {
            // configFile = (String) ConfigUtil.filte(configFile);
            URL url = this.getClass().getResource("venusClientRule.xml");
            if (url == null) {
                throw new VenusConfigException("venusClientRule.xml not found!,pls rebuild venus!");
            }
            RuleSet ruleSet = new FromXmlRuleSet(url, new DigesterRuleParser());
            Digester digester = new Digester();
            digester.setValidating(false);
            digester.addRuleSet(ruleSet);

            try {
                //resourceLoader.getResource(configFile.trim())
                InputStream is = configFile.getInputStream();
                try{
                    VenusClientConfig venus = (VenusClientConfig) digester.parse(is);
                    for (ServiceConfig config : venus.getServiceConfigs()) {
                        if (config.getType() == null) {
                            logger.error("Service type can not be null:" + configFile);
                            throw new ConfigurationException("Service type can not be null:" + configFile);
                        }
                    }
                    clientConfig.getRemoteConfigMap().putAll(venus.getRemoteConfigMap());
                    clientConfig.getServiceConfigs().addAll(venus.getServiceConfigs());
                }finally{
                    if(is != null){
                        is.close();
                    }
                }
            } catch (Exception e) {
                throw new ConfigurationException("can not parser xml:" + configFile, e);
            }
        }
        return clientConfig;
    }


    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		// register to resolvable dependency container
		//BeanFactory beanFactory = applicationContext.getAutowireCapableBeanFactory();
        if (beanFactory instanceof ConfigurableListableBeanFactory) {
            ConfigurableListableBeanFactory cbf = beanFactory;
            for (Map.Entry<Class<?>, ServiceDefinedBean> entry : serviceMap.entrySet()) {
                //cbf.registerResolvableDependency(entry.getKey(), entry.getValue().left);
                final Object bean = entry.getValue().getService();
                if(beanFactory instanceof BeanDefinitionRegistry){
                	BeanDefinitionRegistry reg = (BeanDefinitionRegistry)beanFactory;
                	GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
                	beanDefinition.setBeanClass(ServiceFactoryBean.class);
                	beanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
                	ConstructorArgumentValues args = new ConstructorArgumentValues();
                	args.addIndexedArgumentValue(0,bean);
                	args.addIndexedArgumentValue(1,entry.getValue().getClazz());
                	beanDefinition.setConstructorArgumentValues(args);
                	
                	String beanName = entry.getValue().getClazz().getName()+"#0";
            		beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
            		reg.registerBeanDefinition(beanName, beanDefinition);
                }
            }

			for (Map.Entry<String, ServiceDefinedBean> entry : serviceNameMap.entrySet()) {
				final Object bean = entry.getValue().getService();
				if (beanFactory instanceof BeanDefinitionRegistry) {
					String beanName = entry.getValue().getBeanName();
					
					BeanDefinitionRegistry reg = (BeanDefinitionRegistry) beanFactory;
					GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
					beanDefinition.setBeanClass(ServiceFactoryBean.class);
					beanDefinition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, beanName));
					beanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
					ConstructorArgumentValues args = new ConstructorArgumentValues();
					args.addIndexedArgumentValue(0, bean);
					args.addIndexedArgumentValue(1, entry.getValue().getClazz());
					beanDefinition.setConstructorArgumentValues(args);
					
					beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_NAME);
					reg.registerBeanDefinition(beanName, beanDefinition);

				}
			}
        }
    }

    class VenusFileWatchdog extends FileWatchdog {

        protected VenusFileWatchdog(File... file) {
            super(file);
        }

        @Override
        protected void doOnChange() {
            try {
                XmlServiceFactory.this.initConfiguration();
            } catch (Exception e) {
                XmlServiceFactory.logger.error("reload configuration error", e);
            }
        }

    }

    public void destroy() {
        if (shutdown) {
            return;
        }
        shutdown = true;
    }

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

    public ServiceConfig getServiceConfig(Class<?> type) {
        return serviceConfigMap.get(type);
    }

    public boolean isNeedPing() {
        return needPing;
    }

    public void setNeedPing(boolean needPing) {
        this.needPing = needPing;
    }

    public VenusExceptionFactory getVenusExceptionFactory() {
        return venusExceptionFactory;
    }

    public void setVenusExceptionFactory(VenusExceptionFactory venusExceptionFactory) {
        this.venusExceptionFactory = venusExceptionFactory;
    }

    public Resource[] getConfigFiles() {
        return configFiles;
    }

    public void setConfigFiles(Resource... configFiles) {
        this.configFiles = configFiles;
    }

}
