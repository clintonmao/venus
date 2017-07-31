package com.meidusa.venus.client.proxy;

import com.meidusa.venus.client.authenticate.DummyAuthenticator;
import com.meidusa.venus.client.factory.xml.config.RemoteConfig;
import com.meidusa.venus.Invocation;
import com.meidusa.venus.client.invoker.Invoker;
import com.meidusa.venus.Result;
import com.meidusa.venus.client.invoker.venus.VenusInvoker;
import com.meidusa.venus.exception.VenusExceptionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 服务调用代理，执行寻址/路由/认证/流控/降级及远程调用等逻辑
 * @author Struct
 */

public class InvokerInvocationHandler implements InvocationHandler {

    private static Logger logger = LoggerFactory.getLogger(InvokerInvocationHandler.class);

    /**
     * 服务接口类型
     */
    private Class<?> serviceType;

    /**
     * 静态连接配置
     */
    private RemoteConfig remoteConfig;

    /**
     * 注册中心地址
     */
    private String registerUrl;

    /**
     * 异常处理
     */
    private VenusExceptionFactory venusExceptionFactory;

    /**
     * 认证配置
     */
    private DummyAuthenticator authenticator;


    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Invocation invocation = buildInvocation(proxy,method,args);
        //TODO 权限认证
        //TODO 寻址
        //TODO 路由
        //TODO 负载均衡
        //TODO 流控
        //TODO 降级
        //TODO 集群容错
        Invoker invoker = getInvoker();
        Result result = invoker.invoke(invocation);
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
        //invocation.setRemoteConfig(remoteConfig);
        return invocation;
    }

    /**
     * 获取invoker
     * @return
     */
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

    public String getRegisterUrl() {
        return registerUrl;
    }

    public void setRegisterUrl(String registerUrl) {
        this.registerUrl = registerUrl;
    }
}
