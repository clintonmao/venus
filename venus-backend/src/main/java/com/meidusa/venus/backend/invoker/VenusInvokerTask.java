package com.meidusa.venus.backend.invoker;

import com.meidusa.fastbson.exception.SerializeException;
import com.meidusa.fastjson.JSON;
import com.meidusa.fastjson.JSONException;
import com.meidusa.fastmark.feature.SerializerFeature;
import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.toolkit.net.Connection;
import com.meidusa.toolkit.net.util.InetAddressUtil;
import com.meidusa.toolkit.util.TimeUtil;
import com.meidusa.venus.annotations.ExceptionCode;
import com.meidusa.venus.annotations.RemoteException;
import com.meidusa.venus.backend.ErrorPacketWrapperException;
import com.meidusa.venus.backend.invoker.callback.RemotingInvocationListener;
import com.meidusa.venus.backend.invoker.support.*;
import com.meidusa.venus.backend.services.EndpointInvocation;
import com.meidusa.venus.backend.support.Response;
import com.meidusa.venus.backend.services.Endpoint;
import com.meidusa.venus.backend.services.ServiceManager;
import com.meidusa.venus.backend.services.xml.bean.PerformanceLogger;
import com.meidusa.venus.backend.services.RequestContext;
import com.meidusa.venus.exception.*;
import com.meidusa.venus.io.network.VenusFrontendConnection;
import com.meidusa.venus.io.packet.*;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceResponsePacket;
import com.meidusa.venus.io.serializer.Serializer;
import com.meidusa.venus.io.serializer.SerializerFactory;
import com.meidusa.venus.notify.InvocationListener;
import com.meidusa.venus.rpc.Result;
import com.meidusa.venus.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * venus协议提供方服务调用
 * Created by Zhangzhihua on 2017/8/2.
 */
public class VenusInvokerTask implements Runnable{

    private static Logger logger = LoggerFactory.getLogger(VenusInvokerTask.class);

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

    private RemotingInvocationListener<Serializable> invocationListener;

    private VenusExceptionFactory venusExceptionFactory;

    private VenusFrontendConnection conn;

    private Tuple<Long, byte[]> data;

    private String apiName;

    private String sourceIp;

    /**
     * 服务调用代理
     */
    private VenusInvokerProxy venusInvokerProxy;

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

    public VenusInvokerTask(VenusFrontendConnection conn, Tuple<Long, byte[]> data){
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
        try {
            //解析请求对象
            RpcInvocation invocation = buildInvocation(conn, data);

            //调用服务
            Result result = venusInvokerProxy.invoke(invocation);

            //输出响应
            handleResponse(context,null,null,null,false,null);
        } catch (Exception e) {
            //TODO 处理异常
            //handleResponse(context,null,null,null,false,null);
        }
    }


    /**
     * 构造请求对象 TODO 统一接口invocation定义
     * @return
     */
    RpcInvocation buildInvocation(VenusFrontendConnection conn, Tuple<Long, byte[]> data){
        RpcInvocation invocation = new RpcInvocation();
        SerializeServiceRequestPacket requestPacket = parseRequest(conn, data);
        invocation.setServiceRequestPacket(requestPacket);
        invocation.setLocalHost(conn.getLocalHost());
        invocation.setHost(conn.getHost());
        invocation.setClientId(conn.getClientId());
        //TODO get endpoint
        invocation.setResultType(null);
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
            //无任何实现 delete by zhangzh 2017.8.8
            /*
            if(filter != null){
                filter.before(request);
            }
            if(filter != null){
                filter.after(error);
            }
            */

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
     * 处理响应结果
     * @param context
     * @param endpoint
     * @param responseHandler
     * @param resultPacket
     * @param athenaFlag
     * @throws Exception
     */
    void handleResponse(RequestContext context, Endpoint endpoint, ResponseHandler responseHandler, AbstractServicePacket resultPacket, boolean athenaFlag,Response result) throws Exception{
        if (resultType == EndpointInvocation.ResultType.RESPONSE) {
            handleResponseByResponse(context,endpoint,responseHandler,resultPacket,athenaFlag, result);
        } else if (resultType == EndpointInvocation.ResultType.OK) {
            handleResponseByOK(context,endpoint,responseHandler,resultPacket,athenaFlag,result);
        } else if (resultType == EndpointInvocation.ResultType.NOTIFY) {
            handleResponseByNotify(context,endpoint,responseHandler,resultPacket,athenaFlag,result);
        }
    }

    /**
     * 处理response同步类型调用
     * @param context
     * @param endpoint
     * @param result
     */
    void handleResponseByResponse(RequestContext context, Endpoint endpoint, ResponseHandler responseHandler, AbstractServicePacket resultPacket, boolean athenaFlag, Response result) throws Exception{
        if (result.getErrorCode() == 0) {
            Serializer serializer = SerializerFactory.getSerializer(serializeType);
            ServiceResponsePacket response = new SerializeServiceResponsePacket(serializer, endpoint.getMethod()
                    .getGenericReturnType());
            AbstractServicePacket.copyHead(request, response);
            response.result = result.getResult();
            resultPacket = response;
            responseHandler.postMessageBack(conn, routerPacket, request, response, athenaFlag);
        }else{
            ErrorPacket error = new ErrorPacket();
            AbstractServicePacket.copyHead(request, error);
            error.errorCode = result.getErrorCode();
            error.message = result.getErrorMessage();
            Throwable throwable = result.getException();
            if (throwable != null) {
                Serializer serializer = SerializerFactory.getSerializer(serializeType);
                Map<String, PropertyDescriptor> mpd = Utils.getBeanPropertyDescriptor(throwable.getClass());
                Map<String, Object> additionalData = new HashMap<String, Object>();

                for (Map.Entry<String, PropertyDescriptor> entry : mpd.entrySet()) {
                    additionalData.put(entry.getKey(), entry.getValue().getReadMethod().invoke(throwable));
                }
                error.additionalData = serializer.encode(additionalData);
            }
            resultPacket = error;
            responseHandler.postMessageBack(conn, routerPacket, request, error, athenaFlag);
        }
    }

    /**
     * 处理OK类型调用
     * @param context
     * @param endpoint
     */
    void handleResponseByOK(RequestContext context, Endpoint endpoint, ResponseHandler responseHandler, AbstractServicePacket resultPacket, boolean athenaFlag,Response result) throws Exception{
        if (result.getErrorCode() == 0) {
            OKPacket ok = new OKPacket();
            AbstractServicePacket.copyHead(request, ok);
            resultPacket = ok;
            responseHandler.postMessageBack(conn, routerPacket, request, ok, athenaFlag);
        }else{
            ErrorPacket error = new ErrorPacket();
            AbstractServicePacket.copyHead(request, error);
            error.errorCode = result.getErrorCode();
            error.message = result.getErrorMessage();
            Throwable throwable = result.getException();
            if (throwable != null) {
                Serializer serializer = SerializerFactory.getSerializer(serializeType);
                Map<String, PropertyDescriptor> mpd = Utils.getBeanPropertyDescriptor(throwable.getClass());
                Map<String, Object> additionalData = new HashMap<String, Object>();

                for (Map.Entry<String, PropertyDescriptor> entry : mpd.entrySet()) {
                    additionalData.put(entry.getKey(), entry.getValue().getReadMethod().invoke(throwable));
                }
                error.additionalData = serializer.encode(additionalData);
            }
            resultPacket = error;
            responseHandler.postMessageBack(conn, routerPacket, request, error, athenaFlag);
        }
    }

    /**
     * 处理listener调用
     * @param context
     * @param endpoint
     * @param responseHandler
     * @param resultPacket
     * @param athenaFlag
     * @throws Exception
     */
    void handleResponseByNotify(RequestContext context, Endpoint endpoint, ResponseHandler responseHandler, AbstractServicePacket resultPacket, boolean athenaFlag,Response result) throws Exception{
        if (result.getErrorCode() == 0) {
            if (invocationListener != null && !invocationListener.isResponsed()) {
                invocationListener.onException(new ServiceNotCallbackException("Server side not call back error"));
            }
        }else{
            if (invocationListener != null && !invocationListener.isResponsed()) {
                if (result.getException() == null) {
                    invocationListener.onException(new DefaultVenusException(result.getErrorCode(), result.getErrorMessage()));
                } else {
                    invocationListener.onException(result.getException());
                }
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
