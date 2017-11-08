package com.meidusa.venus.backend.handler;

import com.meidusa.fastjson.JSON;
import com.meidusa.fastmark.feature.SerializerFeature;
import com.meidusa.toolkit.common.bean.util.Initialisable;
import com.meidusa.toolkit.common.bean.util.InitialisationException;
import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.toolkit.net.MessageHandler;
import com.meidusa.toolkit.net.util.InetAddressUtil;
import com.meidusa.toolkit.util.TimeUtil;
import com.meidusa.venus.Result;
import com.meidusa.venus.ServerInvocation;
import com.meidusa.venus.backend.invoker.VenusServerInvocationListener;
import com.meidusa.venus.backend.invoker.VenusServerInvokerProxy;
import com.meidusa.venus.backend.invoker.support.ServerRequestHandler;
import com.meidusa.venus.backend.invoker.support.ServerResponseHandler;
import com.meidusa.venus.backend.invoker.support.ServerResponseWrapper;
import com.meidusa.venus.backend.services.*;
import com.meidusa.venus.backend.support.Response;
import com.meidusa.venus.exception.DefaultVenusException;
import com.meidusa.venus.exception.ExceptionLevel;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.exception.XmlVenusExceptionFactory;
import com.meidusa.venus.io.handler.VenusServerMessageHandler;
import com.meidusa.venus.io.network.VenusFrontendConnection;
import com.meidusa.venus.io.packet.*;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;
import com.meidusa.venus.io.serializer.Serializer;
import com.meidusa.venus.io.serializer.SerializerFactory;
import com.meidusa.venus.io.utils.RpcIdUtil;
import com.meidusa.venus.notify.InvocationListener;
import com.meidusa.venus.notify.ReferenceInvocationListener;
import com.meidusa.venus.support.VenusContext;
import com.meidusa.venus.util.*;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * venus服务端服务调用消息处理
 * @author structchen
 *
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class VenusServerInvokerMessageHandler extends VenusServerMessageHandler implements MessageHandler<VenusFrontendConnection, Tuple<Long, byte[]>>, Initialisable {

    private static Logger logger = LoggerFactory.getLogger(VenusServerInvokerMessageHandler.class);

    private static Logger performanceLogger = LoggerFactory.getLogger("venus.backend.performance");

    private static Logger performancePrintResultLogger = LoggerFactory.getLogger("venus.backend.print.result");

    private static Logger performancePrintParamsLogger = LoggerFactory.getLogger("venus.backend.print.params");

    /*
    private int threadLiveTime = 30;
    private boolean executorEnabled = false;
    private boolean executorProtected;
    private boolean useThreadLocalExecutor;
    private Executor executor;
    private EndpointInvocation.ResultType resultType;
    private Executor executor;
    private RequestContext context;
    private short serializeType;
    private String apiName;
    private String sourceIp;
    private Tuple<Long, byte[]> data;
    */

    private static Logger INVOKER_LOGGER = LoggerFactory.getLogger("venus.service.invoker");

    private static final String TIMEOUT = "waiting-timeout for execution,api=%s,ip=%s,time=%d (ms)";

    private static SerializerFeature[] JSON_FEATURE = new SerializerFeature[]{SerializerFeature.ShortString,SerializerFeature.IgnoreNonFieldGetter,SerializerFeature.SkipTransientField};

    private static String ENDPOINT_INVOKED_TIME = "invoked Total Time: ";

    private byte[] traceID;

    private Endpoint endpoint;

    private VenusFrontendConnection conn;

    private SerializeServiceRequestPacket request;

    private VenusRouterPacket routerPacket;

    private ServiceManager serviceManager;

    private ServerResponseHandler responseHandler = new ServerResponseHandler();

    /**
     * 服务调用代理
     */
    private VenusServerInvokerProxy venusServerInvokerProxy = new VenusServerInvokerProxy();

    private static boolean isEnableRandomPrint = true;

    @Override
    public void init() throws InitialisationException {
    }


   public void handle(VenusFrontendConnection conn, Tuple<Long, byte[]> data) {
        /*
        final long waitTime = TimeUtil.currentTimeMillis() - data.left;
        byte serializeType = conn.getSerializeType();
        String sourceIp = conn.getHost();
        */
        byte[] message = data.right;
        int type = AbstractServicePacket.getType(message);
        if (PacketConstant.PACKET_TYPE_ROUTER == type) {
            VenusRouterPacket routerPacket = new VenusRouterPacket();
            routerPacket.original = message;
            routerPacket.init(message);
            type = AbstractServicePacket.getType(routerPacket.data);
            /*
            message = routerPacket.data;
            serializeType = routerPacket.serializeType;
            sourceIp = InetAddressUtil.intToAddress(routerPacket.srcIP);
            */
        }

        switch (type) {
            case PacketConstant.PACKET_TYPE_PING:
                super.handle(conn, data);
                break;
            case PacketConstant.PACKET_TYPE_PONG:
                super.handle(conn, data);
                break;
            case PacketConstant.PACKET_TYPE_VENUS_STATUS_REQUEST:
                super.handle(conn, data);
                break;
            case PacketConstant.PACKET_TYPE_SERVICE_REQUEST:
                //处理服务调用消息
                doHandle(conn, data);
                break;
            default:
                super.handle(conn, data);
        }

    }

    /**
     * 处理远程调用请求
     * @param conn
     * @param data
     */
    void doHandle(VenusFrontendConnection conn, Tuple<Long, byte[]> data) {
        long bTime = System.currentTimeMillis();
        ServerInvocation invocation = null;
        Result result = null;
        String rpcId = null;
        try {
            //解析请求对象
            invocation = parseInvocation(conn, data);

            //不要打印bytes信息流，会导致后续无法获取
            rpcId = invocation.getRpcId();
            if(logger.isInfoEnabled()){
                logger.info("recv request,rpcId:{},message size:{}.", rpcId,data.getRight().length);
            }

            //通过代理调用服务
            result = getVenusServerInvokerProxy().invoke(invocation, null);
        } catch (Throwable t) {
            if(logger.isErrorEnabled()){
                logger.error("handle error.",t);
            }
            result = new Result();
            int errorCode = XmlVenusExceptionFactory.getInstance().getErrorCode(t.getClass());
            if(errorCode != 0){//自定义异常
                result.setErrorCode(errorCode);
                result.setErrorMessage(t.getMessage());
                result.setException(t);
            }else{//jdk内置异常
                DefaultVenusException ex = new DefaultVenusException(t.getMessage(),t);
                result.setErrorCode(ex.getErrorCode());
                result.setErrorMessage(ex.getMessage());
                result.setException(ex);
            }
        }

        // 输出响应
        // 1、将exception转化为errorPacket方式输出
        // 2、对于rpcException包装异常输出其原始异常，因client不升级可能不存在类定义
        try {
            ServerResponseWrapper responseEntityWrapper = ServerResponseWrapper.parse(invocation,result,false);

            if (invocation.getResultType() == EndpointInvocation.ResultType.RESPONSE) {
                if(logger.isInfoEnabled()){
                    logger.info("write normal response,rpcId:{},cost time:{},result:{}",rpcId,System.currentTimeMillis()-bTime, JSONUtil.toJSONString(result));
                }
                responseHandler.writeResponseForResponse(responseEntityWrapper);
            } else if (invocation.getResultType() == EndpointInvocation.ResultType.OK) {
                if(logger.isInfoEnabled()){
                    logger.info("write normal response,rpcId:{},cost time:{},result:{}",rpcId,System.currentTimeMillis()-bTime,JSONUtil.toJSONString(result));
                }
                responseHandler.writeResponseForOk(responseEntityWrapper);
            } else if (invocation.getResultType() == EndpointInvocation.ResultType.NOTIFY) {
                //callback回调异常情况
                if(result.getErrorCode() != 0){
                    if(logger.isInfoEnabled()){
                        logger.info("write notify response,rpcId:{},result:{}",rpcId,JSONUtil.toJSONString(result));
                    }
                    responseHandler.writeResponseForNotify(responseEntityWrapper);
                }
            }
        } catch (Throwable t) {
            if(logger.isErrorEnabled()){
                logger.error("write response error.",t);
            }
        }finally {
            if(isEnableRandomPrint){
                if(ThreadLocalRandom.current().nextInt(50000) > 49990){
                    if(logger.isInfoEnabled()){
                        logger.info("curent thread:{},instance:{},cost time:{}.",Thread.currentThread(),this,System.currentTimeMillis()-bTime);
                    }
                }
            }
        }
    }

    /**
     * 解析并构造请求对象
     * @return
     */
    ServerInvocation parseInvocation(VenusFrontendConnection conn, Tuple<Long, byte[]> data){
        ServerInvocation invocation = new ServerInvocation();
        invocation.setConn(conn);
        invocation.setData(data);
        invocation.setClientId(conn.getClientId());
        invocation.setHost(conn.getHost());
        invocation.setLocalHost(conn.getLocalHost());
        invocation.setRequestTime(new Date());
        String providerApp = VenusContext.getInstance().getApplication();
        String providerIp = NetUtil.getLocalIp();
        String consumerIp = conn.getHost();
        invocation.setProviderApp(providerApp);
        invocation.setProviderIp(providerIp);
        invocation.setConsumerIp(consumerIp);
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
        Endpoint endpointDef = parseEndpoint(conn, data);
        if(endpointDef == null){
            throw new RpcException("find endpoint def failed.");
        }
        invocation.setEndpointDef(endpointDef);
        invocation.setRpcId(RpcIdUtil.getRpcId(request));
        Service service = endpointDef.getService();
        invocation.setServiceInterface(service.getType());
        invocation.setMethod(endpointDef.getMethod());
        invocation.setResultType(getResultType(endpointDef));
        //设置参数
        initParamsForInvocationListener(request,invocation.getConn(),routerPacket,invocation);
        //获取上下文信息
        RequestContext requestContext = getRequestContext(invocation);
        if(requestContext != null){
            requestContext.setEndPointer(endpointDef);
        }
        invocation.setRequestContext(requestContext);
        //确认监控用，可注掉
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
        final long waitTime = TimeUtil.currentTimeMillis() - data.left;
        byte[] message = data.right;
        int type = AbstractServicePacket.getType(message);
        byte serializeType = conn.getSerializeType();
        String sourceIp = conn.getHost();
        VenusRouterPacket routerPacket = null;
        if (PacketConstant.PACKET_TYPE_ROUTER == type) {
            routerPacket = new VenusRouterPacket();
            routerPacket.original = message;
            routerPacket.init(message);
            type = AbstractServicePacket.getType(routerPacket.data);
            message = routerPacket.data;
            serializeType = routerPacket.serializeType;
            sourceIp = InetAddressUtil.intToAddress(routerPacket.srcIP);
        }

        SerializeServiceRequestPacket request = null;
        Endpoint ep = null;
        ServiceAPIPacket apiPacket = new ServiceAPIPacket();
        ServicePacketBuffer packetBuffer = new ServicePacketBuffer(message);
        apiPacket.init(packetBuffer);

        ep = getServiceManager().getEndpoint(apiPacket.apiName);

        Serializer serializer = SerializerFactory.getSerializer(serializeType);
        request = new SerializeServiceRequestPacket(serializer, ep.getParameterTypeDict());

        packetBuffer.setPosition(0);
        request.init(packetBuffer);
        VenusTracerUtil.logReceive(request.traceId, request.apiName, JSON.toJSONString(request.parameterMap,JSON_FEATURE) );

        return request;
    }

//    SerializeServiceRequestPacket parseRequest(VenusFrontendConnection conn, Tuple<Long, byte[]> data){
//        final long waitTime = TimeUtil.currentTimeMillis() - data.left;
//        byte[] message = data.right;
//        int type = AbstractServicePacket.getType(message);
//        byte serializeType = conn.getSerializeType();
//        String sourceIp = conn.getHost();
//        VenusRouterPacket routerPacket = null;
//        if (PacketConstant.PACKET_TYPE_ROUTER == type) {
//            routerPacket = new VenusRouterPacket();
//            routerPacket.original = message;
//            routerPacket.init(message);
//            type = AbstractServicePacket.getType(routerPacket.data);
//            message = routerPacket.data;
//            serializeType = routerPacket.serializeType;
//            sourceIp = InetAddressUtil.intToAddress(routerPacket.srcIP);
//        }
//
//        SerializeServiceRequestPacket request = null;
//        Endpoint ep = null;
//        ServiceAPIPacket apiPacket = new ServiceAPIPacket();
//        try {
//            ServicePacketBuffer packetBuffer = new ServicePacketBuffer(message);
//            apiPacket.init(packetBuffer);
//
//            ep = getServiceManager().getEndpoint(apiPacket.apiName);
//
//            Serializer serializer = SerializerFactory.getSerializer(serializeType);
//            request = new SerializeServiceRequestPacket(serializer, ep.getParameterTypeDict());
//
//            packetBuffer.setPosition(0);
//            request.init(packetBuffer);
//            VenusTracerUtil.logReceive(request.traceId, request.apiName, JSON.toJSONString(request.parameterMap,JSON_FEATURE) );
//
//            return request;
//        } catch (Exception e) {
//            ErrorPacket error = new ErrorPacket();
//            AbstractServicePacket.copyHead(apiPacket, error);
//            if (e instanceof CodedException || codeMap.containsKey(e.getClass())) {
//                if(e instanceof CodedException){
//                    CodedException codeEx = (CodedException) e;
//                    error.errorCode = codeEx.getErrorCode();
//                }else{
//                    error.errorCode = codeMap.get(e.getClass());
//                }
//            } else {
//                if (e instanceof JSONException || e instanceof SerializeException) {
//                    error.errorCode = VenusExceptionCodeConstant.REQUEST_ILLEGAL;
//                } else {
//                    error.errorCode = VenusExceptionCodeConstant.UNKNOW_EXCEPTION;
//                }
//            }
//            error.message = e.getMessage();
//
//            if(request != null){
//                logPerformance(ep,request.traceId == null ? UUID.toString(PacketConstant.EMPTY_TRACE_ID) : UUID.toString(request.traceId),apiPacket.apiName,waitTime,0,conn.getHost(),sourceIp,request.clientId,request.clientRequestId,request.parameterMap, error);
//
//                if (e instanceof VenusExceptionLevel) {
//                    if (((VenusExceptionLevel) e).getLevel() != null) {
//                        logDependsOnLevel(((VenusExceptionLevel) e).getLevel(), logger, e.getMessage() + " client:{clientID=" + apiPacket.clientId
//                                + ",ip=" + conn.getHost() + ":" + conn.getPort() + ",sourceIP=" + sourceIp + ", apiName=" + apiPacket.apiName
//                                + "}", e);
//                    }
//                } else {
//                    logger.error(e.getMessage() + " [ip=" + conn.getHost() + ":" + conn.getPort() + ",sourceIP=" + sourceIp + ", apiName="+ apiPacket.apiName + "]", e);
//                }
//            }else{
//                logger.error(e.getMessage() + " [ip=" + conn.getHost() + ":" + conn.getPort() + ",sourceIP=" + sourceIp + ", apiName="+ apiPacket.apiName + "]", e);
//            }
//
//            throw new ErrorPacketWrapperException(error);
//        }
//
//    }

    /**
     * 解析endpoint
     * @param conn
     * @param data
     * @return
     */
    Endpoint parseEndpoint(VenusFrontendConnection conn, Tuple<Long, byte[]> data){
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
        ServicePacketBuffer packetBuffer = new ServicePacketBuffer(message);
        apiPacket.init(packetBuffer);

        ep = getServiceManager().getEndpoint(apiPacket.apiName);

        return ep;
    }

//    Endpoint parseEndpoint(VenusFrontendConnection conn, Tuple<Long, byte[]> data){
//        final long waitTime = TimeUtil.currentTimeMillis() - data.left;
//        byte[] message = data.right;
//        int type = AbstractServicePacket.getType(message);
//        VenusRouterPacket routerPacket = null;
//        byte serializeType = conn.getSerializeType();
//        String sourceIp = conn.getHost();
//        if (PacketConstant.PACKET_TYPE_ROUTER == type) {
//            routerPacket = new VenusRouterPacket();
//            routerPacket.original = message;
//            routerPacket.init(message);
//            type = AbstractServicePacket.getType(routerPacket.data);
//            message = routerPacket.data;
//            serializeType = routerPacket.serializeType;
//            sourceIp = InetAddressUtil.intToAddress(routerPacket.srcIP);
//        }
//        final byte packetSerializeType = serializeType;
//        final String finalSourceIp = sourceIp;
//
//        SerializeServiceRequestPacket request = null;
//        Endpoint ep = null;
//        ServiceAPIPacket apiPacket = new ServiceAPIPacket();
//        try {
//            ServicePacketBuffer packetBuffer = new ServicePacketBuffer(message);
//            apiPacket.init(packetBuffer);
//
//            ep = getServiceManager().getEndpoint(apiPacket.apiName);
//
//            return ep;
//        } catch (Exception e) {
//            ErrorPacket error = new ErrorPacket();
//            AbstractServicePacket.copyHead(apiPacket, error);
//            if (e instanceof CodedException || codeMap.containsKey(e.getClass())) {
//                if(e instanceof CodedException){
//                    CodedException codeEx = (CodedException) e;
//                    error.errorCode = codeEx.getErrorCode();
//                }else{
//                    error.errorCode = codeMap.get(e.getClass());
//                }
//            } else {
//                if (e instanceof JSONException || e instanceof SerializeException) {
//                    error.errorCode = VenusExceptionCodeConstant.REQUEST_ILLEGAL;
//                } else {
//                    error.errorCode = VenusExceptionCodeConstant.UNKNOW_EXCEPTION;
//                }
//            }
//            error.message = e.getMessage();
//
//            if(request != null){
//                logPerformance(ep,request.traceId == null ? UUID.toString(PacketConstant.EMPTY_TRACE_ID) : UUID.toString(request.traceId),apiPacket.apiName,waitTime,0,conn.getHost(),finalSourceIp,request.clientId,request.clientRequestId,request.parameterMap, error);
//
//                if (e instanceof VenusExceptionLevel) {
//                    if (((VenusExceptionLevel) e).getLevel() != null) {
//                        logDependsOnLevel(((VenusExceptionLevel) e).getLevel(), logger, e.getMessage() + " client:{clientID=" + apiPacket.clientId
//                                + ",ip=" + conn.getHost() + ":" + conn.getPort() + ",sourceIP=" + finalSourceIp + ", apiName=" + apiPacket.apiName
//                                + "}", e);
//                    }
//                } else {
//                    logger.error(e.getMessage() + " [ip=" + conn.getHost() + ":" + conn.getPort() + ",sourceIP=" + finalSourceIp + ", apiName="+ apiPacket.apiName + "]", e);
//                }
//            }else{
//                logger.error(e.getMessage() + " [ip=" + conn.getHost() + ":" + conn.getPort() + ",sourceIP=" + finalSourceIp + ", apiName="+ apiPacket.apiName + "]", e);
//            }
//
//            throw new ErrorPacketWrapperException(error);
//        }
//
//    }

    /**
     * 设置invocationListener回调参数
     * @param request
     * @param conn
     * @param routerPacket
     */
    void initParamsForInvocationListener(SerializeServiceRequestPacket request, VenusFrontendConnection conn, VenusRouterPacket routerPacket,ServerInvocation invocation){
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
    RequestContext getRequestContext(ServerInvocation rpcInvocation){
        byte packetSerializeType = rpcInvocation.getPacketSerializeType();
        //构造请求上下文信息
        ServerRequestHandler requestHandler = new ServerRequestHandler();
        RequestInfo requestInfo = requestHandler.getRequestInfo(packetSerializeType, routerPacket, rpcInvocation);
        RequestContext requestContext = requestHandler.createContext(requestInfo, endpoint, request);
        return requestContext;
    }

    protected void logPerformance(Endpoint endpoint,String traceId,String apiName,long queuedTime,
                                  long executTime,String remoteIp,String sourceIP, long clientId,long requestId,
                                  Map<String,Object > parameterMap,Object result){
        StringBuilder buffer = new StringBuilder();
        buffer.append("[").append(queuedTime).append(",").append(executTime).append("]ms, (*server*) traceID=").append(traceId).append(", api=").append(apiName).append(", ip=")
                .append(remoteIp).append(", sourceIP=").append(sourceIP).append(", clientID=")
                .append(clientId).append(", requestID=").append(requestId);

        PerformanceLogger pLevel = null;

        if(endpoint != null){
            pLevel = endpoint.getPerformanceLogger();
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

    /**
     * 获取VenusServerInvokerProxy
     * @return
     */
    VenusServerInvokerProxy getVenusServerInvokerProxy(){
        return venusServerInvokerProxy;
    }


}
