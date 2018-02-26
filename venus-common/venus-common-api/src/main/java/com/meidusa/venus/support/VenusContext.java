package com.meidusa.venus.support;

import com.meidusa.toolkit.common.bean.BeanContext;
import com.meidusa.toolkit.net.BackendConnectionPool;
import org.springframework.context.ApplicationContext;

import java.util.Map;

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

    //连接池映射表
    private Map<String, BackendConnectionPool> connectionPoolMap;

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

    public Map<String, BackendConnectionPool> getConnectionPoolMap() {
        return connectionPoolMap;
    }

    public void setConnectionPoolMap(Map<String, BackendConnectionPool> connectionPoolMap) {
        this.connectionPoolMap = connectionPoolMap;
    }
}
