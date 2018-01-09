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

import com.meidusa.fastjson.JSON;
import com.meidusa.toolkit.common.bean.BeanContext;
import com.meidusa.toolkit.common.bean.BeanContextBean;
import com.meidusa.toolkit.common.bean.config.ConfigurationException;
import com.meidusa.venus.VenusApplication;
import com.meidusa.venus.ServiceFactory;
import com.meidusa.venus.ServiceFactoryBean;
import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.annotations.Service;
import com.meidusa.venus.client.factory.AbstractServiceFactory;
import com.meidusa.venus.client.factory.InvokerInvocationHandler;
import com.meidusa.venus.client.factory.xml.config.ClientRemoteConfig;
import com.meidusa.venus.client.factory.xml.config.ReferenceService;
import com.meidusa.venus.client.factory.xml.config.VenusClientConfig;
import com.meidusa.venus.client.factory.xml.support.ClientBeanContext;
import com.meidusa.venus.client.factory.xml.support.ClientBeanUtilsBean;
import com.meidusa.venus.client.factory.xml.support.ServiceDefinedBean;
import com.meidusa.venus.exception.*;
import com.meidusa.venus.io.packet.PacketConstant;
import com.meidusa.venus.metainfo.AnnotationUtil;
import com.meidusa.venus.monitor.VenusMonitorFactory;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.VenusRegistryFactory;
import com.meidusa.venus.support.VenusContext;
import com.meidusa.venus.util.NetUtil;
import com.meidusa.venus.util.VenusBeanUtilsBean;
import com.meidusa.venus.util.VenusLoggerFactory;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于xml配置服务工厂
 */
public class XmlServiceFactory extends AbstractServiceFactory implements ServiceFactory,InitializingBean,BeanFactoryPostProcessor,ApplicationContextAware {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    /**
     * 配置文件列表
     */
    private Resource[] configFiles;

    /**
     * 服务实例映射表
     */
    private Map<Class<?>, ServiceDefinedBean> serviceMap = new HashMap<Class<?>, ServiceDefinedBean>();

    /**
     * 服务实例映射表，建议不再使用此模式，统一走clz KEY模式，存在包不同但类名情况
     */
    //private Map<String, ServiceDefinedBean> serviceNameMap = new HashMap<String, ServiceDefinedBean>();

    private boolean shutdown = false;

    private boolean inited = false;

    private ApplicationContext applicationContext;

    private BeanContext beanContext;

    //应用配置
    private VenusApplication venusApplication;

    //注册中心工厂
    private VenusRegistryFactory venusRegistryFactory;

    //监听中心配置
    private VenusMonitorFactory venusMonitorFactory;

    public XmlServiceFactory(){
        VenusApplication.addServiceFactory(this);
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
        ServiceDefinedBean object = serviceMap.get(t);
        
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

    	valid();

    	//初始化应用上下文
    	initContext();

        //初始化配置
        initConfiguration();
    }

    /**
     * 校验
     */
    void valid(){
        if(venusApplication == null){
            throw new VenusConfigException("venusApplication not config.");
        }
        if(venusRegistryFactory == null || venusRegistryFactory.getRegister() == null){
            if(logger.isWarnEnabled()){
                logger.warn("venusRegistryFactory not enabled,will disable service subscrible.");
            }
        }
        if(venusMonitorFactory == null){
            if(logger.isWarnEnabled()){
                logger.warn("venusMonitorFactory not enabled,will disable monitor reporte.");
            }
        }
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
    private void initConfiguration() throws Exception {
        //加载客户端配置信息
        VenusClientConfig venusClientConfig = parseClientConfig();

        if(CollectionUtils.isEmpty(venusClientConfig.getReferenceServices())){
            if(logger.isWarnEnabled()){
                logger.warn("not config reference provider services.");
            }
            return;
        }

        //初始化service实例
        for (ReferenceService serviceConfig : venusClientConfig.getReferenceServices()) {
            initService(serviceConfig,venusClientConfig);
        }

        //加载注册信息
        if(venusRegistryFactory != null && venusRegistryFactory.getRegister() != null){
            venusRegistryFactory.getRegister().load();
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
            if(logger.isWarnEnabled()){
                logger.warn("direct connect provider,will skip subscrible service.");
            }
            return false;
        }

        //若注册中心未定义，则不订阅
        if(venusRegistryFactory == null || venusRegistryFactory.getRegister() == null){
            if(logger.isWarnEnabled()){
                logger.warn("venusRegistryFactory not config,will skip subscrible service.");
            }
        }
        return true;
    }
    /**
     * 初始化服务代理
     * @param referenceService
     * @param venusClientConfig
     */
    void initServiceProxy(ReferenceService referenceService, VenusClientConfig venusClientConfig) {
        //创建服务代理invocation
        if(logger.isInfoEnabled()){
            logger.info("init service proxy:{}.",referenceService.getServiceInterface().getName());
        }
        //若地址或注册中心都未配置，则抛异常
        if(StringUtils.isEmpty(referenceService.getRemote()) && StringUtils.isEmpty(referenceService.getIpAddressList())){
            if(venusRegistryFactory == null || venusRegistryFactory.getRegister() == null){
                throw new VenusConfigException("init serivce proxy failed,ipAddressList and venusRegistryFactory not config.");
            }
        }

        InvokerInvocationHandler invocationHandler = new InvokerInvocationHandler();
        invocationHandler.setServiceInterface(referenceService.getServiceInterface());
        //若配置静态地址，以静态为先
        if(StringUtils.isNotEmpty(referenceService.getRemote()) || StringUtils.isNotEmpty(referenceService.getIpAddressList())){
            ClientRemoteConfig remoteConfig = getRemoteConfig(referenceService,venusClientConfig);
            invocationHandler.setRemoteConfig(remoteConfig);
        }else{
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
        Object serviceProxyObject = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{referenceService.getServiceInterface()}, invocationHandler);

        ServiceDefinedBean serviceDefinedBean = new ServiceDefinedBean();
        serviceDefinedBean.setName(referenceService.getName());
        serviceDefinedBean.setServiceName(referenceService.getServiceName());
        serviceDefinedBean.setServiceInterface(referenceService.getServiceInterface());
        serviceDefinedBean.setService(serviceProxyObject);
        serviceDefinedBean.setHandler(invocationHandler);
        serviceMap.put(referenceService.getServiceInterface(), serviceDefinedBean);
    }


    /**
     * 订阅服务
     */
    void subscribleService(ReferenceService referenceService){
        //若配置静态地址，则跳过
        if(StringUtils.isNotEmpty(referenceService.getRemote()) || StringUtils.isNotEmpty(referenceService.getIpAddressList())){
            return;
        }
        String appName = venusApplication.getName();
        String serviceInterfaceName = "null";
        if(referenceService.getType() != null){
            serviceInterfaceName = referenceService.getServiceInterface().getName();
        }
        String serivceName = referenceService.getServiceName();
        //int version = VenusConstants.VERSION_DEFAULT;
        String consumerHost = NetUtil.getLocalIp();

        StringBuffer buf = new StringBuffer();
        buf.append("/").append(serviceInterfaceName);
        buf.append("/").append(serivceName);
        buf.append("?application=").append(appName);
        buf.append("&host=").append(consumerHost);
        String subscribleUrl = buf.toString();
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

        for (Resource configFile : configFiles) {
            try {
                VenusClientConfig venusClientConfig = (VenusClientConfig) xStream.fromXML(configFile.getURL());
                for (ReferenceService referenceService : venusClientConfig.getReferenceServices()) {
                    //初始化服务接口
                    String serviceInterfaceName = referenceService.getType();
                    if (serviceInterfaceName == null) {
                        throw new VenusConfigException("Service type can not be null:" + configFile);
                    }
                    Class<?> serviceInterface = null;
                    try {
                        serviceInterface = Class.forName(serviceInterfaceName);
                        referenceService.setServiceInterface(serviceInterface);
                    } catch (ClassNotFoundException e) {
                        throw new VenusConfigException("service interface class not found:" + serviceInterfaceName);
                    }

                    //初始化service
                    Service service = AnnotationUtil.getAnnotation(serviceInterface.getAnnotations(), Service.class);
                    if(service == null){
                        throw new VenusConfigException("service interface annotation config is null:" + serviceInterface.getName());
                    }
                    String serviceName = service.name();
                    if(StringUtils.isEmpty(serviceName)){
                        serviceName = serviceInterface.getSimpleName();
                    }
                    referenceService.setServiceName(serviceName);
                    referenceService.setVersion(service.version());

                    //初始化方法
                    VenusExceptionFactory venusExceptionFactory = XmlVenusExceptionFactory.getInstance();
                    for (Method method : serviceInterface.getMethods()) {
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

                    //设置服务地址
                    if(StringUtils.isNotEmpty(referenceService.getIpAddressList())){
                        //转化及校验地址
                        String ipAddressList = convertAndValidAddress(referenceService.getIpAddressList());
                        referenceService.setIpAddressList(ipAddressList);
                    }
                }
                if(venusClientConfig.getReferenceServices() != null){
                    clientConfig.getReferenceServices().addAll(venusClientConfig.getReferenceServices());
                }
                if(venusClientConfig.getRemoteConfigMap() != null){
                    clientConfig.getRemoteConfigMap().putAll(venusClientConfig.getRemoteConfigMap());
                }
            } catch (Exception e) {
                throw new ConfigurationException("parse venus client config failed:" + configFile.getFilename(),e);
            }
        }
        return clientConfig;
    }

    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        Map<String,String> refBeanMap = new HashMap<String, String>();
        // register to resolvable dependency container
        for (Map.Entry<Class<?>, ServiceDefinedBean> entry : serviceMap.entrySet()) {
            ServiceDefinedBean srvDefBean = entry.getValue();
            if (beanFactory instanceof BeanDefinitionRegistry) {
                BeanDefinitionRegistry reg = (BeanDefinitionRegistry) beanFactory;

                //设置spring bean name，若指定了name名称则以自定义为准，否则取venus接口定义中的serviceName
                String beanName = srvDefBean.getName();
                if(StringUtils.isEmpty(beanName)){
                    beanName = srvDefBean.getServiceName().concat("#0");
                }
                if(StringUtils.isEmpty(beanName)){
                    throw new VenusConfigException("spring bean name and annotation service name is empty:" + srvDefBean.getServiceInterface());
                }
                if(reg.containsBeanDefinition(beanName)){
                    beanName = beanName.concat("#0");
                }

                GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
                beanDefinition.setBeanClass(ServiceFactoryBean.class);
                beanDefinition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, beanName));
                beanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
                ConstructorArgumentValues args = new ConstructorArgumentValues();
                args.addIndexedArgumentValue(0, srvDefBean.getService());
                args.addIndexedArgumentValue(1, srvDefBean.getServiceInterface());
                beanDefinition.setConstructorArgumentValues(args);

                beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_NAME);
                reg.registerBeanDefinition(beanName, beanDefinition);

                refBeanMap.put(beanName,srvDefBean.getServiceInterface().getName());
            }
        }

        if(isPrintRefBean()){
            if(logger.isInfoEnabled()){
                logger.info("##########ref beans##############:\n{}.",JSON.toJSONString(refBeanMap,true));
            }
        }
    }

    /**
     * 将simpleClassName转为首字母小写
     * @return
     */
    /*
    String formatClassName(String simpleClassName){
        if(simpleClassName.contains(".")){
            simpleClassName=simpleClassName.substring(simpleClassName.lastIndexOf(".")+1);
        }
        simpleClassName = simpleClassName.substring(0, 1).toLowerCase().concat(simpleClassName.substring(1));
        return simpleClassName;
    }
    */

    @Override
    public void destroy() {
        if (shutdown) {
            return;
        }

        //反订阅
        if(venusRegistryFactory != null && venusRegistryFactory.getRegister() != null){
            Register register = venusRegistryFactory.getRegister();
            if(register != null){
                register.destroy();
            }
        }

        shutdown = true;
    }

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
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

    public VenusApplication getVenusApplication() {
        return venusApplication;
    }

    public void setVenusApplication(VenusApplication venusApplication) {
        this.venusApplication = venusApplication;
    }

    public VenusMonitorFactory getVenusMonitorFactory() {
        return venusMonitorFactory;
    }

    public void setVenusMonitorFactory(VenusMonitorFactory venusMonitorFactory) {
        this.venusMonitorFactory = venusMonitorFactory;
    }

    public boolean isPrintRefBean(){
        return true;
    }

}
