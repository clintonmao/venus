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

package com.meidusa.venus.client.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import com.meidusa.venus.client.*;
import com.meidusa.venus.client.xml.bean.*;
import com.meidusa.venus.client.xml.support.*;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.RuleSet;
import org.apache.commons.digester.xmlrules.FromXmlRuleSet;
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
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.meidusa.toolkit.common.bean.BeanContext;
import com.meidusa.toolkit.common.bean.BeanContextBean;
import com.meidusa.toolkit.common.bean.config.ConfigurationException;
import com.meidusa.toolkit.common.bean.util.InitialisationException;
import com.meidusa.toolkit.common.poolable.MultipleLoadBalanceObjectPool;
import com.meidusa.toolkit.common.poolable.ObjectPool;
import com.meidusa.toolkit.common.poolable.PoolableObjectPool;
import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.toolkit.net.BackendConnectionPool;
import com.meidusa.toolkit.net.ConnectionConnector;
import com.meidusa.toolkit.net.ConnectionManager;
import com.meidusa.toolkit.net.MultipleLoadBalanceBackendConnectionPool;
import com.meidusa.toolkit.net.PollingBackendConnectionPool;
import com.meidusa.toolkit.util.StringUtil;
import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.digester.DigesterRuleParser;
import com.meidusa.venus.exception.CodedException;
import com.meidusa.venus.exception.ServiceNotFoundException;
import com.meidusa.venus.exception.VenusConfigException;
import com.meidusa.venus.exception.VenusExceptionFactory;
import com.meidusa.venus.exception.XmlVenusExceptionFactory;
import com.meidusa.venus.extension.athena.AthenaExtensionResolver;
import com.meidusa.venus.io.network.VenusBIOConnectionFactory;
import com.meidusa.venus.io.network.VenusBackendConnectionFactory;
import com.meidusa.venus.io.packet.PacketConstant;
import com.meidusa.venus.util.FileWatchdog;
import com.meidusa.venus.util.VenusBeanUtilsBean;

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
     * 连接池映射表
     */
    private Map<String, Tuple<ObjectPool, BackendConnectionPool>> poolMap = new HashMap<String, Tuple<ObjectPool, BackendConnectionPool>>(); // NOPMD

    /**
     * 连接池映射表
     */
    private Map<String, Object> realPoolMap = new HashMap<String, Object>();

    /**
     * 服务实例映射表
     */
    private Map<Class<?>, ServiceDefinedBean> servicesMap = new HashMap<Class<?>, ServiceDefinedBean>();

    /**
     * 服务实例映射表
     */
    private Map<String, ServiceDefinedBean> serviceBeanMap = new HashMap<String, ServiceDefinedBean>();

    private ConnectionManager connManager;

    private ConnectionConnector connector;

    private boolean enableAsync = true;

    private boolean shutdown = false;

    private InvocationListenerContainer container = new InvocationListenerContainer();

    private VenusNIOMessageHandler handler = new VenusNIOMessageHandler();

    private VenusExceptionFactory venusExceptionFactory;

    private int asyncExecutorSize = 10;

    private boolean needPing = false;

    private Timer reloadTimer = new Timer();

    private boolean enableReload = false;

    private boolean inited = false;

    private ResourceLoader resourceLoader = new DefaultResourceLoader();

    private BeanContext beanContext;

	private ApplicationContext applicationContext;

    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> t) {
        if (shutdown) {
            throw new IllegalStateException("service factory has been shutdown");
        }
        ServiceDefinedBean object = servicesMap.get(t);
        
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
        ServiceDefinedBean object = serviceBeanMap.get(name);
        
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
        logger.trace("current Venus Client id=" + PacketConstant.VENUS_CLIENT_ID);
        if (venusExceptionFactory == null) {
            XmlVenusExceptionFactory xmlVenusExceptionFactory = new XmlVenusExceptionFactory();

            //3.0.8版本将采用自动扫描的方式获得 exception 相关的配置
            //xmlVenusExceptionFactory.setConfigFiles(new String[] { "classpath:com/meidusa/venus/exception/VenusSystemException.xml" });
            xmlVenusExceptionFactory.init();
            this.venusExceptionFactory = xmlVenusExceptionFactory;
        }

        handler.setVenusExceptionFactory(venusExceptionFactory);
        if (enableAsync) {
            if (connector == null) {
                this.connector = new ConnectionConnector("connection Connector");
                connector.setDaemon(true);

            }

            if (connManager == null) {
                try {
                    connManager = new ConnectionManager("Connection Manager", this.getAsyncExecutorSize());
                } catch (IOException e) {
                    throw new InitialisationException(e);
                }
                connManager.setDaemon(true);
                connManager.start();
            }

            connector.setProcessors(new ConnectionManager[]{connManager});
            connector.start();
        }

        beanContext = new ClientBeanContext(applicationContext!= null ?applicationContext.getAutowireCapableBeanFactory(): null);
        BeanContextBean.getInstance().setBeanContext(beanContext);
        VenusBeanUtilsBean.setInstance(new ClientBeanUtilsBean(new ConvertUtilsBean(), new PropertyUtilsBean(), beanContext));
        AthenaExtensionResolver.getInstance().resolver();
        handler.setContainer(this.container);
        reloadConfiguration();

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

    class VenusFileWatchdog extends FileWatchdog {

        protected VenusFileWatchdog(File... file) {
            super(file);
        }

        @Override
        protected void doOnChange() {
            try {
                XmlServiceFactory.this.reloadConfiguration();
            } catch (Exception e) {
                XmlServiceFactory.logger.error("reload configuration error", e);
            }
        }

    }

    private synchronized void reloadConfiguration() throws Exception {
        Map<Class<?>, ServiceConfig> serviceConfigMap = new HashMap<Class<?>, ServiceConfig>();
        Map<String, Tuple<ObjectPool, BackendConnectionPool>> poolMap = new HashMap<String, Tuple<ObjectPool, BackendConnectionPool>>();
        final Map<String, Object> realPoolMap = new HashMap<String, Object>();
        Map<Class<?>, ServiceDefinedBean> servicesMap = new HashMap<Class<?>, ServiceDefinedBean>();

        try {
            loadConfiguration(serviceConfigMap, poolMap, realPoolMap, servicesMap);
        } catch (Exception e) {
            reloadTimer.schedule(new ClosePoolTask(realPoolMap), 1000 * 30);
            throw e;
        }
        this.poolMap = poolMap;

        for (Map.Entry<Class<?>, ServiceDefinedBean> entry : servicesMap.entrySet()) {
            Class<?> key = entry.getKey();
            ServiceDefinedBean source = entry.getValue();
            ServiceDefinedBean target = this.servicesMap.get(key);
            if (target != null) {
                target.getHandler().setBioConnPool(source.getHandler().getBioConnPool());
                target.getHandler().setNioConnPool(source.getHandler().getNioConnPool());
                target.getHandler().setSerializeType((byte) source.getHandler().getSerializeType());
            } else {
                this.servicesMap.put(key, source);
            }
        }

        this.serviceConfigMap = serviceConfigMap;
        final Map<String, Object> oldPools = this.realPoolMap;
        this.realPoolMap = realPoolMap;
        reloadTimer.schedule(new ClosePoolTask(oldPools), 1000 * 30);

    }



    /**
     * 加载配置并初始化连接、service实例
     * @param serviceConfigMap
     * @param poolMap
     * @param realPoolMap
     * @param servicesMap
     * @throws Exception
     */
    private void loadConfiguration(Map<Class<?>, ServiceConfig> serviceConfigMap, Map<String, Tuple<ObjectPool, BackendConnectionPool>> poolMap,
                                   Map<String, Object> realPoolMap, Map<Class<?>, ServiceDefinedBean> servicesMap)
            throws Exception {
	    //加载客户端配置信息
        VenusClientConfig clientConfig = loadClientConfig();

        // 初始化remote，并且创建Pool
        for (Map.Entry<String, RemoteConfig> remoteEntry : clientConfig.getRemoteConfigMap().entrySet()) {
            RemoteContainer container = createRemoteContainer(remoteEntry.getValue(), realPoolMap);
            Tuple<ObjectPool, BackendConnectionPool> tuple = new Tuple<ObjectPool, BackendConnectionPool>();
            tuple.left = container.getBioPool();
            tuple.right = container.getNioPool();
            poolMap.put(remoteEntry.getKey(), tuple);
        }

        //初始化service实例
        for (ServiceConfig serviceConfig : clientConfig.getServiceConfigs()) {
            RemoteConfig remoteConfig = clientConfig.getRemoteConfigMap().get(serviceConfig.getRemote());
            Tuple<ObjectPool, BackendConnectionPool> tuple = null;
            if (!StringUtil.isEmpty(serviceConfig.getRemote())) {
                tuple = poolMap.get(serviceConfig.getRemote());
                if (tuple == null) {
                    throw new ConfigurationException("remoteConfig=" + serviceConfig.getRemote() + " not found!!");
                }
            } else {
                String ipAddress = serviceConfig.getIpAddressList();
                tuple = poolMap.get(ipAddress);
                if (ipAddress != null && tuple == null) {
                    RemoteContainer container = createRemoteContainer(ipAddress, realPoolMap, true);
                    tuple = new Tuple<ObjectPool, BackendConnectionPool>();
                    tuple.left = container.getBioPool();
                    tuple.right = container.getNioPool();
                    poolMap.put(ipAddress, tuple);
                }
            }

            if (tuple != null) {
                XmlInvocationHandler invocationHandler = new XmlInvocationHandler();
                invocationHandler.setBioConnPool(tuple.left);
                invocationHandler.setNioConnPool(tuple.right);
                invocationHandler.setServiceFactory(this);
                invocationHandler.setVenusExceptionFactory(this.getVenusExceptionFactory());
                if (remoteConfig != null && remoteConfig.getAuthenticator() != null) {
                    invocationHandler.setSerializeType(remoteConfig.getAuthenticator().getSerializeType());
                }

                invocationHandler.setContainer(this.container);

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
                    serviceBeanMap.put(serviceConfig.getBeanName(), defined);
                }else{
                	servicesMap.put(serviceConfig.getType(), defined);
                }
            } else {
                if (serviceConfig.getInstance() != null) {
                    ServiceDefinedBean defined = new ServiceDefinedBean(serviceConfig.getBeanName(),serviceConfig.getType(),serviceConfig.getInstance(), null);
                    
                    if (serviceConfig.getBeanName() != null) {
                        serviceBeanMap.put(serviceConfig.getBeanName(), defined);
                    }else{
                    	servicesMap.put(serviceConfig.getType(), defined);
                    }
                } else {
                    throw new ConfigurationException("Service instance or ipAddressList or remoteConfig can not be null:" + serviceConfig.getType());
                }
            }

        }
    }

    /**
     * 加载client配置信息
     * @return
     */
    VenusClientConfig loadClientConfig(){
        VenusClientConfig clientConfig = new VenusClientConfig();
        for (Resource configFile : configFiles) {
            // configFile = (String) ConfigUtil.filter(configFile);
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

    private RemoteContainer createRemoteContainer(String ipAddress, Map<String, Object> realPools, boolean share) throws Exception {
        RemoteContainer container = new RemoteContainer();

        if (!StringUtil.isEmpty(ipAddress)) {
            String ipList[] = StringUtil.split(ipAddress, ", ");
            PoolableObjectPool bioPools[] = new PoolableObjectPool[ipList.length];
            BackendConnectionPool nioPools[] = new BackendConnectionPool[ipList.length];
            for (int i = 0; i < ipList.length; i++) {
                String shareName = share ? "SHARED-" : "";
                if (share) {
                    nioPools[i] = (PollingBackendConnectionPool) realPools.get("N-" + shareName + ipList[i]);
                    bioPools[i] = (PoolableObjectPool) realPools.get("B-" + shareName + ipList[i]);
                    if (nioPools[i] != null) {
                        continue;
                    }
                }

                VenusBackendConnectionFactory nioFactory = new VenusBackendConnectionFactory();
                nioPools[i] = new PollingBackendConnectionPool("N-" + shareName + ipList[i], nioFactory, 8);
                bioPools[i] = new PoolableObjectPool();

                // bio
                VenusBIOConnectionFactory bioFactory = new VenusBIOConnectionFactory();

                bioFactory.setNeedPing(needPing);

                String temp[] = StringUtil.split(ipList[i], ":");
                if (temp.length > 1) {
                    nioFactory.setHost(temp[0]);
                    nioFactory.setPort(Integer.valueOf(temp[1]));

                    bioFactory.setHost(temp[0]);
                    bioFactory.setPort(Integer.valueOf(temp[1]));
                } else {
                    nioFactory.setHost(temp[0]);
                    nioFactory.setPort(16800);

                    bioFactory.setHost(temp[0]);
                    bioFactory.setPort(16800);
                }

                if (this.isEnableAsync()) {
                    nioFactory.setConnector(connector);
                    nioFactory.setMessageHandler(handler);
                    nioPools[i].init();
                    realPools.put(nioPools[i].getName(), nioPools[i]);
                }
                bioPools[i].setName("B-" + shareName + nioFactory.getHost() + ":" + nioFactory.getPort());
                bioPools[i].setFactory(bioFactory);
                bioPools[i].setTestOnBorrow(true);
                bioPools[i].setTestWhileIdle(true);
                bioPools[i].init();

                realPools.put(bioPools[i].getName(), bioPools[i]);
            }

            if (ipList.length > 1) {
                MultipleLoadBalanceObjectPool bioPool = new MultipleLoadBalanceObjectPool(MultipleLoadBalanceObjectPool.LOADBALANCING_ROUNDROBIN, bioPools);
                MultipleLoadBalanceBackendConnectionPool nioPool = new MultipleLoadBalanceBackendConnectionPool("N-V-" + ipAddress,
                        MultipleLoadBalanceObjectPool.LOADBALANCING_ROUNDROBIN, nioPools);
                bioPool.setName("B-V-" + ipAddress);
                nioPool.init();
                bioPool.init();

                realPools.put(bioPool.getName(), bioPool);
                realPools.put(nioPool.getName(), nioPool);
                container.setBioPool(bioPool);
                container.setNioPool(nioPool);
            } else {
                container.setBioPool(bioPools[0]);
                container.setNioPool(nioPools[0]);
            }
        } else {
            throw new IllegalArgumentException(" ipaddress cannot be null");
        }

        return container;
    }

    private RemoteContainer createRemoteContainer(RemoteConfig remoteConfig, Map<String, Object> realPools) throws Exception {
        RemoteContainer container = new RemoteContainer();
        FactoryConfig factoryConfig = remoteConfig.getFactory();
        if (factoryConfig == null) {
            throw new ConfigurationException(remoteConfig.getName() + " factory cannot be null");
        }
        PoolConfig poolConfig = remoteConfig.getPool();
        String ipAddress = factoryConfig.getIpAddressList();
        if (!StringUtil.isEmpty(ipAddress)) {
            String ipList[] = StringUtil.split(ipAddress, ", ");
            PoolableObjectPool bioPools[] = new PoolableObjectPool[ipList.length];
            BackendConnectionPool nioPools[] = new BackendConnectionPool[ipList.length];

            for (int i = 0; i < ipList.length; i++) {
                String shareName = remoteConfig.isShare() ? "SHARED-" : "";
                if (remoteConfig.isShare()) {
                    nioPools[i] = (PollingBackendConnectionPool) realPools.get("N-" + shareName + ipList[i]);
                    bioPools[i] = (PoolableObjectPool) realPools.get("B-" + shareName + ipList[i]);
                    if (nioPools[i] != null) {
                        continue;
                    }
                }

                VenusBackendConnectionFactory nioFactory = new VenusBackendConnectionFactory();
                VenusBIOConnectionFactory bioFactory = new VenusBIOConnectionFactory();
                if (remoteConfig.getAuthenticator() != null) {
                    bioFactory.setAuthenticator(remoteConfig.getAuthenticator());
                }

                nioPools[i] = new PollingBackendConnectionPool("N-" + shareName + ipList[i], nioFactory, 8);
                bioPools[i] = new PoolableObjectPool();
                if (poolConfig != null) {
                    BeanUtils.copyProperties(nioPools[i], poolConfig);
                    BeanUtils.copyProperties(bioPools[i], poolConfig);
                } else {
                    bioPools[i].setTestOnBorrow(true);
                    bioPools[i].setTestWhileIdle(true);
                }

                if (remoteConfig.getAuthenticator() != null) {
                    nioFactory.setAuthenticator(remoteConfig.getAuthenticator());
                    bioFactory.setAuthenticator(remoteConfig.getAuthenticator());
                }

                bioFactory.setNeedPing(needPing);
                if (factoryConfig != null) {

                    BeanUtils.copyProperties(nioFactory, factoryConfig);
                    BeanUtils.copyProperties(bioFactory, factoryConfig);
                }
                String temp[] = StringUtil.split(ipList[i], ":");
                if (temp.length > 1) {
                    nioFactory.setHost(temp[0]);
                    nioFactory.setPort(Integer.valueOf(temp[1]));

                    bioFactory.setHost(temp[0]);
                    bioFactory.setPort(Integer.valueOf(temp[1]));
                } else {
                    nioFactory.setHost(temp[0]);
                    nioFactory.setPort(16800);

                    bioFactory.setHost(temp[0]);
                    bioFactory.setPort(16800);
                }

                if (this.isEnableAsync()) {
                    nioFactory.setConnector(this.connector);
                    nioFactory.setMessageHandler(handler);
                    // nioPools[i].setName("n-connPool-"+nioFactory.getIpAddress());
                    nioPools[i].init();
                    realPools.put(nioPools[i].getName(), nioPools[i]);
                }
                bioPools[i].setName("B-" + shareName + bioFactory.getHost() + ":" + bioFactory.getPort());
                bioPools[i].setFactory(bioFactory);
                bioPools[i].init();
                realPools.put(bioPools[i].getName(), bioPools[i]);
            }

            if (ipList.length > 1) {
                MultipleLoadBalanceObjectPool bioPool = new MultipleLoadBalanceObjectPool(remoteConfig.getLoadbalance(), bioPools);
                MultipleLoadBalanceBackendConnectionPool nioPool = new MultipleLoadBalanceBackendConnectionPool(remoteConfig.getName(), remoteConfig.getLoadbalance(),
                        nioPools);

                bioPool.setName("B-V-" + remoteConfig.getName());

                container.setBioPool(bioPool);
                container.setNioPool(nioPool);
                bioPool.init();
                nioPool.init();

                realPools.put(bioPool.getName(), bioPool);
                realPools.put(nioPool.getName(), nioPool);
            } else {
                container.setBioPool(bioPools[0]);
                container.setNioPool(nioPools[0]);
            }
            container.setRemoteConfig(remoteConfig);
        } else {
            throw new IllegalArgumentException("remtoe=" + remoteConfig.getName() + ", ipaddress cannot be null");
        }

        return container;
    }

    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		// register to resolvable dependency container
		//BeanFactory beanFactory = applicationContext.getAutowireCapableBeanFactory();
        if (beanFactory instanceof ConfigurableListableBeanFactory) {
            ConfigurableListableBeanFactory cbf = beanFactory;
            for (Map.Entry<Class<?>, ServiceDefinedBean> entry : servicesMap.entrySet()) {
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

			for (Map.Entry<String, ServiceDefinedBean> entry : serviceBeanMap.entrySet()) {
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

    public void destroy() {
        if (shutdown) {
            return;
        }
        shutdown = true;
        if (connector != null) {
            if (connector.isAlive()) {
                connector.shutdown();
            }
        }
        if (connManager != null) {
            if (connManager.isAlive()) {
                connManager.shutdown();
            }
        }
    }

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

    public ServiceConfig getServiceConfig(Class<?> type) {
        return serviceConfigMap.get(type);
    }

    public boolean isEnableReload() {
        return enableReload;
    }

    public void setEnableReload(boolean enableReload) {
        this.enableReload = enableReload;
    }

    public boolean isNeedPing() {
        return needPing;
    }

    public void setNeedPing(boolean needPing) {
        this.needPing = needPing;
    }

    public boolean isEnableAsync() {
        return enableAsync;
    }

    public void setEnableAsync(boolean enableAsync) {
        this.enableAsync = enableAsync;
    }

    public int getAsyncExecutorSize() {
        return asyncExecutorSize;
    }

    public ConnectionConnector getConnector() {
        return connector;
    }

    public void setConnector(ConnectionConnector connector) {
        this.connector = connector;
    }

    public void setAsyncExecutorSize(int asyncExecutorSize) {
        this.asyncExecutorSize = asyncExecutorSize;
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
