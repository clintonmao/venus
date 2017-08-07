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
import com.meidusa.venus.backend.invoker.callback.RemotingInvocationListener;
import com.meidusa.venus.backend.invoker.support.*;
import com.meidusa.venus.backend.invoker.sync.VenusEndpointInvocation;
import com.meidusa.venus.backend.services.EndpointInvocation;
import com.meidusa.venus.backend.services.RequestInfo;
import com.meidusa.venus.backend.support.Response;
import com.meidusa.venus.backend.services.Endpoint;
import com.meidusa.venus.backend.services.Service;
import com.meidusa.venus.backend.services.ServiceManager;
import com.meidusa.venus.backend.services.xml.bean.PerformanceLogger;
import com.meidusa.venus.backend.services.RequestContext;
import com.meidusa.venus.backend.support.UtilTimerStack;
import com.meidusa.venus.exception.*;
import com.meidusa.venus.extension.athena.AthenaTransactionId;
import com.meidusa.venus.extension.athena.delegate.AthenaReporterDelegate;
import com.meidusa.venus.extension.athena.delegate.AthenaTransactionDelegate;
import com.meidusa.venus.io.ServiceFilter;
import com.meidusa.venus.io.network.VenusFrontendConnection;
import com.meidusa.venus.io.packet.*;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceResponsePacket;
import com.meidusa.venus.io.serializer.Serializer;
import com.meidusa.venus.io.serializer.SerializerFactory;
import com.meidusa.venus.io.support.VenusStatus;
import com.meidusa.venus.notify.InvocationListener;
import com.meidusa.venus.notify.ReferenceInvocationListener;
import com.meidusa.venus.rpc.RpcException;
import com.meidusa.venus.service.monitor.MonitorRuntime;
import com.meidusa.venus.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Method;
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

    private VenusFrontendConnection conn;

    private RequestContext context;

    private EndpointInvocation.ResultType resultType;

    private ServiceFilter filter;

    private byte[] traceID;

    private SerializeServiceRequestPacket request;

    private short serializeType;

    private VenusRouterPacket routerPacket;

    private RemotingInvocationListener<Serializable> invocationListener;

    private VenusExceptionFactory venusExceptionFactory;

    private Tuple<Long, byte[]> data;

    private String apiName;

    private String sourceIp;

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


    @Override
    public void run() {
        handle(null,null);
    }

    public void handle(final VenusFrontendConnection conn, final Tuple<Long, byte[]> data) {
        //解析并构造调用对象
        RpcInvocation invocation = buildInvocation(conn, data);
        if(invocation == null){
            return;
        }
        //调用请求处理
        doHandle(conn,invocation);
    }

    /**
     * 构造请求对象 TODO 统一接口invocation定义
     * @return
     */
    RpcInvocation buildInvocation(VenusFrontendConnection conn, Tuple<Long, byte[]> data){
        SerializeServiceRequestPacket requestPacket = parseRequest(conn, data);
        if(requestPacket == null){
            return null;
        }
        //TODO 构造invocation
        return null;
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

            return null;
        }

    }

    /**
     * invokeInvocation
     * @return
     */
    public void doHandle(VenusFrontendConnection conn, RpcInvocation invocation) throws RpcException{
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
        invoke(conn, endpoint,
                context, resultType,
                filter, routerPacket,
                request, serializeType,
                invocationListener, venusExceptionFactory,
                data);
        //end
    }


    public void invoke(VenusFrontendConnection conn, Endpoint endpoint,
                                 RequestContext context, EndpointInvocation.ResultType resultType, ServiceFilter filter,
                                 VenusRouterPacket routerPacket, SerializeServiceRequestPacket request,
                                 short serializeType, RemotingInvocationListener<Serializable> invocationListener,
                                 VenusExceptionFactory venusExceptionFactory, Tuple<Long, byte[]> data) {

        /*
        this.conn = conn;
        this.endpoint = endpoint;
        this.context = context;
        this.resultType = resultType;
        this.filter = filter;
        this.request = request;
        this.traceID = request.traceId;
        this.serializeType = serializeType;
        this.routerPacket = routerPacket;
        this.invocationListener = invocationListener;
        this.venusExceptionFactory = venusExceptionFactory;
        this.data = data;
        this.apiName = request.apiName;
        if (routerPacket != null) {
            this.sourceIp = InetAddressUtil.intToAddress(routerPacket.srcIP);
        }else {
            this.sourceIp = conn.getHost();
        }
        */

        boolean athenaFlag = endpoint.getService().getAthenaFlag();
        if (athenaFlag) {
            AthenaReporterDelegate.getDelegate().metric(apiName + ".doHandle");
            AthenaTransactionId transactionId = new AthenaTransactionId();
            transactionId.setRootId(context.getRootId());
            transactionId.setParentId(context.getParentId());
            transactionId.setMessageId(context.getMessageId());
            AthenaTransactionDelegate.getDelegate().startServerTransaction(transactionId, apiName);
            AthenaTransactionDelegate.getDelegate().setServerInputSize(data.right.length);
        }


        AbstractServicePacket resultPacket = null;
        ResponseHandler responseHandler = new ResponseHandler();
        long startRunTime = TimeUtil.currentTimeMillis();
        Response result = null;

        try {
            if (conn.isClosed() && resultType == EndpointInvocation.ResultType.RESPONSE) {
                return;
            }

            ThreadLocalMap.put(VenusTracerUtil.REQUEST_TRACE_ID, traceID);
            ThreadLocalMap.put(ThreadLocalConstant.REQUEST_CONTEXT, context);

            if (filter != null) {
                filter.before(request);
            }
            // doHandle service endpoint
            if (resultType == EndpointInvocation.ResultType.RESPONSE) {
                handleInvokeByResponse(context,endpoint,responseHandler,resultPacket,athenaFlag);
            } else if (resultType == EndpointInvocation.ResultType.OK) {
                handleInvokeByOK(context,endpoint,responseHandler,resultPacket,athenaFlag);
            } else if (resultType == EndpointInvocation.ResultType.NOTIFY) {
                handleInvokeByNotify(context,endpoint,responseHandler,resultPacket,athenaFlag);
            }

            if(athenaFlag) {
                AthenaReporterDelegate.getDelegate().metric(apiName + ".complete");
            }
        } catch (Exception e) {
            if (athenaFlag) {
                AthenaReporterDelegate.getDelegate().metric(apiName + ".error");
            }
            ErrorPacket error = new ErrorPacket();
            AbstractServicePacket.copyHead(request, error);
            Integer code = CodeMapScanner.getCodeMap().get(e.getClass());
            if (code != null) {
                error.errorCode = code;
            } else {
                if (e instanceof CodedException) {
                    CodedException codeEx = (CodedException) e;
                    error.errorCode = codeEx.getErrorCode();
                    if (logger.isDebugEnabled()) {
                        logger.debug("error when doHandle", e);
                    }
                } else {
                    try {
                        Method method = e.getClass().getMethod("getErrorCode");
                        int i = (Integer) method.invoke(e);
                        error.errorCode = i;
                        if (logger.isDebugEnabled()) {
                            logger.debug("error when doHandle", e);
                        }
                    } catch (Exception e1) {
                        error.errorCode = VenusExceptionCodeConstant.UNKNOW_EXCEPTION;
                        if (logger.isWarnEnabled()) {
                            logger.warn("error when doHandle", e);
                        }
                    }
                }
            }
            resultPacket = error;
            error.message = e.getMessage();
            responseHandler.postMessageBack(conn, routerPacket, request, error, athenaFlag);

            return;
        } catch (OutOfMemoryError e) {
            ErrorPacket error = new ErrorPacket();
            AbstractServicePacket.copyHead(request, error);
            error.errorCode = VenusExceptionCodeConstant.SERVICE_UNAVAILABLE_EXCEPTION;
            error.message = e.getMessage();
            resultPacket = error;
            responseHandler.postMessageBack(conn, routerPacket, request, error, athenaFlag);
            VenusStatus.getInstance().setStatus(PacketConstant.VENUS_STATUS_OUT_OF_MEMORY);
            logger.error("error when doHandle", e);
            throw e;
        } catch (Error e) {
            ErrorPacket error = new ErrorPacket();
            AbstractServicePacket.copyHead(request, error);
            error.errorCode = VenusExceptionCodeConstant.SERVICE_UNAVAILABLE_EXCEPTION;
            error.message = e.getMessage();
            resultPacket = error;
            responseHandler.postMessageBack(conn, routerPacket, request, error, athenaFlag);
            logger.error("error when doHandle", e);
            return;
        } finally {
            if (athenaFlag) {
                AthenaTransactionDelegate.getDelegate().completeServerTransaction();
            }
            long endRunTime = TimeUtil.currentTimeMillis();
            long queuedTime = startRunTime - data.left;
            long executeTime = endRunTime - startRunTime;
            if ((endpoint.getTimeWait() < (queuedTime + executeTime)) && athenaFlag) {
                AthenaReporterDelegate.getDelegate().metric(apiName + ".timeout");
            }
            MonitorRuntime.getInstance().calculateAverage(endpoint.getService().getName(), endpoint.getName(), executeTime, false);
            PerformanceHandler.logPerformance(endpoint, request, queuedTime, executeTime, conn.getHost(), sourceIp, result);
            if (filter != null) {
                filter.after(resultPacket);
            }
            ThreadLocalMap.remove(ThreadLocalConstant.REQUEST_CONTEXT);
            ThreadLocalMap.remove(VenusTracerUtil.REQUEST_TRACE_ID);
        }

    }

    /**
     * 处理response同步类型调用
     * @param context
     * @param endpoint
     */
    void handleInvokeByResponse(RequestContext context,Endpoint endpoint,ResponseHandler responseHandler,AbstractServicePacket resultPacket,boolean athenaFlag) throws Exception{
        Response result = invokeInvocation(context, endpoint);
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
    void handleInvokeByOK(RequestContext context,Endpoint endpoint,ResponseHandler responseHandler,AbstractServicePacket resultPacket,boolean athenaFlag) throws Exception{
        Response result = invokeInvocation(context, endpoint);
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
    void handleInvokeByNotify(RequestContext context,Endpoint endpoint,ResponseHandler responseHandler,AbstractServicePacket resultPacket,boolean athenaFlag) throws Exception{
        Response result = invokeInvocation(context, endpoint);
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

    /**
     * 执行调用
     * @param context
     * @param endpoint
     * @return
     */
    private Response invokeInvocation(RequestContext context, Endpoint endpoint) {
        Response response = new Response();
        VenusEndpointInvocation invocation = new VenusEndpointInvocation(context, endpoint);
        //invocation.addObserver(ObserverScanner.getInvocationObservers());
        try {
            UtilTimerStack.push(ENDPOINT_INVOKED_TIME);
            response.setResult(invocation.invoke());
        } catch (Throwable e) {
            AthenaReporterDelegate.getDelegate().problem(e.getMessage(), e);
            //VenusMonitorDelegate.getInstance().reportError(e.getMessage(), e);
            if (e instanceof ServiceInvokeException) {
                e = ((ServiceInvokeException) e).getTargetException();
            }
            if (e instanceof Exception) {
                response.setException((Exception) e);
            } else {
                response.setException(new DefaultVenusException(e.getMessage(), e));
            }

            Integer code = CodeMapScanner.getCodeMap().get(e.getClass());

            if (code != null) {
                response.setErrorCode(code);
                response.setErrorMessage(e.getMessage());
            } else {
                response.setError(e, venusExceptionFactory);
            }

            Service service = endpoint.getService();
            if (e instanceof VenusExceptionLevel) {
                if (((VenusExceptionLevel) e).getLevel() != null) {
                    LogHandler.logDependsOnLevel(((VenusExceptionLevel) e).getLevel(), INVOKER_LOGGER, e.getMessage() + " " + context.getRequestInfo().getRemoteIp() + " "
                            + service.getName() + ":" + endpoint.getMethod().getName() + " " + Utils.toString(context.getParameters()), e);
                }
            } else {
                if (e instanceof RuntimeException && !(e instanceof CodedException)) {
                    INVOKER_LOGGER.error(e.getMessage() + " " + context.getRequestInfo().getRemoteIp() + " " + service.getName() + ":" + endpoint.getMethod().getName()
                            + " " + Utils.toString(context.getParameters()), e);
                } else {
                    if (endpoint.isAsync()) {
                        if (INVOKER_LOGGER.isErrorEnabled()) {

                            INVOKER_LOGGER.error(e.getMessage() + " " + context.getRequestInfo().getRemoteIp() + " " + service.getName() + ":"
                                    + endpoint.getMethod().getName() + " " + Utils.toString(context.getParameters()), e);
                        }
                    } else {
                        if (INVOKER_LOGGER.isDebugEnabled()) {
                            INVOKER_LOGGER.debug(e.getMessage() + " " + context.getRequestInfo().getRemoteIp() + " " + service.getName() + ":"
                                    + endpoint.getMethod().getName() + " " + Utils.toString(context.getParameters()), e);
                        }
                    }
                }
            }
        } finally {
            UtilTimerStack.pop(ENDPOINT_INVOKED_TIME);
        }

        return response;
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
