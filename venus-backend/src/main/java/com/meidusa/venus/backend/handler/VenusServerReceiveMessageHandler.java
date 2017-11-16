package com.meidusa.venus.backend.handler;

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
import com.meidusa.venus.backend.support.ServerRequestHandler;
import com.meidusa.venus.backend.support.ServerResponseHandler;
import com.meidusa.venus.backend.support.ServerResponseWrapper;
import com.meidusa.venus.backend.services.*;
import com.meidusa.venus.exception.*;
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

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * venus服务端服务调用消息处理
 * @author structchen
 *
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class VenusServerReceiveMessageHandler extends VenusServerMessageHandler implements MessageHandler<VenusFrontendConnection, Tuple<Long, byte[]>>, Initialisable {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    private static Logger tracerLogger = VenusLoggerFactory.getTracerLogger();

    private static SerializerFeature[] JSON_FEATURE = new SerializerFeature[]{SerializerFeature.ShortString,SerializerFeature.IgnoreNonFieldGetter,SerializerFeature.SkipTransientField};

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

    @Override
    public void init() throws InitialisationException {
    }


   public void handle(VenusFrontendConnection conn, Tuple<Long, byte[]> data) {
        byte[] message = data.right;
        int type = AbstractServicePacket.getType(message);
        if (PacketConstant.PACKET_TYPE_ROUTER == type) {
            if(logger.isInfoEnabled()){
                logger.info("recv router packet type...");
            }
            VenusRouterPacket routerPacket = new VenusRouterPacket();
            routerPacket.original = message;
            routerPacket.init(message);
            type = AbstractServicePacket.getType(routerPacket.data);
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
            if(tracerLogger.isInfoEnabled()){
                tracerLogger.info("recv request,rpcId:{},sourceIp:{},message size:{}.", rpcId,conn.getHost(),data.getRight().length);
            }

            //通过代理调用服务
            result = getVenusServerInvokerProxy().invoke(invocation, null);
        } catch (Throwable t) {
            result = buildResult(t);
        }finally {
            try {
                //输出tracer日志
                printTracerLogger(invocation,result,bTime);
            } catch (Exception e) {}
        }

        // 输出响应，将exception转化为errorPacket方式输出
        try {
            ServerResponseWrapper responseWrapper = ServerResponseWrapper.parse(invocation,result,false);

            if (invocation.getResultType() == EndpointInvocation.ResultType.RESPONSE) {
                if(tracerLogger.isInfoEnabled()){
                    tracerLogger.info("send normal response,rpcId:{},used time:{}ms.",rpcId,System.currentTimeMillis()-bTime);
                }
                responseHandler.writeResponseForResponse(responseWrapper);
            } else if (invocation.getResultType() == EndpointInvocation.ResultType.OK) {
                if(tracerLogger.isInfoEnabled()){
                    tracerLogger.info("send normal response,rpcId:{},used time:{}ms.",rpcId,System.currentTimeMillis()-bTime);
                }
                responseHandler.writeResponseForOk(responseWrapper);
            } else if (invocation.getResultType() == EndpointInvocation.ResultType.NOTIFY) {
                //callback回调异常情况
                if(result.getErrorCode() != 0){
                    if(tracerLogger.isInfoEnabled()){
                        tracerLogger.info("send notify response,rpcId:{},used time:{}ms.",rpcId,System.currentTimeMillis()-bTime);
                    }
                    responseHandler.writeResponseForNotify(responseWrapper);
                }
            }
        } catch (Throwable t) {
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error("send response error.",t);
            }
        }
    }


    /**
     * 输出tracer日志
     * @param invocation
     * @param result
     * @param bTime
     */
    void printTracerLogger(ServerInvocation invocation,Result result,long bTime){
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
            if(result.getErrorCode() == 0 && result.getException() == null){
                output = JSONUtil.toJSONString(result.getResult());
            }else if(result.getException() != null){
                hasException = true;
                output = result.getException();
            }else if(result.getErrorCode() != 0){
                output = String.format("%s-%s",result.getErrorCode(),result.getErrorMessage());
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
        String tpl = "{} handle,rpcId:{},method:{},status:{},used time:{}ms,param:{},result:{}.";
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
            if(args != null && args.length > 0){
                for(Object arg:args){
                    if(arg instanceof ReferenceInvocationListener){
                       invocation.setInvocationListener((ReferenceInvocationListener)arg);
                    }
                }
            }
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

        return request;
    }


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

    /**
     * 将异常转化为result
     * @param t
     * @return
     */
    Result buildResult(Throwable t){
        Result result = new Result();
        //将rpcException包装异常转化为v3内置异常
        Throwable ex = restoreException(t);

        int errorCode = XmlVenusExceptionFactory.getInstance().getErrorCode(ex.getClass());
        if(errorCode != 0){//自定义异常
            result.setErrorCode(errorCode);
            result.setErrorMessage(ex.getMessage());
            result.setException(ex);
        }else{//内置异常
            //包装为venus默认异常，以便到client能还原异常信息
            DefaultVenusException dex = new DefaultVenusException(VenusExceptionCodeConstant.UNKNOW_EXCEPTION,ex.getMessage(),ex);
            result.setErrorCode(dex.getErrorCode());
            result.setErrorMessage(dex.getMessage());
            result.setException(dex);
        }
        return result;
    }

    /**
     * 将rpcExcpetion包装异常转换为V3版本已内置异常，为不兼容升级
     * @param t
     * @return
     */
    Throwable restoreException(Throwable t){
        if(t instanceof RpcException){
            RpcException rex = (RpcException)t;
            if(rex.getCause() != null){
                return rex.getCause();
            }else{
                DefaultVenusException dex = new DefaultVenusException(rex.getErrorCode(),rex.getMessage());
                return dex;
            }
        }else{
            return t;
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
