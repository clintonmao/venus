package com.meidusa.venus.client.invoker;

import com.meidusa.venus.client.factory.xml.config.RemoteConfig;

import java.lang.reflect.Method;

/**
 * invocation
 * Created by Zhangzhihua on 2017/7/31.
 */
public class Invocation {

    private Class<?> serviceType;

    private Method method;

    private Object[] args;

    private RemoteConfig remoteConfig;

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public Class<?> getServiceType() {
        return serviceType;
    }

    public void setServiceType(Class<?> serviceType) {
        this.serviceType = serviceType;
    }

    public RemoteConfig getRemoteConfig() {
        return remoteConfig;
    }

    public void setRemoteConfig(RemoteConfig remoteConfig) {
        this.remoteConfig = remoteConfig;
    }
}
