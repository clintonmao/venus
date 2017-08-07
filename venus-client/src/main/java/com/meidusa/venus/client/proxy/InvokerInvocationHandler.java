package com.meidusa.venus.client.proxy;

import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.annotations.Service;
import com.meidusa.venus.annotations.util.AnnotationUtil;
import com.meidusa.venus.Address;
import com.meidusa.venus.URL;
import com.meidusa.venus.client.authenticate.DummyAuthenticator;
import com.meidusa.venus.client.factory.xml.config.RemoteConfig;
import com.meidusa.venus.client.interceptor.valid.ValidInterceptor;
import com.meidusa.venus.rpc.*;
import com.meidusa.venus.client.cluster.FailoverClusterInvoker;
import com.meidusa.venus.exception.VenusExceptionFactory;
import com.meidusa.venus.rpc.limit.ActivesLimitInterceptor;
import com.meidusa.venus.rpc.limit.TpsLimitInterceptor;
import com.meidusa.venus.metainfo.EndpointParameter;
import com.meidusa.venus.metainfo.EndpointParameterUtil;
import com.meidusa.venus.rpc.mock.ReturnMockInvoker;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.mysql.MysqlRegister;
import com.meidusa.venus.rpc.router.Router;
import com.meidusa.venus.rpc.router.condition.ConditionRouter;
import com.meidusa.venus.service.registry.ServiceDefinition;
import com.meidusa.venus.util.VenusTracerUtil;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 服务调用代理，执行校验/认证/流控/降级/寻址/路由/调用/容错等逻辑
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
        //构造请求对象
        Invocation invocation = buildInvocation(proxy,method,args);

        //前置处理
        for(Interceptor interceptor:getInterceptors()){
            Result result = interceptor.intercept(invocation);
            if(result != null){
                //TODO 异常处理
                return result.getObject();
            }
        }

        //寻址，TODO 地址变化对连接池的影响
        List<Address> addressList = lookup(invocation);

        //路由规则过滤
        addressList = router.filter(addressList,invocation);

        //服务调用，直接放通调用或者集群容错调用
        if(isNeedMock(invocation)){
            //mock调用
            Invoker mockInvoker = getMockInvoker(invocation);
            Result result = mockInvoker.invoke(invocation);
            return result.getObject();
        }else{
            //集群调用
            Invoker clusterInvoker = getClusterInvoker(addressList,invocation);
            Result result = clusterInvoker.invoke(invocation);
            return result.getObject();
        }
    }

    /**
     * 获取interceptors TODO 初始化处理
     * @return
     */
    Interceptor[] getInterceptors(){
        return new Interceptor[]{
                //校验
                new ValidInterceptor(),
                //并发数流控
                new ActivesLimitInterceptor(),
                //TPS控制
                new TpsLimitInterceptor()
        };
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
     * 获取cluster invoker
     * @return
     */
    Invoker getClusterInvoker(List<Address> addressList,Invocation invocation){
        //TODO 处理集群容错wrapper
        return new FailoverClusterInvoker();
    }

    /**
     * 判断是否需要放通处理
     * @param invocation
     * @return
     */
    boolean isNeedMock(Invocation invocation){
        //TODO
        return false;
    }

    /**
     * 获取mock invoker
     * @param invocation
     * @return
     */
    Invoker getMockInvoker(Invocation invocation){
        //TODO 处理mock wrapper
        return new ReturnMockInvoker();
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
