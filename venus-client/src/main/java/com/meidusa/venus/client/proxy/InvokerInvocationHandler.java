package com.meidusa.venus.client.proxy;

import com.meidusa.venus.client.authenticate.DummyAuthenticator;
import com.meidusa.venus.client.factory.xml.config.RemoteConfig;
import com.meidusa.venus.client.invoker.Invocation;
import com.meidusa.venus.client.invoker.Invoker;
import com.meidusa.venus.client.invoker.Result;
import com.meidusa.venus.client.invoker.venus.VenusInvoker;
import com.meidusa.venus.exception.VenusExceptionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 服务调用代理，调用相应的invoker
 * @author Struct
 */

public class InvokerInvocationHandler implements InvocationHandler {

    private static Logger logger = LoggerFactory.getLogger(InvokerInvocationHandler.class);

    private Class<?> serviceType;

    /**
     * 远程连接配置，包含ip相关信息
     */
    private RemoteConfig remoteConfig;

    private VenusExceptionFactory venusExceptionFactory;

    private DummyAuthenticator authenticator;


    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //权限认证
        //流控
        //降级
        //集群容错
        Invocation invocation = buildInvocation(proxy,method,args);
        Result result = getInvoker().invoke(invocation);
        return result.getObject();
    }

    /**
     * 构造请求
     * @param proxy
     * @param method
     * @param args
     * @return
     */
    Invocation buildInvocation(Object proxy, Method method, Object[] args){
        Invocation invocation = new Invocation();
        invocation.setServiceType(serviceType);
        invocation.setMethod(method);
        invocation.setArgs(args);
        invocation.setRemoteConfig(remoteConfig);
        return invocation;
    }

    Invoker getInvoker(){
        return new VenusInvoker();
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

    public VenusExceptionFactory getVenusExceptionFactory() {
        return venusExceptionFactory;
    }

    public void setVenusExceptionFactory(VenusExceptionFactory venusExceptionFactory) {
        this.venusExceptionFactory = venusExceptionFactory;
    }

    public DummyAuthenticator getAuthenticator() {
        return authenticator;
    }

    public void setAuthenticator(DummyAuthenticator authenticator) {
        this.authenticator = authenticator;
    }
}
