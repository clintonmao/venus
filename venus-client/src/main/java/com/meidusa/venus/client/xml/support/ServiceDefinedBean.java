package com.meidusa.venus.client.xml.support;

import com.meidusa.venus.client.xml.XmlInvocationHandler;

/**
 * 服务定义实例
 * Created by Zhangzhihua on 2017/7/28.
 */
public class ServiceDefinedBean {

        private String beanName;

        private Class<?> clazz;

        private Object service;

        private XmlInvocationHandler handler;

        public ServiceDefinedBean(String beanName,Class<?> clazz, Object service,XmlInvocationHandler handler){
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

    public XmlInvocationHandler getHandler() {
        return handler;
    }

    public void setHandler(XmlInvocationHandler handler) {
        this.handler = handler;
    }
}
