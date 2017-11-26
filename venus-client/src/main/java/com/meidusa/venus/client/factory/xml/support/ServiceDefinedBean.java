package com.meidusa.venus.client.factory.xml.support;

import com.meidusa.venus.client.factory.InvokerInvocationHandler;

/**
 * 服务定义实例
 * Created by Zhangzhihua on 2017/7/28.
 */
public class ServiceDefinedBean {

    //注意：尽量避免以beanNaem为进行spring注册，可能存在不同包但同名的情况，该情况下会导致少注册bean
    private String beanName;

    private Class<?> clazz;

    private Object service;

    private InvokerInvocationHandler handler;

    public ServiceDefinedBean(String beanName,Class<?> clazz, Object service,InvokerInvocationHandler handler){
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

    public InvokerInvocationHandler getHandler() {
        return handler;
    }

    public void setHandler(InvokerInvocationHandler handler) {
        this.handler = handler;
    }
}
