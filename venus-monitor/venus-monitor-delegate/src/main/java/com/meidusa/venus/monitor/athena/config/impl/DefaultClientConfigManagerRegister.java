package com.meidusa.venus.monitor.athena.config.impl;

import com.meidusa.venus.ServiceFactoryBean;
import com.meidusa.venus.exception.VenusConfigException;
import com.meidusa.venus.monitor.config.ClientConfigManagerRegister;
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
public class DefaultClientConfigManagerRegister implements ClientConfigManagerRegister {

    @Override
    public Object initConfigManager(String appName, boolean enabled) {
        //初始化实例
        ClientConfigManager clientConfigManager = initAthenaConfigManager(appName, enabled);
        if(clientConfigManager == null){
            throw new VenusConfigException("init clientConfigManager failed.");
        }
        return clientConfigManager;
    }

    /**
     * 初始化athena配置信息
     */
    ClientConfigManager initAthenaConfigManager(String appName,boolean enable){
        DefaultClientConfigManager clientConfigManager = new DefaultClientConfigManager();
        clientConfigManager.setAppName(appName);
        clientConfigManager.setMonitorEnabled(enable);
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
        String beanName = clientConfigManager.getClass().getSimpleName();
        BeanDefinitionRegistry reg = (BeanDefinitionRegistry) beanFactory;
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(ServiceFactoryBean.class);
        beanDefinition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, beanName));
        beanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
        ConstructorArgumentValues args = new ConstructorArgumentValues();
        args.addIndexedArgumentValue(0, clientConfigManager);
        args.addIndexedArgumentValue(1, ClientConfigManager.class);
        beanDefinition.setConstructorArgumentValues(args);
        beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_NAME);
        reg.registerBeanDefinition(beanName, beanDefinition);
    }
}
