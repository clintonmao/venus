package com.meidusa.venus.client.factory.xml.support;

import com.meidusa.venus.client.factory.InvokerInvocationHandler;

/**
 * 服务定义实例
 * Created by Zhangzhihua on 2017/7/28.
 */
public class ServiceDefinedBean {

    //spring实例名称
    private String name;

    //服务名称
    private String serviceName;

    //服务接口
    private Class<?> clazz;

    private Object service;

    private InvokerInvocationHandler handler;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public InvokerInvocationHandler getHandler() {
        return handler;
    }

    public void setHandler(InvokerInvocationHandler handler) {
        this.handler = handler;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}
