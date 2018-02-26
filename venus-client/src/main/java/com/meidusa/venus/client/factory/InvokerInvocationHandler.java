package com.meidusa.venus.client.factory;

import com.meidusa.venus.client.ClientInvocation;
import com.meidusa.venus.Result;
import com.meidusa.venus.ServiceFactory;
import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.annotations.Service;
import com.meidusa.venus.client.factory.xml.config.ClientRemoteConfig;
import com.meidusa.venus.client.factory.xml.config.ReferenceMethod;
import com.meidusa.venus.client.factory.xml.config.ReferenceService;
import com.meidusa.venus.client.invoker.ClientInvokerProxy;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.exception.VenusConfigException;
import com.meidusa.venus.io.packet.PacketConstant;
import com.meidusa.venus.io.utils.RpcIdUtil;
import com.meidusa.venus.metainfo.AnnotationUtil;
import com.meidusa.venus.metainfo.EndpointParameter;
import com.meidusa.venus.metainfo.EndpointParameterUtil;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.support.EndpointWrapper;
import com.meidusa.venus.support.ServiceWrapper;
import com.meidusa.venus.support.VenusContext;
import com.meidusa.venus.support.VenusUtil;
import com.meidusa.venus.util.JSONUtil;
import com.meidusa.venus.util.NetUtil;
import com.meidusa.venus.util.VenusLoggerFactory;
import com.meidusa.venus.util.VenusTracerUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

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
        //对object默认方法，不作远程调用
        if(isObjectInvoke(proxy, method,args)){
            return invokeObject(proxy, method, args);
        }

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
        //初始化service
        Service serviceAnno = AnnotationUtil.getAnnotation(method.getDeclaringClass().getAnnotations(), Service.class);
        if(serviceAnno == null){
            throw new VenusConfigException(String.format("service %s service annotation not declare",serviceInterface.getName()));
        }
        ServiceWrapper serviceWrapper = ServiceWrapper.wrapper(serviceAnno);
        invocation.setService(serviceWrapper);
        String serviceName = serviceAnno.name();
        if(StringUtils.isEmpty(serviceName)){
            serviceName = serviceInterface.getCanonicalName();
        }
        invocation.setServiceName(serviceName);
        invocation.setVersion(String.valueOf(serviceAnno.version()));
        //初始化endpoint
        Endpoint endpointAnno =  AnnotationUtil.getAnnotation(method.getAnnotations(), Endpoint.class);
        if(endpointAnno == null){
            throw new VenusConfigException(String.format("method %s endpoint annotation not declare",method.getName()));
        }
        EndpointWrapper endpointWrapper = EndpointWrapper.wrapper(endpointAnno);
        invocation.setEndpoint(endpointWrapper);
        //初始化apiName
        String apiName = VenusUtil.getApiName(method,serviceWrapper,endpointWrapper);
        invocation.setApiName(apiName);
        //方法相关
        EndpointParameter[] params = EndpointParameterUtil.getPrameters(method);
        invocation.setParams(params);
        invocation.setMethod(method);
        invocation.setArgs(args);
        //其它设置
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
            if(referenceMethod != null && referenceMethod.getTimeoutCfg() != 0){
                invocation.setTimeout(referenceMethod.getTimeoutCfg());
            }else if(referenceService.getTimeoutCfg() != 0){
                invocation.setTimeout(referenceService.getTimeoutCfg());
            }
            if(referenceMethod != null && referenceMethod.getRetriesCfg() != 0){
                invocation.setRetries(referenceMethod.getRetriesCfg());
            }else if(referenceService.getRetriesCfg() != 0){
                invocation.setRetries(referenceService.getRetriesCfg());
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
        String rpcId = invocation.getRpcId();
        String methodPath = invocation.getMethodPath();
        //参数
        String param = "{}";
        if(invocation.isEnablePrintParam() && !VenusUtil.isAthenaInterface(invocation)){
            if(invocation.getArgs() != null){
                param = JSONUtil.toJSONString(invocation.getArgs());
            }
        }
        //结果
        Object ret = "{}";
        if(invocation.isEnablePrintResult() && !VenusUtil.isAthenaInterface(invocation)){
            if(object != null){
                ret = JSONUtil.toJSONString(object);
            }
        }
        //异常
        Object error = "{}";
        if(invocation.isEnablePrintResult() && !VenusUtil.isAthenaInterface(invocation)){
            if(exception != null){
                hasException = true;
                error = exception;
            }
        }
        String status = "";
        if(hasException){
            status = "failed";
        }else if(usedTime > 1000){
            status = ">1000ms";
        }else if(usedTime > 500){
            status = ">500ms";
        }else if(usedTime > 200){
            status = ">200ms";
        }else{
            status = "<200ms";
        }

        //输出日志
        Logger trLogger = tracerLogger;
        if(VenusUtil.isAthenaInterface(invocation)){
            trLogger = logger;
        }
        if(hasException){
            String tpl = "[C] [{},{}],consumer invoke,rpcId:{},method:{},param:{},error:{}.";
            Object[] arguments = new Object[]{
                    status,
                    usedTime + "ms",
                    rpcId,
                    methodPath,
                    param,
                    error
            };
            if(trLogger.isErrorEnabled()){
                trLogger.error(tpl,arguments);
            }
            //错误日志
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error(tpl,arguments);
            }
        }else{
            String tpl = "[C] [{},{}],consumer invoke,rpcId:{},method:{},param:{},result:{}.";
            Object[] arguments = new Object[]{
                    status,
                    usedTime + "ms",
                    rpcId,
                    methodPath,
                    param,
                    ret
            };
            if(usedTime > 200){
                if(trLogger.isWarnEnabled()){
                    trLogger.warn(tpl,arguments);
                }
            }else{
                if(trLogger.isInfoEnabled()){
                    trLogger.info(tpl,arguments);
                }
            }
        }

    }

    /**
     * 判断是否为Object内置方法，如toString,equals,hashcode等
     * @param proxy
     * @param method
     * @return
     */
    boolean isObjectInvoke(Object proxy, Method method, Object[] args){
        Class<?> declaringClazz = method.getDeclaringClass();
        if(declaringClazz == null){
            throw new IllegalStateException(String.valueOf(method));
        }
        return Object.class == declaringClazz;
    }

    /**
     * 调用object内置方法
     * @param proxy
     * @param method
     * @return
     */
    Object invokeObject(Object proxy, Method method, Object[] args){
        String name = method.getName();
        if ("equals".equals(name)) {
            return proxy == args[0];
        } else if ("hashCode".equals(name)) {
            return System.identityHashCode(proxy);
        } else if ("toString".equals(name)) {
            return proxy.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(proxy)) + "," + this;
        } else {
            throw new IllegalStateException(String.valueOf(method));
        }
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
