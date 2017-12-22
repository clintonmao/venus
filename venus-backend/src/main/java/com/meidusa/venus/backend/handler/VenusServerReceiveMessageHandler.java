package com.meidusa.venus.backend.handler;

import com.meidusa.toolkit.common.bean.util.Initialisable;
import com.meidusa.toolkit.common.bean.util.InitialisationException;
import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.toolkit.net.MessageHandler;
import com.meidusa.toolkit.net.util.InetAddressUtil;
import com.meidusa.venus.Result;
import com.meidusa.venus.ServerInvocation;
import com.meidusa.venus.backend.context.RequestContext;
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
import com.meidusa.venus.support.VenusUtil;
import com.meidusa.venus.util.*;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
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
        //解析请求报文
       ServerInvocation invocation = parseInvocation(conn, data);
       int type = invocation.getMessageType();

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
               doHandle(invocation);
                break;
            default:
                super.handle(conn, data);
        }

    }

    /**
     * 处理远程调用请求
     */
    void doHandle(ServerInvocation invocation) {
        long bTime = System.currentTimeMillis();

        Result result = null;
        try {
            //解析API请求信息
            parseApiRequest(invocation);

            //打印接收日志，无效请求日志输出到default
            boolean isIgnoreLog = false;
            if(StringUtils.isNotEmpty(invocation.getApiName()) && invocation.getApiName().contains("ServiceRegistry")){
                isIgnoreLog = true;
            }
            String tpl = "[P] recv request,rpcId:{},api:{},sourceIp:{},routeIp:{},message size:{}.";
            Object[] arguments = new Object[]{
                    invocation.getRpcId(),
                    invocation.getApiName(),
                    invocation.getSourceIp(),
                    invocation.getRouteIp(),
                    invocation.getData().getRight().length
            };
            if(!isIgnoreLog){
                if(tracerLogger.isInfoEnabled()){
                    tracerLogger.info(tpl,arguments);
                }
            }else{
                if(logger.isInfoEnabled()){
                    logger.info(tpl,arguments);
                }
            }

            //解析端点定义及服务报文
            parseEndpointAndRequest(invocation);

            //解析参数相关信息
            parseParamsAndListener(invocation);

            //通过代理调用服务
            result = getVenusServerInvokerProxy().invoke(invocation, null);
        } catch (Throwable t) {
            result = buildExceptionResult(t);
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
                responseHandler.writeResponseForResponse(responseWrapper);
            } else if (invocation.getResultType() == EndpointInvocation.ResultType.OK) {
                responseHandler.writeResponseForOk(responseWrapper);
            } else if (invocation.getResultType() == EndpointInvocation.ResultType.NOTIFY) {
                //callback回调异常情况
                if(result.getErrorCode() != 0){
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
     * 构造请求对象
     * @param conn
     * @param data
     * @return
     */
    ServerInvocation parseInvocation(VenusFrontendConnection conn, Tuple<Long, byte[]> data){
        byte[] message = data.right;
        int type = AbstractServicePacket.getType(message);
        byte serializeType = conn.getSerializeType();
        String sourceIp = conn.getHost();
        String routeIp = "";
        VenusRouterPacket routerPacket = null;
        if (PacketConstant.PACKET_TYPE_ROUTER == type) {
            routerPacket = new VenusRouterPacket();
            routerPacket.original = message;
            routerPacket.init(message);
            message = routerPacket.data;
            type = AbstractServicePacket.getType(routerPacket.data);
            serializeType = routerPacket.serializeType;
            routeIp = sourceIp;
            sourceIp = InetAddressUtil.intToAddress(routerPacket.srcIP);
        }

        ServerInvocation invocation = new ServerInvocation();
        //基本报文信息
        invocation.setMessage(message);
        invocation.setMessageType(type);
        invocation.setRouterPacket(routerPacket);
        invocation.setSerializeType(serializeType);
        invocation.setSourceIp(sourceIp);
        invocation.setRouteIp(routeIp);
        //其它信息
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
        //设置默认响应类型
        invocation.setResultType(EndpointInvocation.ResultType.RESPONSE);

        return invocation;
    }

    /**
     * 解析API请求
     * @param invocation
     */
    void parseApiRequest(ServerInvocation invocation){
        ServiceAPIPacket apiPacket = new ServiceAPIPacket();
        ServicePacketBuffer packetBuffer = new ServicePacketBuffer(invocation.getMessage());
        apiPacket.init(packetBuffer);
        invocation.setApiPacket(apiPacket);
        invocation.setApiName(apiPacket.apiName);
        invocation.setPacketBuffer(packetBuffer);
        String rpcId = RpcIdUtil.getRpcId(apiPacket);
        invocation.setRpcId(rpcId);
    }

    /**
     * 解析请求消息
     * @return
     */
    void parseEndpointAndRequest(ServerInvocation invocation){
        byte serializeType = invocation.getSerializeType();
        ServiceAPIPacket apiPacket = invocation.getApiPacket();
        ServicePacketBuffer packetBuffer = invocation.getPacketBuffer();

        //获取endpoint定义
        Endpoint endpoint = getServiceManager().getEndpoint(apiPacket.apiName);
        invocation.setEndpointDef(endpoint);
        Service service = endpoint.getService();
        invocation.setServiceInterface(service.getType());
        invocation.setMethod(endpoint.getMethod());
        invocation.setResultType(getResultType(endpoint));

        //解析serviceRequest
        Serializer serializer = SerializerFactory.getSerializer(serializeType);
        SerializeServiceRequestPacket serviceRequestPacket = new SerializeServiceRequestPacket(serializer, endpoint.getParameterTypeDict());
        packetBuffer.setPosition(0);
        serviceRequestPacket.init(packetBuffer);
        invocation.setServiceRequestPacket(serviceRequestPacket);
    }

    /**
     * 解析并构造请求对象
     * @return
     */
    void parseParamsAndListener(ServerInvocation invocation){
        Endpoint endpoint = invocation.getEndpointDef();
        VenusRouterPacket routerPacket = invocation.getRouterPacket();
        SerializeServiceRequestPacket serviceRequestPacket = invocation.getServiceRequestPacket();
        if(MapUtils.isNotEmpty(serviceRequestPacket.parameterMap)){
            Object[] args = serviceRequestPacket.parameterMap.values().toArray();
            invocation.setArgs(args);
            if(args != null && args.length > 0){
                for(Object arg:args){
                    if(arg instanceof ReferenceInvocationListener){
                        invocation.setInvocationListener((ReferenceInvocationListener)arg);
                    }
                }
            }
        }
        //设置参数
        initParamsForInvocationListener(serviceRequestPacket,invocation.getConn(),routerPacket,invocation);
        //获取上下文信息
        RequestContext requestContext = getRequestContext(invocation);
        if(requestContext != null){
            requestContext.setEndPointer(endpoint);
        }
        invocation.setRequestContext(requestContext);

        //获取athena监控上下文信息
        //ThreadLocalMap.put(VenusTracerUtil.REQUEST_TRACE_ID, traceID);
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
     * @return
     */
    RequestContext getRequestContext(ServerInvocation invocation){
        byte packetSerializeType = invocation.getPacketSerializeType();
        Endpoint endpoint = invocation.getEndpointDef();
        VenusRouterPacket routerPacket = invocation.getRouterPacket();
        SerializeServiceRequestPacket serviceRequestPacket = invocation.getServiceRequestPacket();

        //构造请求上下文信息
        ServerRequestHandler requestHandler = new ServerRequestHandler();
        RequestInfo requestInfo = requestHandler.getRequestInfo(packetSerializeType, routerPacket, invocation);
        RequestContext requestContext = requestHandler.createContext(requestInfo, endpoint, serviceRequestPacket);
        return requestContext;
    }

    /**
     * 将异常转化为result
     * @param t
     * @return
     */
    Result buildExceptionResult(Throwable t){
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

    /**
     * 输出tracer日志
     * @param invocation
     * @param result
     * @param bTime
     */
    void printTracerLogger(ServerInvocation invocation,Result result,long bTime){
        //构造参数
        boolean hasException = false;
        boolean isIgnoreException = false;
        long usedTime = System.currentTimeMillis() - bTime;
        String rpcId = invocation.getRpcId();
        String apiName = invocation.getApiName();
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
            if(result.getErrorCode() == 0 && result.getException() == null && result.getResult() != null){
                ret = JSONUtil.toJSONString(result.getResult());
            }
        }
        //异常
        Object error = "{}";
        if(invocation.isEnablePrintResult() && !VenusUtil.isAthenaInterface(invocation)){
            if(result.getException() != null){
                hasException = true;
                error = result.getException();
                //过滤venus.ServiceRegistry服务不存在异常
                if(error != null && error instanceof ServiceNotFoundException){
                    ServiceNotFoundException serviceNotFoundException = (ServiceNotFoundException)error;
                    String errorMsg = serviceNotFoundException.getMessage();
                    if(StringUtils.isNotEmpty(errorMsg)){
                        if(errorMsg.contains("ServiceRegistry")){
                            isIgnoreException = true;
                            error = "invalid request,venus not provide serviceRegistry serivce from V4.";
                        }
                    }
                }
            }else if(result.getErrorCode() != 0){
                hasException = true;
                error = String.format("%s-%s",result.getErrorCode(),result.getErrorMessage());
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
        if(hasException){
            String tpl = "[P] [{},{}],provider handle,rpcId:{},api:{},method:{},param:{},error:{}.";
            Object[] arguments = new Object[]{
                    status,
                    usedTime + "ms",
                    rpcId,
                    apiName,
                    methodPath,
                    param,
                    error
            };
            if(!isIgnoreException){
                if(tracerLogger.isErrorEnabled()){
                    tracerLogger.error(tpl,arguments);
                }
                //异常日志
                if(exceptionLogger.isErrorEnabled()){
                    exceptionLogger.error(tpl,arguments);
                }
            }else{
                if(logger.isWarnEnabled()){
                    logger.warn(tpl,arguments);
                }
            }
        }else{
            String tpl = "[P] [{},{}],provider handle,rpcId:{},api:{},method:{},param:{},result:{}.";
            Object[] arguments = new Object[]{
                    status,
                    usedTime + "ms",
                    rpcId,
                    apiName,
                    methodPath,
                    param,
                    ret
            };
            if(usedTime > 200){
                if(tracerLogger.isWarnEnabled()){
                    tracerLogger.warn(tpl,arguments);
                }
            }else{
                if(tracerLogger.isInfoEnabled()){
                    tracerLogger.info(tpl,arguments);
                }
            }
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
