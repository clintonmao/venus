package com.meidusa.venus.backend.invoker;

import com.meidusa.fastjson.JSON;
import com.meidusa.fastmark.feature.SerializerFeature;
import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.toolkit.util.TimeUtil;
import com.meidusa.venus.annotations.ExceptionCode;
import com.meidusa.venus.annotations.RemoteException;
import com.meidusa.venus.backend.ErrorPacketWrapperException;
import com.meidusa.venus.backend.filter.valid.ValidFilter;
import com.meidusa.venus.backend.invoker.support.*;
import com.meidusa.venus.backend.services.*;
import com.meidusa.venus.backend.services.xml.bean.PerformanceLogger;
import com.meidusa.venus.backend.support.Response;
import com.meidusa.venus.backend.support.UtilTimerStack;
import com.meidusa.venus.exception.*;
import com.meidusa.venus.extension.athena.AthenaTransactionId;
import com.meidusa.venus.extension.athena.delegate.AthenaReporterDelegate;
import com.meidusa.venus.extension.athena.delegate.AthenaTransactionDelegate;
import com.meidusa.venus.io.packet.AbstractServicePacket;
import com.meidusa.venus.io.packet.ErrorPacket;
import com.meidusa.venus.io.packet.PacketConstant;
import com.meidusa.venus.io.packet.VenusRouterPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;
import com.meidusa.venus.io.support.VenusStatus;
import com.meidusa.venus.rpc.*;
import com.meidusa.venus.service.monitor.MonitorRuntime;
import com.meidusa.venus.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * venus服务调用代理，除调用服务实现，还负责校验、认证、流控、降级、监控相关处理
 * Created by Zhangzhihua on 2017/8/2.
 */
public class VenusInvokerProxy implements Invoker{

    private static Logger logger = LoggerFactory.getLogger(VenusInvokerProxy.class);

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

    //private RemotingInvocationListener<Serializable> invocationListener;

    private VenusExceptionFactory venusExceptionFactory;

    //private VenusFrontendConnection conn;

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
    public void init() throws RpcException {

    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        RpcInvocation rpcInvocation = (RpcInvocation)invocation;
        //前置操作，校验、认证、流控、降级
        for(Filter filter : getFilters()){
            Result result = filter.invoke(rpcInvocation);
            if(result != null){
                return result;
            }
        }

        //处理调用请求 TODO 统一或适配result/response
        Response result = doInvoke(rpcInvocation);
        return null;
    }

    @Override
    public void destroy() throws RpcException {

    }

    /**
     * 获取拦截器列表
     * @return
     */
    Filter[] getFilters(){
        return new Filter[]{
                //校验
                new ValidFilter()
        };
    }

    /**
     * invoke
     * @return
     */
    public Response doInvoke(RpcInvocation invocation) throws RpcException{
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
        EndpointInvocation.ResultType resultType = invocation.getResultType();

        //构造请求上下文信息
        RequestHandler requestHandler = new RequestHandler();
        RequestInfo requestInfo = requestHandler.getRequestInfo(packetSerializeType, routerPacket, invocation);
        RequestContext requestContext = requestHandler.createContext(requestInfo, endpoint, request);

        //调用服务
        boolean athenaFlag = endpoint.getService().getAthenaFlag();
        if (athenaFlag) {
            AthenaReporterDelegate.getDelegate().metric(apiName + ".handleRequest");
            AthenaTransactionId transactionId = new AthenaTransactionId();
            transactionId.setRootId(requestContext.getRootId());
            transactionId.setParentId(requestContext.getParentId());
            transactionId.setMessageId(requestContext.getMessageId());
            AthenaTransactionDelegate.getDelegate().startServerTransaction(transactionId, apiName);
            AthenaTransactionDelegate.getDelegate().setServerInputSize(data.right.length);
        }


        AbstractServicePacket resultPacket = null;
        ResponseHandler responseHandler = new ResponseHandler();
        long startRunTime = TimeUtil.currentTimeMillis();
        Response result = null;

        try {
            if (invocation.getConn().isClosed() && resultType == EndpointInvocation.ResultType.RESPONSE) {
                throw new RpcException("conn is closed.");
            }

            ThreadLocalMap.put(VenusTracerUtil.REQUEST_TRACE_ID, traceID);
            ThreadLocalMap.put(ThreadLocalConstant.REQUEST_CONTEXT, requestContext);

            //无任何实现 delete by zhangzh 2017.8.8
            /*
            if (filte != null) {
                filte.before(request);
            }
            */

            //调用服务实例
            result = invokeEndpoint(requestContext,endpoint);

            if(athenaFlag) {
                AthenaReporterDelegate.getDelegate().metric(apiName + ".complete");
            }
            return result;
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
                        logger.debug("error when handleRequest", e);
                    }
                } else {
                    try {
                        Method method = e.getClass().getMethod("getErrorCode");
                        int i = (Integer) method.invoke(e);
                        error.errorCode = i;
                        if (logger.isDebugEnabled()) {
                            logger.debug("error when handleRequest", e);
                        }
                    } catch (Exception e1) {
                        error.errorCode = VenusExceptionCodeConstant.UNKNOW_EXCEPTION;
                        if (logger.isWarnEnabled()) {
                            logger.warn("error when handleRequest", e);
                        }
                    }
                }
            }
            resultPacket = error;
            error.message = e.getMessage();
            //responseHandler.postMessageBack(conn, routerPacket, request, error, athenaFlag);
            throw new ErrorPacketWrapperException(error);
        } catch (OutOfMemoryError e) {
            ErrorPacket error = new ErrorPacket();
            AbstractServicePacket.copyHead(request, error);
            error.errorCode = VenusExceptionCodeConstant.SERVICE_UNAVAILABLE_EXCEPTION;
            error.message = e.getMessage();
            resultPacket = error;
            //responseHandler.postMessageBack(conn, routerPacket, request, error, athenaFlag);
            VenusStatus.getInstance().setStatus(PacketConstant.VENUS_STATUS_OUT_OF_MEMORY);
            logger.error("error when handleRequest", e);
            throw new ErrorPacketWrapperException(error);
        } catch (Error e) {
            ErrorPacket error = new ErrorPacket();
            AbstractServicePacket.copyHead(request, error);
            error.errorCode = VenusExceptionCodeConstant.SERVICE_UNAVAILABLE_EXCEPTION;
            error.message = e.getMessage();
            resultPacket = error;
            //responseHandler.postMessageBack(conn, routerPacket, request, error, athenaFlag);
            logger.error("error when handleRequest", e);
            throw new ErrorPacketWrapperException(error);
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
            PerformanceHandler.logPerformance(endpoint, request, queuedTime, executeTime, invocation.getHost(), sourceIp, result);
            //无任何实现 delete by zhangzh 2017.8.8
            /*
            if (filte != null) {
                filte.after(resultPacket);
            }
            */
            ThreadLocalMap.remove(ThreadLocalConstant.REQUEST_CONTEXT);
            ThreadLocalMap.remove(VenusTracerUtil.REQUEST_TRACE_ID);
        }
    }

    /**
     * 执行调用
     * @param context
     * @param endpoint
     * @return
     */
    private Response invokeEndpoint(RequestContext context, Endpoint endpoint) {
        Response response = new Response();
        VenusInvoker invocation = new VenusInvoker(context, endpoint);
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
