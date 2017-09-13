package com.meidusa.venus.backend.invoker;

import com.athena.service.api.AthenaDataService;
import com.meidusa.fastbson.exception.SerializeException;
import com.meidusa.fastjson.JSON;
import com.meidusa.fastjson.JSONException;
import com.meidusa.fastmark.feature.SerializerFeature;
import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.toolkit.net.util.InetAddressUtil;
import com.meidusa.toolkit.util.TimeUtil;
import com.meidusa.venus.annotations.ExceptionCode;
import com.meidusa.venus.annotations.RemoteException;
import com.meidusa.venus.backend.ErrorPacketWrapperException;
import com.meidusa.venus.backend.invoker.support.RequestHandler;
import com.meidusa.venus.backend.invoker.support.ResponseEntityWrapper;
import com.meidusa.venus.backend.invoker.support.ResponseHandler;
import com.meidusa.venus.RpcInvocation;
import com.meidusa.venus.backend.services.*;
import com.meidusa.venus.backend.services.xml.bean.PerformanceLogger;
import com.meidusa.venus.backend.support.Response;
import com.meidusa.venus.client.AthenaContext;
import com.meidusa.venus.exception.*;
import com.meidusa.venus.io.network.VenusFrontendConnection;
import com.meidusa.venus.io.packet.*;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;
import com.meidusa.venus.io.serializer.Serializer;
import com.meidusa.venus.io.serializer.SerializerFactory;
import com.meidusa.venus.notify.InvocationListener;
import com.meidusa.venus.Result;
import com.meidusa.venus.notify.ReferenceInvocationListener;
import com.meidusa.venus.util.*;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * venus服务调用task，多线程执行
 * Created by Zhangzhihua on 2017/8/2.
 */
public class VenusServerInvokerTask implements Runnable{

    private static Logger logger = LoggerFactory.getLogger(VenusServerInvokerTask.class);

    private static Logger performanceLogger = LoggerFactory.getLogger("venus.backend.performance");

    private static Logger performancePrintResultLogger = LoggerFactory.getLogger("venus.backend.print.result");

    private static Logger performancePrintParamsLogger = LoggerFactory.getLogger("venus.backend.print.params");

    private static Logger INVOKER_LOGGER = LoggerFactory.getLogger("venus.service.invoker");

    private static final String TIMEOUT = "waiting-timeout for execution,api=%s,ip=%s,time=%d (ms)";

    private static SerializerFeature[] JSON_FEATURE = new SerializerFeature[]{SerializerFeature.ShortString,SerializerFeature.IgnoreNonFieldGetter,SerializerFeature.SkipTransientField};

    private ServiceManager serviceManager;

    private Executor executor;

    static Map<Class<?>,Integer> codeMap = new HashMap<Class<?>,Integer>();

    private static String ENDPOINT_INVOKED_TIME = "invoked Total Time: ";

    private Endpoint endpoint;

    private RequestContext context;

    private EndpointInvocation.ResultType resultType;

    private byte[] traceID;

    private SerializeServiceRequestPacket request;

    private short serializeType;

    private VenusRouterPacket routerPacket;

    //private VenusServerInvocationListener<Serializable> invocationListener;

    private VenusExceptionFactory venusExceptionFactory;

    private VenusFrontendConnection conn;

    private Tuple<Long, byte[]> data;

    private String apiName;

    private String sourceIp;

    private ResponseHandler responseHandler = new ResponseHandler();

    private static AthenaDataService athenaDataService;

    /**
     * 服务调用代理
     */
    private static VenusServerInvokerProxy venusServerInvokerProxy;

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

    public VenusServerInvokerTask(VenusFrontendConnection conn, Tuple<Long, byte[]> data){
        this.conn = conn;
        this.data = data;
    }

    @Override
    public void run() {
        handle(this.conn,this.data);
    }

    /**
     * 处理远程调用请求
     * @param conn
     * @param data
     */
    public void handle(VenusFrontendConnection conn, Tuple<Long, byte[]> data) {
        RpcInvocation invocation = null;
        Result result = null;
        try {
            //解析请求对象
            invocation = parseInvocation(conn, data);

            //通过代理调用服务
            VenusServerInvokerProxy venusServerInvokerProxy = getVenusServerInvokerProxy();
            result = venusServerInvokerProxy.invoke(invocation, null);
        } catch (Exception e) {
            //TODO 异常信息包装
            result = new Result();
            result.setErrorCode(500);
            result.setErrorMessage(e.getMessage());
        }

        //输出响应
        try {
            ResponseEntityWrapper responseEntityWrapper = ResponseEntityWrapper.parse(invocation,result,false);

            if (invocation.getResultType() == EndpointInvocation.ResultType.RESPONSE) {
                responseHandler.writeResponseForResponse(responseEntityWrapper);
            } else if (invocation.getResultType() == EndpointInvocation.ResultType.OK) {
                responseHandler.writeResponseForOk(responseEntityWrapper);
            } else if (invocation.getResultType() == EndpointInvocation.ResultType.NOTIFY) {
                //callback回调异常情况
                if(result.getErrorCode() != 0){
                    responseHandler.writeResponseForNotify(responseEntityWrapper);
                }
            }
        } catch (Exception e) {
            logger.error("write response error.",e);
        }
    }

    /**
     * 获取VenusServerInvokerProxy
     * @return
     */
    VenusServerInvokerProxy getVenusServerInvokerProxy(){
        if(venusServerInvokerProxy == null){
            venusServerInvokerProxy = new VenusServerInvokerProxy();
            venusServerInvokerProxy.setAthenaDataService(getAthenaDataService());
        }
        return venusServerInvokerProxy;
    }

    /**
     * 初始化athenaDataService
     * @return
     */
    AthenaDataService getAthenaDataService(){
        if(athenaDataService != null){
            return athenaDataService;
        }
        athenaDataService = AthenaContext.getInstance().getAthenaDataService();
        return athenaDataService;
    }


    /**
     * 解析并构造请求对象 TODO 统一接口invocation定义
     * @return
     */
    RpcInvocation parseInvocation(VenusFrontendConnection conn, Tuple<Long, byte[]> data){
        RpcInvocation invocation = new RpcInvocation();
        invocation.setConn(conn);
        invocation.setData(data);
        invocation.setClientId(conn.getClientId());
        invocation.setHost(conn.getHost());
        invocation.setLocalHost(conn.getLocalHost());
        invocation.setRequestTime(new Date());
        //设置请求报文
        SerializeServiceRequestPacket request = parseRequest(conn, data);
        invocation.setRequest(request);
        if(MapUtils.isNotEmpty(request.parameterMap)){
            Object[] args = request.parameterMap.values().toArray();
            invocation.setArgs(args);
        }
        this.request = request;
        this.routerPacket = invocation.getRouterPacket();
        //设置端点定义
        Endpoint endpointEx = parseEndpoint(conn, data);
        invocation.setEndpointEx(endpointEx);
        //TODO 补全接口服务相关信息
        if(endpointEx != null){
            Service service = endpointEx.getService();
            if(service != null){
                invocation.setServiceInterface(service.getType());
            }
            invocation.setMethod(endpointEx.getMethod());
        }
        invocation.setResultType(getResultType(endpointEx));
        //设置参数
        initParamsForInvocationListener(request,invocation.getConn(),routerPacket,invocation);
        //获取上下文信息
        RequestContext requestContext = getRequestContext(invocation);
        if(requestContext != null){
            requestContext.setEndPointer(endpointEx);
        }
        invocation.setRequestContext(requestContext);
        ThreadLocalMap.put(VenusTracerUtil.REQUEST_TRACE_ID, traceID);
        ThreadLocalMap.put(ThreadLocalConstant.REQUEST_CONTEXT, requestContext);
        if(requestContext.getRootId() != null){
            invocation.setAthenaId(requestContext.getRootId().getBytes());
        }
        if(requestContext.getParentId() != null){
            invocation.setParentId(requestContext.getParentId().getBytes());
        }
        if(requestContext.getMessageId() != null){
            invocation.setMessageId(requestContext.getMessageId().getBytes());
        }
        return invocation;
    }



    /**
     * 解析请求消息
     * @param conn
     * @param data
     * @return
     */
    SerializeServiceRequestPacket parseRequest(VenusFrontendConnection conn, Tuple<Long, byte[]> data){
        //TODO 代码复用
        final long waitTime = TimeUtil.currentTimeMillis() - data.left;
        byte[] message = data.right;
        int type = AbstractServicePacket.getType(message);
        VenusRouterPacket routerPacket = null;
        byte serializeType = conn.getSerializeType();
        String sourceIp = conn.getHost();
        if (PacketConstant.PACKET_TYPE_ROUTER == type) {
            routerPacket = new VenusRouterPacket();
            routerPacket.original = message;
            routerPacket.init(message);
            type = AbstractServicePacket.getType(routerPacket.data);
            message = routerPacket.data;
            serializeType = routerPacket.serializeType;
            sourceIp = InetAddressUtil.intToAddress(routerPacket.srcIP);
        }
        final byte packetSerializeType = serializeType;
        final String finalSourceIp = sourceIp;

        SerializeServiceRequestPacket request = null;
        Endpoint ep = null;
        ServiceAPIPacket apiPacket = new ServiceAPIPacket();
        try {
            ServicePacketBuffer packetBuffer = new ServicePacketBuffer(message);
            apiPacket.init(packetBuffer);

            ep = getServiceManager().getEndpoint(apiPacket.apiName);

            Serializer serializer = SerializerFactory.getSerializer(packetSerializeType);
            request = new SerializeServiceRequestPacket(serializer, ep.getParameterTypeDict());
            logger.info("recv request,messageId:{} reqeust:{}.",request.messageId,request);

            packetBuffer.setPosition(0);
            request.init(packetBuffer);
            VenusTracerUtil.logReceive(request.traceId, request.apiName, JSON.toJSONString(request.parameterMap,JSON_FEATURE) );

            return request;
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

            throw new ErrorPacketWrapperException(error);
        }

    }

    /**
     * 解析endpoint
     * @param conn
     * @param data
     * @return
     */
    Endpoint parseEndpoint(VenusFrontendConnection conn, Tuple<Long, byte[]> data){
        //TODO 代码复用
        final long waitTime = TimeUtil.currentTimeMillis() - data.left;
        byte[] message = data.right;
        int type = AbstractServicePacket.getType(message);
        VenusRouterPacket routerPacket = null;
        byte serializeType = conn.getSerializeType();
        String sourceIp = conn.getHost();
        if (PacketConstant.PACKET_TYPE_ROUTER == type) {
            routerPacket = new VenusRouterPacket();
            routerPacket.original = message;
            routerPacket.init(message);
            type = AbstractServicePacket.getType(routerPacket.data);
            message = routerPacket.data;
            serializeType = routerPacket.serializeType;
            sourceIp = InetAddressUtil.intToAddress(routerPacket.srcIP);
        }
        final byte packetSerializeType = serializeType;
        final String finalSourceIp = sourceIp;

        SerializeServiceRequestPacket request = null;
        Endpoint ep = null;
        ServiceAPIPacket apiPacket = new ServiceAPIPacket();
        try {
            ServicePacketBuffer packetBuffer = new ServicePacketBuffer(message);
            apiPacket.init(packetBuffer);

            ep = getServiceManager().getEndpoint(apiPacket.apiName);

            return ep;
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

            throw new ErrorPacketWrapperException(error);
        }

    }

    /**
     * 设置invocationListener回调参数
     * @param request
     * @param conn
     * @param routerPacket
     */
    void initParamsForInvocationListener(SerializeServiceRequestPacket request, VenusFrontendConnection conn, VenusRouterPacket routerPacket,RpcInvocation invocation){
        for (Map.Entry<String, Object> entry : request.parameterMap.entrySet()) {
            if (entry.getValue() instanceof ReferenceInvocationListener) {
                VenusServerInvocationListener<Serializable> invocationListener = new VenusServerInvocationListener<Serializable>(conn, (ReferenceInvocationListener) entry.getValue(), request,
                        routerPacket,invocation);
                invocationListener.setResponseHandler(responseHandler);
                request.parameterMap.put(entry.getKey(), invocationListener);
            }
        }
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
     * 获取上下文信息
     * @param rpcInvocation
     * @return
     */
    RequestContext getRequestContext(RpcInvocation rpcInvocation){
        byte packetSerializeType = rpcInvocation.getPacketSerializeType();
        //构造请求上下文信息
        RequestHandler requestHandler = new RequestHandler();
        RequestInfo requestInfo = requestHandler.getRequestInfo(packetSerializeType, routerPacket, rpcInvocation);
        RequestContext requestContext = requestHandler.createContext(requestInfo, endpoint, request);
        return requestContext;
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
