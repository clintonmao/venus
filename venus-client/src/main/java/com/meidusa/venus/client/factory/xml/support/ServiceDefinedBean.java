package com.meidusa.venus.client.factory.xml.support;

import com.meidusa.venus.client.proxy.VenusClientInvokerProxy;

/**
 * 服务定义实例
 * Created by Zhangzhihua on 2017/7/28.
 */
public class ServiceDefinedBean {

    private String beanName;

    private Class<?> clazz;

    private Object service;

    private VenusClientInvokerProxy handler;

    public ServiceDefinedBean(String beanName,Class<?> clazz, Object service,VenusClientInvokerProxy handler){
        this.beanName = beanName;
        this.clazz = clazz;
        this.service = service;
        this.handler = handler;
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    public Object getService() {
        return service;
    }

    public void setService(Object service) {
        this.service = service;
    }

    public VenusClientInvokerProxy getHandler() {
        return handler;
    }

    public void setHandler(VenusClientInvokerProxy handler) {
        this.handler = handler;
    }
}
