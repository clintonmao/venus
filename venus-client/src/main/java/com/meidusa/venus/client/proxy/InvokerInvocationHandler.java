package com.meidusa.venus.client.proxy;

import com.meidusa.venus.*;
import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.annotations.Service;
import com.meidusa.venus.annotations.util.AnnotationUtil;
import com.meidusa.venus.client.authenticate.DummyAuthenticator;
import com.meidusa.venus.client.factory.xml.config.RemoteConfig;
import com.meidusa.venus.client.filter.valid.ValidFilter;
import com.meidusa.venus.client.invoker.injvm.InjvmInvoker;
import com.meidusa.venus.client.cluster.FailoverClusterInvoker;
import com.meidusa.venus.exception.VenusExceptionFactory;
import com.meidusa.venus.client.filter.limit.ActivesLimitFilter;
import com.meidusa.venus.client.filter.limit.TpsLimitFilter;
import com.meidusa.venus.metainfo.EndpointParameter;
import com.meidusa.venus.metainfo.EndpointParameterUtil;
import com.meidusa.venus.client.filter.mock.MockFilterProxy;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.mysql.MysqlRegister;
import com.meidusa.venus.client.router.Router;
import com.meidusa.venus.client.router.condition.ConditionRouter;
import com.meidusa.venus.service.registry.ServiceDefinition;
import com.meidusa.venus.util.VenusTracerUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
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

    /**
     * jvm内部调用
     */
    private InjvmInvoker injvmInvoker;

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            //构造请求对象
            Invocation invocation = buildInvocation(proxy,method,args);

            //前置处理，校验、流控、降级等
            for(Filter filter : getFilters()){
                Result result = filter.invoke(invocation);
                if(result != null){
                    return result;
                }
            }

            //根据配置选择jvm内部调用还是集群环境容错调用
            Result result = null;
            Service service = invocation.getService();
            Endpoint endpoint = invocation.getEndpoint();
            if (endpoint != null && service != null) {
                if (StringUtils.isEmpty(service.implement())) {
                    //集群容错调用
                    result = doInvokeInCluster(invocation);
                } else {
                    //jvm内部调用
                    result = doInvokeInJvm(invocation);
                }
            }else{
                //TODO 确认endpoint为空情况
                result = doInvokeInJvm(invocation);
            }

            //返回结果
            //TODO 处理成功调用，但失败情况
            return result.getObject();
        } catch (Exception e) {
            //TODO 处理异常
            return null;
        }
    }

    /**
     * jvm内部调用
     * @param invocation
     * @return
     */
    Result doInvokeInJvm(Invocation invocation){
        return injvmInvoker.invoke(invocation);
    }

    /**
     * 集群容错调用
     * @param invocation
     * @return
     */
    Result doInvokeInCluster(Invocation invocation){
        //寻址，TODO 地址变化对连接池的影响
        List<Address> addressList = lookup(invocation);

        //路由规则过滤
        addressList = router.filte(addressList, invocation);

        //集群调用
        Invoker clusterInvoker = getClusterInvoker(addressList,invocation);
        Result result = clusterInvoker.invoke(invocation);
        return result;
    }

    /**
     * 获取interceptors TODO 初始化处理
     * @return
     */
    Filter[] getFilters(){
        return new Filter[]{
                //校验
                new ValidFilter(),
                //流控
                new MockFilterProxy(),
                //并发数流控
                new ActivesLimitFilter(),
                //TPS控制
                new TpsLimitFilter()
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
