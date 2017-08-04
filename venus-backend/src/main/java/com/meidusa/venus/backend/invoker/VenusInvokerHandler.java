package com.meidusa.venus.backend.invoker;

import com.meidusa.fastjson.JSON;
import com.meidusa.fastmark.feature.SerializerFeature;
import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.toolkit.net.Connection;
import com.meidusa.venus.annotations.ExceptionCode;
import com.meidusa.venus.annotations.RemoteException;
import com.meidusa.venus.backend.invoker.callback.RemotingInvocationListener;
import com.meidusa.venus.backend.invoker.sync.EndpointInvocation;
import com.meidusa.venus.backend.support.RequestInfo;
import com.meidusa.venus.backend.support.Response;
import com.meidusa.venus.backend.invoker.support.RequestHandler;
import com.meidusa.venus.backend.invoker.support.RpcInvocation;
import com.meidusa.venus.backend.services.Endpoint;
import com.meidusa.venus.backend.services.Service;
import com.meidusa.venus.backend.services.ServiceManager;
import com.meidusa.venus.backend.services.xml.bean.PerformanceLogger;
import com.meidusa.venus.backend.support.RequestContext;
import com.meidusa.venus.exception.*;
import com.meidusa.venus.io.ServiceFilter;
import com.meidusa.venus.io.network.VenusFrontendConnection;
import com.meidusa.venus.io.packet.*;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;
import com.meidusa.venus.notify.InvocationListener;
import com.meidusa.venus.notify.ReferenceInvocationListener;
import com.meidusa.venus.rpc.RpcException;
import com.meidusa.venus.util.ClasspathAnnotationScanner;
import com.meidusa.venus.util.Range;
import com.meidusa.venus.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * venus协议提供方服务调用
 * Created by Zhangzhihua on 2017/8/2.
 */
public class VenusInvokerHandler {

    private static Logger logger = LoggerFactory.getLogger(VenusInvokerHandler.class);

    private static Logger performanceLogger = LoggerFactory.getLogger("venus.backend.performance");

    private static Logger performancePrintResultLogger = LoggerFactory.getLogger("venus.backend.print.result");

    private static Logger performancePrintParamsLogger = LoggerFactory.getLogger("venus.backend.print.params");

    private static final String TIMEOUT = "waiting-timeout for execution,api=%s,ip=%s,time=%d (ms)";

    private static String ENDPOINT_INVOKED_TIME = "invoked Totle Time: ";

    private static SerializerFeature[] JSON_FEATURE = new SerializerFeature[]{SerializerFeature.ShortString,SerializerFeature.IgnoreNonFieldGetter,SerializerFeature.SkipTransientField};

    private VenusExceptionFactory venusExceptionFactory;

    private ServiceManager serviceManager;

    private ServiceFilter filter;

    private Executor executor;

    static Map<Class<?>,Integer> codeMap = new HashMap<Class<?>,Integer>();

    static {
        Map<Class<?>,ExceptionCode>  map = ClasspathAnnotationScanner.find(Exception.class,ExceptionCode.class);
        if(map != null){
            for(Map.Entry<Class<?>, ExceptionCode> entry:map.entrySet()){
                codeMap.put(entry.getKey(), entry.getValue().errorCode());
            }
        }

        Map<Class<?>,RemoteException> rmap = ClasspathAnnotationScanner.find(Exception.class,RemoteException.class);

        if(rmap != null){
            for(Map.Entry<Class<?>, RemoteException> entry:rmap.entrySet()){
                codeMap.put(entry.getKey(), entry.getValue().errorCode());
            }
        }
    }

    /**
     * doInvoke
     * @return
     */
    public void invoke(VenusFrontendConnection conn,RpcInvocation invocation) throws RpcException{
        //获取调用信息
        Tuple<Long, byte[]> data = invocation.getData();
        byte[] message = invocation.getMessage();
        byte packetSerializeType = invocation.getPacketSerializeType();
        long waitTime = invocation.getWaitTime();
        String finalSourceIp = invocation.getFinalSourceIp();
        VenusRouterPacket routerPacket = invocation.getRouterPacket();
        byte serializeType = invocation.getSerializeType();
        SerializeServiceRequestPacket request = invocation.getServiceRequestPacket();
        final String apiName = request.apiName;
        final Endpoint endpoint = invocation.getEp();//getServiceManager().getEndpoint(serviceName, methodName, request.parameterMap.keySet().toArray(new String[] {}));
        /*
        int index = apiName.lastIndexOf(".");
        String serviceName = request.apiName.substring(0, index);
        String methodName = request.apiName.substring(index + 1);
        RequestInfo info = getRequestInfo(conn, request);
        */
        //获取方法返回结果类型
        EndpointInvocation.ResultType resultType = getResultType(endpoint);

        //初始化参数信息
        RemotingInvocationListener<Serializable> invocationListener = null;
        initParamsAndInvocationListener(request,invocationListener,conn,routerPacket);

        //有效性校验
        boolean isValid = validRequest(conn,invocation,resultType,invocationListener);
        if(!isValid){
            return;
        }

        //TODO 处理各种横切面操作，如认证、监控、流控、降级等

        //构造请求上下文信息
        RequestHandler requestHandler = new RequestHandler();
        RequestInfo requestInfo = requestHandler.getRequestInfo(packetSerializeType, conn, routerPacket);
        RequestContext context = requestHandler.createContext(requestInfo, endpoint, request);

        //异步调用服务
        VenusInvokerTask runnable = new VenusInvokerTask(conn, endpoint,
                context, resultType,
                filter, routerPacket,
                request, serializeType,
                invocationListener, venusExceptionFactory,
                data);

        if (executor == null) {
            runnable.run();
        } else {
            executor.execute(runnable);
        }

        //end
    }

    /**
     * 校验请求有效性，有效:true;错误:false
     * @param conn
     * @param invocation
     * @param resultType
     * @param invocationListener
     * @return
     */
    boolean validRequest(VenusFrontendConnection conn, RpcInvocation invocation, EndpointInvocation.ResultType resultType, RemotingInvocationListener<Serializable> invocationListener){
        Tuple<Long, byte[]> data = invocation.getData();
        byte[] message = invocation.getMessage();
        byte packetSerializeType = invocation.getPacketSerializeType();
        long waitTime = invocation.getWaitTime();
        String finalSourceIp = invocation.getFinalSourceIp();
        VenusRouterPacket routerPacket = invocation.getRouterPacket();
        byte serializeType = invocation.getSerializeType();
        SerializeServiceRequestPacket request = invocation.getServiceRequestPacket();
        final String apiName = request.apiName;
        final Endpoint endpoint = invocation.getEp();

        // service version error
        ErrorPacket errorPacket = null;

        if (errorPacket == null) {
            errorPacket = checkVersion(endpoint, request);
        }
        if (errorPacket == null) {
            errorPacket = checkActive(endpoint, request);
        }

        /**
         * 判断超时
         */
        boolean isTimeout = false;
        if (errorPacket == null) {
            errorPacket = checkTimeout(conn,endpoint, request, waitTime);
            if(errorPacket != null){
                isTimeout = true;
            }
        }


        __TIMEOUT:{
            if (errorPacket != null) {
                if (resultType == EndpointInvocation.ResultType.NOTIFY) {
                    if(isTimeout){
                        break __TIMEOUT;
                    }
                    if (invocationListener != null) {
                        invocationListener.onException(new ServiceVersionNotAllowException(errorPacket.message));
                    } else {
                        postMessageBack(conn, routerPacket, request, errorPacket);
                    }
                } else {
                    postMessageBack(conn, routerPacket, request, errorPacket);
                }
                if(filter != null){
                    filter.before(request);
                }
                logPerformance(endpoint,UUID.toString(request.traceId),apiName,waitTime,0,conn.getHost(),finalSourceIp,request.clientId,request.clientRequestId,request.parameterMap, errorPacket);
                if(filter != null){
                    filter.after(errorPacket);
                }
                //TODO 修改返回方式
                return false;
            }
        }

        //没有错误
        return true;
    }
    /**
     * 根据方法定义获取返回类型
     * @param endpoint
     * @return
     */
    EndpointInvocation.ResultType getResultType(Endpoint endpoint){
        EndpointInvocation.ResultType resultType = EndpointInvocation.ResultType.RESPONSE;
        if (endpoint.isVoid()) {
            resultType = EndpointInvocation.ResultType.OK;
            if (endpoint.isAsync()) {
                resultType = EndpointInvocation.ResultType.NONE;
            }

            for (Class clazz : endpoint.getMethod().getParameterTypes()) {
                if (InvocationListener.class.isAssignableFrom(clazz)) {
                    resultType = EndpointInvocation.ResultType.NOTIFY;
                    break;
                }
            }
        }
        return resultType;
    }

    /**
     * 初始化参数信息
     * @param request
     * @param invocationListener
     * @param conn
     * @param routerPacket
     */
    void initParamsAndInvocationListener(SerializeServiceRequestPacket request,RemotingInvocationListener<Serializable> invocationListener,VenusFrontendConnection conn,VenusRouterPacket routerPacket){
        for (Map.Entry<String, Object> entry : request.parameterMap.entrySet()) {
            if (entry.getValue() instanceof ReferenceInvocationListener) {
                invocationListener = new RemotingInvocationListener<Serializable>(conn, (ReferenceInvocationListener) entry.getValue(), request,
                        routerPacket);
                request.parameterMap.put(entry.getKey(), invocationListener);
            }
        }
    }

    public void postMessageBack(Connection conn, VenusRouterPacket routerPacket, AbstractServicePacket source, AbstractServicePacket result) {
        if (routerPacket == null) {
            conn.write(result.toByteBuffer());
        } else {
            routerPacket.data = result.toByteArray();
            conn.write(routerPacket.toByteBuffer());
        }
    }

    private static ErrorPacket checkTimeout(VenusFrontendConnection conn, Endpoint endpoint, AbstractServiceRequestPacket request, long waitTime) {
        if (waitTime > endpoint.getTimeWait()) {
            ErrorPacket error = new ErrorPacket();
            AbstractServicePacket.copyHead(request, error);
            error.errorCode = VenusExceptionCodeConstant.INVOCATION_ABORT_WAIT_TIMEOUT;
            error.message = String.format(TIMEOUT, request.apiName,conn.getLocalHost(),waitTime);
            return error;
        }

        return null;
    }

    private static ErrorPacket checkActive(Endpoint endpoint, AbstractServiceRequestPacket request) {
        Service service = endpoint.getService();
        if (!service.isActive() || !endpoint.isActive()) {
            ErrorPacket error = new ErrorPacket();
            AbstractServicePacket.copyHead(request, error);
            error.errorCode = VenusExceptionCodeConstant.SERVICE_INACTIVE_EXCEPTION;
            StringBuffer buffer = new StringBuffer();
            buffer.append("Service=").append(endpoint.getService().getName());
            if (!service.isActive()) {
                buffer.append(" is not active");
            }

            if (!endpoint.isActive()) {
                buffer.append(", endpoint=").append(endpoint.getName()).append(" is not active");
            }

            error.message = buffer.toString();
            return error;
        }

        return null;
    }

    private static ErrorPacket checkVersion(Endpoint endpoint, AbstractServiceRequestPacket request) {
        Service service = endpoint.getService();

        // service version check
        Range range = service.getVersionRange();
        if (range == null || range.contains(request.serviceVersion)) {
            return null;
        } else {
            ErrorPacket error = new ErrorPacket();
            AbstractServicePacket.copyHead(request, error);
            error.errorCode = VenusExceptionCodeConstant.SERVICE_VERSION_NOT_ALLOWD_EXCEPTION;
            error.message = "Service=" + endpoint.getService().getName() + ",version=" + request.serviceVersion + " not allow";
            return error;
        }
    }

    protected void logPerformance(Endpoint endpoint,String traceId,String apiName,long queuedTime,
                                  long executTime,String remoteIp,String sourceIP, long clientId,long requestId,
                                  Map<String,Object > parameterMap,Object result){
        StringBuffer buffer = new StringBuffer();
        buffer.append("[").append(queuedTime).append(",").append(executTime).append("]ms, (*server*) traceID=").append(traceId).append(", api=").append(apiName).append(", ip=")
                .append(remoteIp).append(", sourceIP=").append(sourceIP).append(", clientID=")
                .append(clientId).append(", requestID=").append(requestId);

        PerformanceLogger pLevel = null;

        if(endpoint != null){
            //TODO 确认代码功能
            //pLevel = endpoint.getPerformanceLogger();
        }

        if (pLevel != null) {

            if (pLevel.isPrintParams()) {
                buffer.append(", params=");
                buffer.append(JSON.toJSONString(parameterMap,JSON_FEATURE));
            }
            if (pLevel.isPrintResult()) {
                buffer.append(", result=");
                if(result instanceof ErrorPacket){
                    buffer.append("{ errorCode=").append(((ErrorPacket) result).errorCode);
                    buffer.append(", message=").append(((ErrorPacket) result).message);
                    buffer.append("}");
                }else if(result instanceof Response){
                    if(((Response) result).getErrorCode()>0){
                        buffer.append("{ errorCode=").append(((Response) result).getErrorCode());
                        buffer.append(", message=\"").append(((Response) result).getErrorMessage()).append("\"");
                        buffer.append(", className=\"").append(((Response) result).getException().getClass().getSimpleName()).append("\"");
                        buffer.append("}");
                    }else{
                        buffer.append(JSON.toJSONString(result,JSON_FEATURE));
                    }
                }
            }

            if (queuedTime >= pLevel.getError() || executTime >= pLevel.getError() || queuedTime + executTime >= pLevel.getError()) {
                if (performanceLogger.isErrorEnabled()) {
                    performanceLogger.error(buffer.toString());
                }
            } else if (queuedTime >= pLevel.getWarn() || executTime >= pLevel.getWarn() || queuedTime + executTime >= pLevel.getWarn()) {
                if (performanceLogger.isWarnEnabled()) {
                    performanceLogger.warn(buffer.toString());
                }
            } else if (queuedTime >= pLevel.getInfo() || executTime >= pLevel.getInfo() || queuedTime + executTime >= pLevel.getInfo()) {
                if (performanceLogger.isInfoEnabled()) {
                    performanceLogger.info(buffer.toString());
                }
            } else {
                if (performanceLogger.isDebugEnabled()) {
                    performanceLogger.debug(buffer.toString());
                }
            }

        } else {
            buffer.append(", params=");
            if (performancePrintParamsLogger.isDebugEnabled()) {
                buffer.append(JSON.toJSONString(parameterMap,JSON_FEATURE));
            }else{
                buffer.append("{print.params:disabled}");
            }

            if (result == null) {
                buffer.append(", result=<null>");
            } else {
                buffer.append(", result=");
                if(result instanceof ErrorPacket){
                    buffer.append("{ errorCode=").append(((ErrorPacket) result).errorCode);
                    buffer.append(", message=").append(((ErrorPacket) result).message);
                    buffer.append("}");
                }else if(result instanceof Response){
                    if(((Response) result).getErrorCode()>0){
                        buffer.append("{errorCode=").append(((Response) result).getErrorCode());
                        buffer.append(", message=\"").append(((Response) result).getErrorMessage()).append("\"");
                        buffer.append(", className=\"").append(((Response) result).getException().getClass().getSimpleName()).append("\"");
                        buffer.append("}");
                    }else{
                        if (performancePrintResultLogger.isDebugEnabled()) {
                            buffer.append(JSON.toJSONString(result,new SerializerFeature[]{SerializerFeature.ShortString}));
                        }else{
                            buffer.append("{print.result:disabled}");
                        }
                    }
                }
            }
            if (queuedTime >= 5 * 1000 || executTime >= 5 * 1000 || queuedTime + executTime >= 5 * 1000) {
                if (performanceLogger.isErrorEnabled()) {
                    performanceLogger.error(buffer.toString());
                }
            } else if (queuedTime >= 3 * 1000 || executTime >= 3 * 1000 || queuedTime + executTime >= 3 * 1000) {
                if (performanceLogger.isWarnEnabled()) {
                    performanceLogger.warn(buffer.toString());
                }
            } else if (queuedTime >= 1 * 1000 || executTime >= 1 * 1000 || queuedTime + executTime >= 1 * 1000) {
                if (performanceLogger.isInfoEnabled()) {
                    performanceLogger.info(buffer.toString());
                }
            } else {
                if (performanceLogger.isDebugEnabled()) {
                    performanceLogger.debug(buffer.toString());
                }
            }
        }
    }

    private void logDependsOnLevel(ExceptionLevel level, Logger specifiedLogger, String msg, Throwable e) {
        switch (level) {
            case DEBUG:
                specifiedLogger.debug(msg, e);
                break;
            case INFO:
                specifiedLogger.info(msg, e);
                break;
            case TRACE:
                specifiedLogger.trace(msg, e);
                break;
            case WARN:
                specifiedLogger.warn(msg, e);
                break;
            case ERROR:
                specifiedLogger.error(msg, e);
                break;
            default:
                break;
        }
    }

    public ServiceManager getServiceManager() {
        return serviceManager;
    }

    public void setServiceManager(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    public VenusExceptionFactory getVenusExceptionFactory() {
        return venusExceptionFactory;
    }

    public void setVenusExceptionFactory(VenusExceptionFactory venusExceptionFactory) {
        this.venusExceptionFactory = venusExceptionFactory;
    }
}
