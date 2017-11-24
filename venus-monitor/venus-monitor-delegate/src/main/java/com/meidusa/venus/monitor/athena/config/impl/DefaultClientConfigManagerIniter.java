package com.meidusa.venus.monitor.athena.config.impl;

import com.athena.service.api.AthenaDataService;
import com.meidusa.venus.ServiceFactoryBean;
import com.meidusa.venus.exception.VenusConfigException;
import com.meidusa.venus.monitor.config.ClientConfigManagerIniter;
import com.saic.framework.athena.configuration.ClientConfigManager;
import com.saic.framework.athena.configuration.DefaultClientConfigManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;

/**
 * athena配置信息管理代理，目的将athena-client依赖独立出来
 * Created by Zhangzhihua on 2017/10/9.
 */
public class DefaultClientConfigManagerIniter implements ClientConfigManagerIniter {

    @Override
    public Object initConfigManager(String appName, boolean enabled) {
        //初始化实例
        ClientConfigManager clientConfigManager = initDefaultConfigManager(appName, enabled);
        if(clientConfigManager == null){
            throw new VenusConfigException("init clientConfigManager failed.");
        }
        return clientConfigManager;
    }

    /**
     * 初始化athena配置信息
     */
    ClientConfigManager initDefaultConfigManager(String appName, boolean enable){
        DefaultClientConfigManager clientConfigManager = new DefaultClientConfigManager();
        clientConfigManager.setAppName(appName);
        clientConfigManager.setMonitorEnabled(enable);
        clientConfigManager.init();
        return clientConfigManager;
    }

    @Override
    public void registeConfigManager(ConfigurableListableBeanFactory beanFactory, Object clientConfigManager) {
        ClientConfigManager clientConfigMgr = (ClientConfigManager)clientConfigManager;
        registeAthenaConfigManager(beanFactory,clientConfigMgr);
    }

    /**
     * 注册configManager
     * @param beanFactory
     */
    void registeAthenaConfigManager(ConfigurableListableBeanFactory beanFactory, ClientConfigManager clientConfigManager){
        String simpleClassName = clientConfigManager.getClass().getSimpleName();
        if(simpleClassName.contains(".")){
            simpleClassName=simpleClassName.substring(simpleClassName.lastIndexOf(".")+1);
        }
        simpleClassName = simpleClassName.substring(0, 1).toLowerCase().concat(simpleClassName.substring(1));
        BeanDefinitionRegistry reg = (BeanDefinitionRegistry) beanFactory;
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(ServiceFactoryBean.class);
        beanDefinition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, simpleClassName));
        beanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
        ConstructorArgumentValues args = new ConstructorArgumentValues();
        args.addIndexedArgumentValue(0, clientConfigManager);
        args.addIndexedArgumentValue(1, ClientConfigManager.class);
        beanDefinition.setConstructorArgumentValues(args);
        beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_NAME);
        reg.registerBeanDefinition(simpleClassName, beanDefinition);
    }
}
