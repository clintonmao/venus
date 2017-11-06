package com.meidusa.venus.client.factory;

import com.meidusa.venus.ClientInvocation;
import com.meidusa.venus.Result;
import com.meidusa.venus.ServiceFactory;
import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.annotations.Service;
import com.meidusa.venus.client.authenticate.DummyAuthenticator;
import com.meidusa.venus.client.factory.xml.config.ClientRemoteConfig;
import com.meidusa.venus.client.factory.xml.config.ReferenceMethod;
import com.meidusa.venus.client.factory.xml.config.ReferenceService;
import com.meidusa.venus.client.invoker.ClientInvokerProxy;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.exception.VenusExceptionFactory;
import com.meidusa.venus.io.packet.PacketConstant;
import com.meidusa.venus.io.utils.RpcIdUtil;
import com.meidusa.venus.metainfo.AnnotationUtil;
import com.meidusa.venus.metainfo.EndpointParameter;
import com.meidusa.venus.metainfo.EndpointParameterUtil;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.support.EndpointWrapper;
import com.meidusa.venus.support.ServiceWrapper;
import com.meidusa.venus.support.VenusContext;
import com.meidusa.venus.util.NetUtil;
import com.meidusa.venus.util.VenusTracerUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 客户端服务调用代理
 * @author Struct
 */

public class InvokerInvocationHandler implements InvocationHandler {

    private static Logger logger = LoggerFactory.getLogger(InvokerInvocationHandler.class);

    /**
     * 服务接口类型
     */
    private Class<?> serviceInterface;

    /**
     * 异常处理
     */
    private VenusExceptionFactory venusExceptionFactory;

    /**
     * service工厂
     */
    private ServiceFactory serviceFactory;

    /**
     * 认证配置
     */
    private DummyAuthenticator authenticator;

    /**
     * 引用服务配置
     */
    private ReferenceService referenceService;

    /**
     * 静态配置地址
     */
    private ClientRemoteConfig remoteConfig;

    /**
     * 注册中心
     */
    private Register register;

    private ClientInvokerProxy clientInvokerProxy = new ClientInvokerProxy();

    private static AtomicLong sequenceId = new AtomicLong(1);

    static boolean isInited;

    public InvokerInvocationHandler(){
        if(!isInited){
            init();
            isInited = true;
        }
    }

    /**
     * 初始化操作
     */
    void init(){
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            //构造请求
            ClientInvocation invocation = buildInvocation(proxy, method, args);

            //通过代理调用服务
            Result result = getClientInvokerProxy().invoke(invocation,null);

            if(result.getErrorCode() == 0){//调用成功
                return result.getResult();
            }else{//调用失败
                Throwable exception = result.getException();
                if(exception != null){
                    //若是rpc异常，则判断是否为包装异常
                    throw exception;
                }else{
                    throw new RpcException(String.format("%s-%s",String.valueOf(result.getErrorCode()),result.getErrorMessage()));
                }
            }
        } catch (Throwable e) {
            if(logger.isErrorEnabled()){
                logger.error("invoke error.",e);
            }
            throw e;
        }
    }

    /**
     * 获取client调用代理
     * @return
     */
    public ClientInvokerProxy getClientInvokerProxy() {
        //TODO auth/exceptionFactory通过懒加载注入
        clientInvokerProxy.setAuthenticator(getAuthenticator());
        clientInvokerProxy.setVenusExceptionFactory(getVenusExceptionFactory());
        clientInvokerProxy.setRegister(register);
        clientInvokerProxy.setRemoteConfig(getRemoteConfig());
        return clientInvokerProxy;
    }

    /**
     * 构造请求
     * @param proxy
     * @param method
     * @param args
     * @return
     */
    ClientInvocation buildInvocation(Object proxy, Method method, Object[] args){
        ClientInvocation invocation = new ClientInvocation();
        invocation.setServiceInterface(serviceInterface);
        Endpoint endpoint =  AnnotationUtil.getAnnotation(method.getAnnotations(), Endpoint.class);
        EndpointWrapper endpointWrapper = EndpointWrapper.wrapper(endpoint);
        invocation.setEndpoint(endpointWrapper);
        Service service = AnnotationUtil.getAnnotation(method.getDeclaringClass().getAnnotations(), Service.class);
        ServiceWrapper serviceWrapper = ServiceWrapper.wrapper(service);
        invocation.setService(serviceWrapper);
        invocation.setVersion(String.valueOf(service.version()));
        EndpointParameter[] params = EndpointParameterUtil.getPrameters(method);
        invocation.setParams(params);
        invocation.setMethod(method);
        invocation.setArgs(args);
        invocation.setRequestTime(new Date());
        String consumerApp = VenusContext.getInstance().getApplication();
        invocation.setConsumerApp(consumerApp);
        invocation.setConsumerIp(NetUtil.getLocalIp(true));
        invocation.setAsync(false);
        //clientId
        invocation.setClientId(PacketConstant.VENUS_CLIENT_ID);
        invocation.setClientRequestId(sequenceId.getAndIncrement());
        //设置rpcId
        invocation.setRpcId(RpcIdUtil.getRpcId(invocation.getClientId(),invocation.getClientRequestId()));
        //设置traceId
        byte[] traceID = VenusTracerUtil.getTracerID();
        if (traceID == null) {
            traceID = VenusTracerUtil.randomTracerID();
        }
        invocation.setTraceID(traceID);
        if(register != null){
            invocation.setLookupType(1);
        }
        //设置自定义参数
        if(referenceService != null){
            ReferenceMethod referenceMethod = getReferenceMethod(method);
            if(referenceService.getCoreConnections() != 0){
                invocation.setCoreConnections(referenceService.getCoreConnections());
            }
            //timeout、retries支持方法级设置
            if(referenceMethod != null && referenceMethod.getTimeout() != 0){
                invocation.setTimeout(referenceMethod.getTimeout());
            }else if(referenceService.getTimeout() != 0){
                invocation.setTimeout(referenceService.getTimeout());
            }
            if(referenceMethod != null && referenceMethod.getRetries() != 0){
                invocation.setRetries(referenceMethod.getRetries());
            }else if(referenceService.getRetries() != 0){
                invocation.setRetries(referenceService.getRetries());
            }
            if(StringUtils.isNotEmpty(referenceService.getCluster())){
                invocation.setCluster(referenceService.getCluster());
            }
            if(StringUtils.isNotEmpty(referenceService.getLoadbalance())){
                invocation.setLoadbalance(referenceService.getLoadbalance());
            }
        }
        return invocation;
    }

    /**
     * 查找方法自定义配置项
     * @param method
     * @return
     */
    ReferenceMethod getReferenceMethod(Method method){
        List<ReferenceMethod> methodList = referenceService.getMethodList();
        if(CollectionUtils.isEmpty(methodList)){
            return null;
        }
        for(ReferenceMethod rMethod:methodList){
            if(rMethod.getName() != null && method.getName().equals(rMethod.getName())){
                return rMethod;
            }
        }
        return null;
    }

    public Class<?> getServiceInterface() {
        return serviceInterface;
    }

    public void setServiceInterface(Class<?> serviceInterface) {
        this.serviceInterface = serviceInterface;
    }

    public ClientRemoteConfig getRemoteConfig() {
        return remoteConfig;
    }

    public void setRemoteConfig(ClientRemoteConfig remoteConfig) {
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

    public Register getRegister() {
        return register;
    }

    public void setRegister(Register register) {
        this.register = register;
    }

    public ServiceFactory getServiceFactory() {
        return serviceFactory;
    }

    public void setServiceFactory(ServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
    }

    public ReferenceService getReferenceService() {
        return referenceService;
    }

    public void setReferenceService(ReferenceService referenceService) {
        this.referenceService = referenceService;
    }
}
