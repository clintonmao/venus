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
import com.meidusa.venus.io.packet.PacketConstant;
import com.meidusa.venus.io.utils.RpcIdUtil;
import com.meidusa.venus.metainfo.AnnotationUtil;
import com.meidusa.venus.metainfo.EndpointParameter;
import com.meidusa.venus.metainfo.EndpointParameterUtil;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.support.EndpointWrapper;
import com.meidusa.venus.support.ServiceWrapper;
import com.meidusa.venus.support.VenusContext;
import com.meidusa.venus.util.JSONUtil;
import com.meidusa.venus.util.NetUtil;
import com.meidusa.venus.util.VenusLoggerFactory;
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

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    private static Logger tracerLogger = VenusLoggerFactory.getTracerLogger();

    /**
     * 服务接口类型
     */
    private Class<?> serviceInterface;

    /**
     * service工厂
     */
    private ServiceFactory serviceFactory;

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

    public InvokerInvocationHandler(){
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        long bTime = System.currentTimeMillis();
        ClientInvocation invocation = null;
        Object object = null;
        Throwable exception = null;
        try {
            //构造请求
            invocation = buildInvocation(proxy, method, args);

            //通过代理调用服务
            Result result = getClientInvokerProxy().invoke(invocation,null);

            //处理结果
            if(result.getErrorCode() == 0 && result.getException() == null){//调用成功
                object = result.getResult();
                return object;
            }else{//调用失败
                exception = buildException(result);
                throw exception;
            }
        } catch (Throwable t) {
            exception = t;
            throw exception;
        } finally {
            try {
                //输出tracer日志
                printTracerLogger(invocation,object,exception,bTime);
            } catch (Exception e) {}
        }
    }

    /**
     * 输出tracer日志
     * @param invocation
     * @param object
     * @param exception
     * @param bTime
     */
    void printTracerLogger(ClientInvocation invocation,Object object,Throwable exception,long bTime){
        //构造参数
        boolean hasException = false;
        long usedTime = System.currentTimeMillis() - bTime;
        String invokeModel = invocation.getInvokeModel();
        String rpcId = invocation.getRpcId();
        String methodPath = invocation.getMethodPath();
        String param = "";
        if(invocation.isEnablePrintParam() && invocation.getArgs() != null){
            param = JSONUtil.toJSONString(invocation.getArgs());
        }
        Object output = "";
        if(invocation.isEnablePrintResult()){
            if(object != null){
                output = JSONUtil.toJSONString(object);
            }else if(exception != null){
                hasException = true;
                output = exception;
            }
        }
        String status = "";
        if(hasException){
            status = "failed";
        }else if(usedTime > 1000){
            status = "slow>1000ms";
        }else if(usedTime > 500){
            status = "slow>500ms";
        }else if(usedTime > 200){
            status = "slow>200ms";
        }else{
            status = "success";
        }

        //打印结果
        String tpl = "{} invoke,rpcId:{},method:{},status:{},used time:{}ms,param:{},result:{}.";
        Object[] arguments = new Object[]{
                invokeModel,
                rpcId,
                methodPath,
                status,
                usedTime,
                param,
                output
        };
        if(hasException){
            //输出错误日志
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error(tpl,arguments);
            }
            if(tracerLogger.isErrorEnabled()){
                tracerLogger.error(tpl,arguments);
            }
        }else if(usedTime > 200){
            if(tracerLogger.isWarnEnabled()){
                tracerLogger.warn(tpl,arguments);
            }
        }else{
            if(tracerLogger.isInfoEnabled()){
                tracerLogger.info(tpl,arguments);
            }
        }

    }

    /**
     * 获取client调用代理
     * @return
     */
    public ClientInvokerProxy getClientInvokerProxy() {
        clientInvokerProxy.setRemoteConfig(getRemoteConfig());
        clientInvokerProxy.setRegister(register);
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

    /**
     * 将错误信息转化为异常
     * @param result
     * @return
     */
    Throwable buildException(Result result){
        Throwable rex = null;
        Throwable exception = result.getException();
        if(exception == null){
            rex = new RpcException(String.format("%s-%s",String.valueOf(result.getErrorCode()),result.getErrorMessage()));
        }

        //若是rpc异常，则判断是否为包装异常
        if(exception instanceof RpcException){
            if(exception.getCause() != null){
                rex = exception.getCause();
            }else{
                rex = exception;
            }
        }else{
            rex = exception;
        }
        return rex;
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
