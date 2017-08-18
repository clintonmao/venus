package com.meidusa.venus.client.proxy;

import com.meidusa.venus.*;
import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.annotations.Service;
import com.meidusa.venus.annotations.util.AnnotationUtil;
import com.meidusa.venus.client.authenticate.DummyAuthenticator;
import com.meidusa.venus.client.cluster.FailoverClusterInvoker;
import com.meidusa.venus.client.factory.simple.SimpleServiceFactory;
import com.meidusa.venus.client.factory.xml.config.RemoteConfig;
import com.meidusa.venus.client.filter.limit.ActivesLimitFilter;
import com.meidusa.venus.client.filter.limit.TpsLimitFilter;
import com.meidusa.venus.client.filter.mock.MockFilterProxy;
import com.meidusa.venus.client.filter.valid.ValidFilter;
import com.meidusa.venus.client.invoker.injvm.InjvmInvoker;
import com.meidusa.venus.client.router.Router;
import com.meidusa.venus.client.router.condition.ConditionRouter;
import com.meidusa.venus.exception.VenusExceptionFactory;
import com.meidusa.venus.metainfo.EndpointParameter;
import com.meidusa.venus.metainfo.EndpointParameterUtil;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.RegisterService;
import com.meidusa.venus.registry.mysql.MysqlRegister;
import com.meidusa.venus.service.registry.HostPort;
import com.meidusa.venus.service.registry.ServiceDefinition;
import com.meidusa.venus.util.NetUtil;
import com.meidusa.venus.util.VenusTracerUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
    private String registerUrl = "192.168.1.1:9000";

    /**
     * 注册中心
     */
    private Register register;

    /**
     * 异常处理
     */
    private VenusExceptionFactory venusExceptionFactory;

    /**
     * 认证配置
     */
    private DummyAuthenticator authenticator;

    /**
     * 路由服务
     */
    private Router router = new ConditionRouter();

    /**
     * jvm内部调用
     */
    private InjvmInvoker injvmInvoker;


    public InvokerInvocationHandler(){
        init();
    }

    /**
     * 初始化
     */
    void init(){
        subscrible();
    }

    /**
     * 订阅服务
     */
    void subscrible(){
        String subscribleUrl = "subscrible://com.chexiang.venus.demo.provider.HelloService/helloService?version=1.0.0&host=" + NetUtil.getLocalIp();
        URL url = URL.parse(subscribleUrl);
        logger.info("url:{}",url);
        getRegister().subscrible(url);
    }

    /**
     * 获取注册中心
     * @return
     */
    Register getRegister(){
        if(register != null){
            return register;
        }
        //TODO 对于远程，使用registerUrl初始化remoteRegisterService
        register = MysqlRegister.getInstance(true,null);
        return register;
    }

    /**
     * 获取注册中心远程服务
     * @param registerUrl
     * @return
     */
    RegisterService getRegisterService(String registerUrl){
        String[] split = registerUrl.split(";");
        List<HostPort> hosts = new ArrayList<HostPort>();
        for (int i = 0; i < split.length; i++) {
            String str = split[i];
            String[] split2 = str.split(":");
            if (split2.length > 1) {
                String host = split2[0];
                String port = split2[1];
                HostPort hp = new HostPort(host, Integer.parseInt(port));
                hosts.add(hp);
            }
        }

        HostPort hp = hosts.get(new Random().nextInt(hosts.size()));
        SimpleServiceFactory ssf = new SimpleServiceFactory(hp.getHost(), hp.getPort());
        ssf.setCoTimeout(60000);
        ssf.setSoTimeout(60000);
        RegisterService registerService = ssf.getService(RegisterService.class);
        return registerService;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            //构造请求对象
            Invocation invocation = buildInvocation(proxy,method,args);

            //前置处理，校验、流控、降级等
            for(Filter filter : getFilters()){
                Result result = filter.invoke(invocation,null);
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
        } catch (Throwable e) {
            //TODO 处理异常
            throw  e;
        }
    }

    /**
     * jvm内部调用
     * @param invocation
     * @return
     */
    Result doInvokeInJvm(Invocation invocation){
        return injvmInvoker.invoke(invocation, null);
    }

    /**
     * 集群容错调用
     * @param invocation
     * @return
     */
    Result doInvokeInCluster(Invocation invocation){
        //寻址，TODO 地址变化对连接池的影响
        List<URL> urlList = lookup(invocation);

        //路由规则过滤
        urlList = router.filte(urlList, invocation);

        //集群调用
        Result result = getClusterInvoker().invoke(invocation, urlList);
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
    List<URL> lookup(Invocation invocation){
        if(remoteConfig != null){
            return lookupByDynamic(invocation);
        }else if(StringUtils.isNotEmpty(registerUrl)){
            return lookupByDynamic(invocation);
        }else{
            throw new RpcException("remoteConfig and registerUrl not allow empty.");
        }
    }

    /**
     * 静态寻址，直接配置addressList或remote
     * @param invocation
     * @return
     */
    List<URL> lookupByStatic(Invocation invocation){
        List<URL> urlList = new ArrayList<URL>();
        //TODO 确认及处理多个地址格式
        String ipAddressList = remoteConfig.getFactory().getIpAddressList();
        String[] arr = ipAddressList.split(":");
        URL url = new URL();
        url.setHost(arr[0]);
        url.setPort(Integer.parseInt(arr[1]));
        urlList.add(url);
        return urlList;
    }

    /**
     * 动态寻址，注册中心查找
     * @param invocation
     * @return
     */
    List<URL> lookupByDynamic(Invocation invocation){
        List<URL> urlList = new ArrayList<URL>();

        String path = "venus://com.chexiang.venus.demo.provider.HelloService/helloService?version=1.0.0";
        URL serviceUrl = URL.parse(path);
        ServiceDefinition serviceDefinition = getRegister().lookup(serviceUrl);
        if(serviceDefinition == null || CollectionUtils.isEmpty(serviceDefinition.getIpAddress())){
            throw new RpcException("service not found available providers.");
        }
        logger.info("serviceDefinition:{}",serviceDefinition);

        for(String item:serviceDefinition.getIpAddress()){
            String[] arr = item.split(":");
            URL url = new URL();
            url.setHost(arr[0]);
            url.setPort(Integer.parseInt(arr[1]));
            urlList.add(url);
        }
        return urlList;
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
    ClusterInvoker getClusterInvoker(){
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

}
