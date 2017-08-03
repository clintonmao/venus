package com.meidusa.venus.backend.invoker;

import com.meidusa.fastbson.exception.SerializeException;
import com.meidusa.fastjson.JSON;
import com.meidusa.fastjson.JSONException;
import com.meidusa.fastmark.feature.SerializerFeature;
import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.toolkit.net.Connection;
import com.meidusa.venus.annotations.ExceptionCode;
import com.meidusa.venus.annotations.RemoteException;
import com.meidusa.venus.backend.support.RequestInfo;
import com.meidusa.venus.backend.support.Response;
import com.meidusa.venus.backend.invoker.support.RequestHandler;
import com.meidusa.venus.backend.invoker.support.ProviderInvocation;
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
import com.meidusa.venus.io.serializer.Serializer;
import com.meidusa.venus.io.serializer.SerializerFactory;
import com.meidusa.venus.notify.InvocationListener;
import com.meidusa.venus.notify.ReferenceInvocationListener;
import com.meidusa.venus.rpc.RpcException;
import com.meidusa.venus.util.ClasspathAnnotationScanner;
import com.meidusa.venus.util.Range;
import com.meidusa.venus.util.UUID;
import com.meidusa.venus.util.VenusTracerUtil;
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
public class VenusInvoker {

    private static Logger logger = LoggerFactory.getLogger(VenusInvoker.class);

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
    public void invoke(ProviderInvocation invocation) throws RpcException{
        //获取调用信息
        Tuple<Long, byte[]> data = invocation.getData();
        byte[] message = invocation.getMessage();
        byte packetSerializeType = invocation.getPacketSerializeType();
        VenusFrontendConnection conn = invocation.getConn();
        long waitTime = invocation.getWaitTime();
        String finalSourceIp = invocation.getFinalSourceIp();
        VenusRouterPacket routerPacket = invocation.getRouterPacket();
        byte serializeType = invocation.getSerializeType();

        //解析报文并查找服务端点，TODO 校验有效性 校验服务是否存在
        SerializeServiceRequestPacket request = null;
        Endpoint ep = null;
        ServiceAPIPacket apiPacket = new ServiceAPIPacket();
        try {
            ServicePacketBuffer packetBuffer = new ServicePacketBuffer(message);
            apiPacket.init(packetBuffer);

            ep = getServiceManager().getEndpoint(apiPacket.apiName);

            Serializer serializer = SerializerFactory.getSerializer(packetSerializeType);
            request = new SerializeServiceRequestPacket(serializer, ep.getParameterTypeDict());

            packetBuffer.setPosition(0);
            request.init(packetBuffer);
            VenusTracerUtil.logReceive(request.traceId, request.apiName, JSON.toJSONString(request.parameterMap,JSON_FEATURE) );
        } catch (Exception e) {
            ErrorPacket error = new ErrorPacket();
            AbstractServicePacket.copyHead(apiPacket, error);
            if (e instanceof CodedException || codeMap.containsKey(e.getClass())) {
                if(e instanceof CodedException){
                    CodedException codeEx = (CodedException) e;
                    error.errorCode = codeEx.getErrorCode();
                }else{
                    error.errorCode = codeMap.get(e.getClass());
                }
            } else {
                if (e instanceof JSONException || e instanceof SerializeException) {
                    error.errorCode = VenusExceptionCodeConstant.REQUEST_ILLEGAL;
                } else {
                    error.errorCode = VenusExceptionCodeConstant.UNKNOW_EXCEPTION;
                }
            }
            error.message = e.getMessage();
            if(filter != null){
                filter.before(request);
            }
            postMessageBack(conn, routerPacket, request, error);
            if(filter != null){
                filter.after(error);
            }

            if(request != null){
                logPerformance(ep,request.traceId == null ? UUID.toString(PacketConstant.EMPTY_TRACE_ID) : UUID.toString(request.traceId),apiPacket.apiName,waitTime,0,conn.getHost(),finalSourceIp,request.clientId,request.clientRequestId,request.parameterMap, error);

                if (e instanceof VenusExceptionLevel) {
                    if (((VenusExceptionLevel) e).getLevel() != null) {
                        logDependsOnLevel(((VenusExceptionLevel) e).getLevel(), logger, e.getMessage() + " client:{clientID=" + apiPacket.clientId
                                + ",ip=" + conn.getHost() + ":" + conn.getPort() + ",sourceIP=" + finalSourceIp + ", apiName=" + apiPacket.apiName
                                + "}", e);
                    }
                } else {
                    logger.error(e.getMessage() + " [ip=" + conn.getHost() + ":" + conn.getPort() + ",sourceIP=" + finalSourceIp + ", apiName="+ apiPacket.apiName + "]", e);
                }
            }else{
                logger.error(e.getMessage() + " [ip=" + conn.getHost() + ":" + conn.getPort() + ",sourceIP=" + finalSourceIp + ", apiName="+ apiPacket.apiName + "]", e);
            }

            return;
        }

        final String apiName = request.apiName;
                /*int index = apiName.lastIndexOf(".");
                String serviceName = request.apiName.substring(0, index);
                String methodName = request.apiName.substring(index + 1);*/
        // RequestInfo info = getRequestInfo(conn, request);

        final Endpoint endpoint = ep;//getServiceManager().getEndpoint(serviceName, methodName, request.parameterMap.keySet().toArray(new String[] {}));

        EndpointInvocation.ResultType resultType = EndpointInvocation.ResultType.RESPONSE;
        RemotingInvocationListener<Serializable> invocationListener = null;
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

        for (Map.Entry<String, Object> entry : request.parameterMap.entrySet()) {
            if (entry.getValue() instanceof ReferenceInvocationListener) {
                invocationListener = new RemotingInvocationListener<Serializable>(conn, (ReferenceInvocationListener) entry.getValue(), request,
                        routerPacket);
                request.parameterMap.put(entry.getKey(), invocationListener);
            }
        }

        //TODO 校验

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
                return;
            }
        }

        RequestHandler requestHandler = new RequestHandler();

        RequestInfo requestInfo = requestHandler.getRequestInfo(packetSerializeType, conn, routerPacket);
        RequestContext context = requestHandler.createContext(requestInfo, endpoint, request);

        //TODO 服务调用
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
