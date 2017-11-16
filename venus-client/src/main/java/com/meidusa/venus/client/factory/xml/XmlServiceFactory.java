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
import com.meidusa.venus.*;
import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.annotations.Service;
import com.meidusa.venus.client.factory.InvokerInvocationHandler;
import com.meidusa.venus.client.factory.xml.config.ClientRemoteConfig;
import com.meidusa.venus.client.factory.xml.config.ReferenceService;
import com.meidusa.venus.client.factory.xml.config.VenusClientConfig;
import com.meidusa.venus.client.factory.xml.support.ClientBeanContext;
import com.meidusa.venus.client.factory.xml.support.ClientBeanUtilsBean;
import com.meidusa.venus.client.factory.xml.support.ServiceDefinedBean;
import com.meidusa.venus.client.invoker.venus.VenusClientInvoker;
import com.meidusa.venus.exception.*;
import com.meidusa.venus.io.packet.PacketConstant;
import com.meidusa.venus.metainfo.AnnotationUtil;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.VenusRegisteException;
import com.meidusa.venus.registry.VenusRegistryFactory;
import com.meidusa.venus.support.VenusContext;
import com.meidusa.venus.util.FileWatchdog;
import com.meidusa.venus.util.NetUtil;
import com.meidusa.venus.util.VenusBeanUtilsBean;
import com.meidusa.venus.util.VenusLoggerFactory;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
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
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 基于xml配置服务工厂
 */
public class XmlServiceFactory implements ServiceFactory,InitializingBean,BeanFactoryPostProcessor,ApplicationContextAware {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    /**
     * 配置文件列表
     */
    private Resource[] configFiles;

    /**
     * 服务配置映射表
     */
    private Map<Class<?>, ReferenceService> serviceConfigMap = new HashMap<Class<?>, ReferenceService>();

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

    private ApplicationContext applicationContext;

    private BeanContext beanContext;

    private Application application;

    /**
     * 注册中心工厂
     */
    private VenusRegistryFactory venusRegistryFactory;

    public XmlServiceFactory(){
        Application.addServiceFactory(this);
    }

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

        //初始化配置
        initConfiguration();
    }

    /**
     * 初始化应用上下文
     */
    void initContext(){
        if(logger.isDebugEnabled()){
            logger.debug("current Venus Client id=" + PacketConstant.VENUS_CLIENT_ID);
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
     * 初始化配置
     * @throws Exception
     */
    private synchronized void initConfiguration() throws Exception {
        //加载客户端配置信息
        VenusClientConfig venusClientConfig = parseClientConfig();

        //初始化service实例
        for (ReferenceService serviceConfig : venusClientConfig.getReferenceServices()) {
            initService(serviceConfig,venusClientConfig);
        }

        //TODO 设置序列化类型
        //target.getMessageHandler().setSerializeType((byte) source.getMessageHandler().getSerializeType());
    }

    /**
     * 初始化service
     * @param serviceConfig
     */
    void initService(ReferenceService serviceConfig, VenusClientConfig venusClientConfig) {
        //初始化服务代理
        initServiceProxy(serviceConfig,venusClientConfig);

        //若走注册中心，则订阅服务
        if(isNeedSubscrible(serviceConfig)){
            try {
                subscribleService(serviceConfig);
            } catch (Exception e) {
                if(exceptionLogger.isErrorEnabled()){
                    exceptionLogger.error("subscrible service failed,will retry.",e);
                }
            }
        }
    }

    /**
     * 判断是否
     * @param serviceConfig
     * @return
     */
    boolean isNeedSubscrible(ReferenceService serviceConfig){
        //若直连，则不订阅
        if(StringUtils.isNotEmpty(serviceConfig.getRemote()) || StringUtils.isNotEmpty(serviceConfig.getIpAddressList())){
            return false;
        }

        //若注册中心未定义，则不订阅
        return !(venusRegistryFactory == null || venusRegistryFactory.getRegister() == null);
    }
    /**
     * 初始化服务代理
     * @param referenceService
     * @param venusClientConfig
     */
    void initServiceProxy(ReferenceService referenceService, VenusClientConfig venusClientConfig) {
        //创建服务代理invocation
        InvokerInvocationHandler invocationHandler = new InvokerInvocationHandler();
        invocationHandler.setServiceInterface(referenceService.getClzType());
        //若配置静态地址，以静态为先
        if(StringUtils.isNotEmpty(referenceService.getRemote()) || StringUtils.isNotEmpty(referenceService.getIpAddressList())){
            ClientRemoteConfig remoteConfig = getRemoteConfig(referenceService,venusClientConfig);
            invocationHandler.setRemoteConfig(remoteConfig);
        }else{
            if(venusRegistryFactory == null || venusRegistryFactory.getRegister() == null){
                throw new VenusConfigException("venus register not config.");
            }
            invocationHandler.setRegister(venusRegistryFactory.getRegister());
        }
        invocationHandler.setReferenceService(referenceService);
        invocationHandler.setServiceFactory(this);
        /*
        if (remoteConfig != null && remoteConfig.getAuthenticator() != null) {
            invocationHandler.setSerializeType(remoteConfig.getAuthenticator().getSerializeType());
        }
        */

        //创建服务代理
        Object object = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{referenceService.getClzType()}, invocationHandler);

        VenusExceptionFactory venusExceptionFactory = XmlVenusExceptionFactory.getInstance();
        for (Method method : referenceService.getClzType().getMethods()) {
            Endpoint endpoint = method.getAnnotation(Endpoint.class);
            if (endpoint != null) {
                Service service = AnnotationUtil.getAnnotation(method.getDeclaringClass().getAnnotations(), Service.class);
                if(service != null){
                    referenceService.setBeanName(service.name());
                    referenceService.setVersion(service.version());
                }

                Class[] eclazz = method.getExceptionTypes();
                for (Class clazz : eclazz) {
                    if (venusExceptionFactory != null && CodedException.class.isAssignableFrom(clazz)) {
                        venusExceptionFactory.addException(clazz);
                    }
                }
            }
        }

        serviceConfigMap.put(referenceService.getClzType(), referenceService);
        ServiceDefinedBean defined = new ServiceDefinedBean(referenceService.getBeanName(),referenceService.getClzType(),object, invocationHandler);
        if (referenceService.getBeanName() != null) {
            serviceNameMap.put(referenceService.getBeanName(), defined);
        }else{
            serviceMap.put(referenceService.getClzType(), defined);
        }
    }


    /**
     * 订阅服务
     */
    void subscribleService(ReferenceService referenceConfig){
        //若配置静态地址，则跳过
        if(StringUtils.isNotEmpty(referenceConfig.getRemote()) || StringUtils.isNotEmpty(referenceConfig.getIpAddressList())){
            return;
        }
        String appName = application.getName();
        String serviceInterfaceName = "null";
        if(referenceConfig.getType() != null){
            serviceInterfaceName = referenceConfig.getClzType().getName();
        }
        String serivceName = referenceConfig.getBeanName();
        //int version = VenusConstants.VERSION_DEFAULT;
        String consumerHost = NetUtil.getLocalIp();

        StringBuffer buf = new StringBuffer();
        buf.append("/").append(serviceInterfaceName);
        buf.append("/").append(serivceName);
        buf.append("?application=").append(appName);
        buf.append("&host=").append(consumerHost);
        String subscribleUrl = buf.toString();
        /*
        String subscribleUrl = String.format(
                "subscrible://%s/%s?version=%s&application=%s&host=%s",
                serviceInterfaceName,
                serivceName,
                versionx,
                appName,
                consumerHost
                );
        */
        com.meidusa.venus.URL url = com.meidusa.venus.URL.parse(subscribleUrl);
        venusRegistryFactory.getRegister().subscrible(url);
    }


    /**
     * 获取静态地址配置信息
     * @param serviceConfig
     * @param venusClientConfig
     * @return
     */
    ClientRemoteConfig getRemoteConfig(ReferenceService serviceConfig, VenusClientConfig venusClientConfig){
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
     * 基于xstream解析客户端配置
     * @since 4.0
     * @return
     */
    VenusClientConfig parseClientConfig(){
        VenusClientConfig clientConfig = new VenusClientConfig();

        XStream xStream = new XStream();
        xStream.autodetectAnnotations(true);
        xStream.processAnnotations(VenusClientConfig.class);
        xStream.processAnnotations(ReferenceService.class);

        try {
            for (Resource configFile : configFiles) {
                VenusClientConfig venusClientConfig = (VenusClientConfig) xStream.fromXML(configFile.getURL());
                for (ReferenceService referenceService : venusClientConfig.getReferenceServices()) {
                    String interfaceType = referenceService.getType();
                    if (interfaceType == null) {
                        throw new VenusConfigException("Service type can not be null:" + configFile);
                    }
                    try {
                        Class<?> clzType = Class.forName(interfaceType);
                        referenceService.setClzType(clzType);
                    } catch (ClassNotFoundException e) {
                        throw new VenusConfigException("service interface class not found:" + interfaceType);
                    }
                }
                if(venusClientConfig.getReferenceServices() != null){
                    clientConfig.getReferenceServices().addAll(venusClientConfig.getReferenceServices());
                }
                if(venusClientConfig.getRemoteConfigMap() != null){
                    clientConfig.getRemoteConfigMap().putAll(venusClientConfig.getRemoteConfigMap());
                }
            }
        } catch (Exception e) {
            throw new VenusConfigException("parse venus client config failed" + e);
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

    @Override
    public void destroy() {
        if (shutdown) {
            return;
        }

        //反订阅
        if(venusRegistryFactory != null && venusRegistryFactory.getRegister() != null){
            Register register = venusRegistryFactory.getRegister();
            Set<URL> subscribleUrls = register.getSubscribleUrls();
            if(register != null && CollectionUtils.isNotEmpty(subscribleUrls)){
                for(URL url:subscribleUrls){
                    try {
                        register.unsubscrible(url);
                    } catch (VenusRegisteException e) {
                        if(exceptionLogger.isErrorEnabled()){
                            String errorMsg = String.format("unsubscrible url:%s failed.",url);
                            exceptionLogger.error(errorMsg,e);
                        }
                    }
                }
            }
        }

        shutdown = true;
    }

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

    public ReferenceService getServiceConfig(Class<?> type) {
        return serviceConfigMap.get(type);
    }

    public boolean isNeedPing() {
        return needPing;
    }

    public void setNeedPing(boolean needPing) {
        this.needPing = needPing;
    }

    public Resource[] getConfigFiles() {
        return configFiles;
    }

    public void setConfigFiles(Resource... configFiles) {
        this.configFiles = configFiles;
    }

    public VenusRegistryFactory getVenusRegistryFactory() {
        return venusRegistryFactory;
    }

    public void setVenusRegistryFactory(VenusRegistryFactory venusRegistryFactory) {
        this.venusRegistryFactory = venusRegistryFactory;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }
}
