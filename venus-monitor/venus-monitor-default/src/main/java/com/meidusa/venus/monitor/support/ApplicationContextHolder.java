package com.meidusa.venus.monitor.support;

import com.meidusa.venus.exception.VenusConfigException;
import org.apache.commons.lang.StringUtils;
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
        String simpleClassName=beanDefinition.getBeanClassName();
        if(StringUtils.isEmpty(simpleClassName) || simpleClassName.length() < 2){
            throw new VenusConfigException("className:" + simpleClassName + " invalid.");
        }
        if(simpleClassName.contains(".")){
            simpleClassName=simpleClassName.substring(simpleClassName.lastIndexOf(".")+1);
        }
        simpleClassName = simpleClassName.substring(0, 1).toLowerCase().concat(simpleClassName.substring(1));
        registerBean(simpleClassName,beanDefinition);
    }

    public static BeanDefinitionBuilder getBeanDefinitionBuilder(Class clazz){
        return BeanDefinitionBuilder.genericBeanDefinition(clazz);
    }

}