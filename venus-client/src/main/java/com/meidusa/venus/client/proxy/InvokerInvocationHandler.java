package com.meidusa.venus.client.proxy;

import com.meidusa.venus.Address;
import com.meidusa.venus.URL;
import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.annotations.Service;
import com.meidusa.venus.annotations.util.AnnotationUtil;
import com.meidusa.venus.client.RpcException;
import com.meidusa.venus.client.authenticate.DummyAuthenticator;
import com.meidusa.venus.client.factory.xml.config.RemoteConfig;
import com.meidusa.venus.Invocation;
import com.meidusa.venus.client.invoker.Invoker;
import com.meidusa.venus.Result;
import com.meidusa.venus.client.invoker.venus.VenusInvoker;
import com.meidusa.venus.exception.VenusExceptionFactory;
import com.meidusa.venus.metainfo.EndpointParameter;
import com.meidusa.venus.metainfo.EndpointParameterUtil;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.mysql.MysqlRegister;
import com.meidusa.venus.router.Router;
import com.meidusa.venus.router.condition.ConditionRouter;
import com.meidusa.venus.service.registry.ServiceDefinition;
import com.meidusa.venus.util.VenusTracerUtil;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * 服务调用代理，执行寻址/路由/认证/流控/降级/调用等逻辑
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

    /**
     * 注册中心
     */
    private Register register;

    /**
     * 路由服务
     */
    private Router router = new ConditionRouter();

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //TODO 将部分功能由aop实现
        Invocation invocation = buildInvocation(proxy,method,args);
        //TODO 校验
        valid(invocation);
        //TODO 寻址，地址变化对连接池的影响
        List<Address> addressList = lookup(invocation);
        //TODO 路由
        addressList = router.filter(addressList,invocation);
        //TODO 负载均衡
        //TODO 流控
        //TODO 降级
        //TODO 集群容错
        Invoker invoker = getInvoker();
        Result result = invoker.invoke(invocation);
        return result.getObject();
    }

    /**
     * 校验
     * @param invocation
     */
    void valid(Invocation invocation) throws IllegalAccessException {
        //endpoint定义校验
        if(invocation.getEndpoint() == null){
            Method method = invocation.getMethod();
            if (!method.getDeclaringClass().equals(Object.class)) {
                logger.error("remote invoke error: endpoint annotation not declare on method=" + method.getName());
                throw new IllegalAccessException("remote invoke error: endpoint annotation not declare on method=" + method.getName());
            }
        }
        //TODO 校验地址及注册中心不能都为空

    }

    /**
     * 查找服务提供者地址列表
     * @param invocation
     * @return
     */
    List<Address> lookup(Invocation invocation){
        //静态地址
        if(remoteConfig != null){
            //TODO 转化为addressList
            return null;
        }
        //注册中心查找
        URL url = null;
        ServiceDefinition serviceDefinition = getRegister(registerUrl).lookup(url);
        if(serviceDefinition == null || CollectionUtils.isEmpty(serviceDefinition.getIpAddress())){
            throw new RpcException(String.format("service % not found available providers.",serviceType.getName()));
        }
        //TODO 若都为空，抛异常
        return null;
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
        Endpoint endpoint =  AnnotationUtil.getAnnotation(method.getAnnotations(), Endpoint.class);
        invocation.setEndpoint(endpoint);
        if (endpoint != null) {
            Service service = AnnotationUtil.getAnnotation(method.getDeclaringClass().getAnnotations(), Service.class);
            invocation.setService(service);
            if (service != null && com.meidusa.toolkit.common.util.StringUtil.isEmpty(service.implement())) {
                EndpointParameter[] params = EndpointParameterUtil.getPrameters(method);
                invocation.setParams(params);
            }
        }

        //设置traceId
        byte[] traceID = VenusTracerUtil.getTracerID();
        if (traceID == null) {
            traceID = VenusTracerUtil.randomTracerID();
        }
        //设置调用方式
        boolean async = false;
        if (endpoint.async()) {
            async = true;
        }
        return invocation;
    }

    /**
     * 获取invoker
     * @return
     */
    Invoker getInvoker(){
        //TODO 处理集群容错wrapper
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

    /**
     * 根据注册中心url获取注册中心对象
     * @param registerUrl
     * @return
     */
    public Register getRegister(String registerUrl) {
        //TODO 缓存
        return new MysqlRegister();
    }
}
