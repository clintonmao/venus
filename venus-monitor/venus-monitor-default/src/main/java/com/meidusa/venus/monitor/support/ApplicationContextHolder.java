package com.meidusa.venus.monitor.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * spring上下文持有类
 * Created by Zhangzhihua on 2017/10/12.
 */
public class ApplicationContextHolder implements ApplicationContextAware {

    private static ApplicationContext context;

    private static BeanDefinitionRegistry beanDefinitonRegistry;

    public static ApplicationContext getApplicationContext() {
        return context;
    }

    public static Object getBean(String name){
        return context.getBean(name);
    }

    public static <T> T getBean(Class<T> clz){
        return context.getBean(clz);
    }

    public static <T> T getBean(String name , Class<T> clz){
        return context.getBean(name, clz);
    }

    @Override
    public void setApplicationContext(ApplicationContext ac)
            throws BeansException {
        context = ac;
        ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) context;
        beanDefinitonRegistry = (BeanDefinitionRegistry) configurableApplicationContext
                .getBeanFactory();
    }

    /**
     * 注册bean
     * @param beanName
     * @param beanDefinition
     */
    public synchronized static void registerBean(String beanName, BeanDefinition beanDefinition){
        if(!beanDefinitonRegistry.containsBeanDefinition(beanName)){
            beanDefinitonRegistry.registerBeanDefinition(beanName, beanDefinition);
        }
    }

    /**
     * 注册bean
     * @param beanDefinition
     */
    public static void registerBean(BeanDefinition beanDefinition){
        String simpleNameString=beanDefinition.getBeanClassName();
        if(simpleNameString.contains(".")){
            simpleNameString=simpleNameString.substring(simpleNameString.lastIndexOf(".")+1);
        }
        registerBean(simpleNameString,beanDefinition);
    }

    public static BeanDefinitionBuilder getBeanDefinitionBuilder(Class clazz){
        return BeanDefinitionBuilder.genericBeanDefinition(clazz);
    }

}