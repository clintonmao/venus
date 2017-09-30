package com.meidusa.venus;

import com.meidusa.toolkit.common.bean.BeanContext;
import org.springframework.context.ApplicationContext;

import java.rmi.registry.Registry;

/**
 * venus应用上下文信息
 * Created by Zhangzhihua on 2017/8/28.
 */
public class VenusContext {

    private static VenusContext venusContext;

    /**
     * spring上下文
     */
    private ApplicationContext applicationContext;

    private BeanContext beanContext;

    /**
     * 应用名称
     */
    private String application;

    private String port;

    public static VenusContext getInstance(){
        if(venusContext == null){
            venusContext = new VenusContext();
        }
        return venusContext;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public BeanContext getBeanContext() {
        return beanContext;
    }

    public void setBeanContext(BeanContext beanContext) {
        this.beanContext = beanContext;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }
}
