package com.meidusa.venus.support;

import com.meidusa.toolkit.common.bean.BeanContext;
import com.meidusa.venus.ConnectionFactory;
import com.meidusa.venus.ConnectionProcesser;
import org.springframework.context.ApplicationContext;

/**
 * venus应用上下文信息
 * Created by Zhangzhihua on 2017/8/28.
 */
public class VenusContext {

    private static VenusContext venusContext;

    //spring上下文
    private ApplicationContext applicationContext;

    //spring上下文
    private BeanContext beanContext;

    //应用名称
    private String application;

    private ConnectionFactory connectionFactory;

    private ConnectionProcesser connectionProcesser;

    private String encodeType = "venus";

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

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public ConnectionProcesser getConnectionProcesser() {
        return connectionProcesser;
    }

    public void setConnectionProcesser(ConnectionProcesser connectionProcesser) {
        this.connectionProcesser = connectionProcesser;
    }

    public String getEncodeType() {
        return encodeType;
    }

    public void setEncodeType(String encodeType) {
        this.encodeType = encodeType;
    }
}
