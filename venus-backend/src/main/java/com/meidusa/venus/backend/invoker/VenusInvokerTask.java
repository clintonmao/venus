package com.meidusa.venus.backend.invoker;

import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.toolkit.net.util.InetAddressUtil;
import com.meidusa.toolkit.util.TimeUtil;
import com.meidusa.venus.backend.invoker.callback.RemotingInvocationListener;
import com.meidusa.venus.backend.invoker.support.CodeMapScanner;
import com.meidusa.venus.backend.invoker.support.LogHandler;
import com.meidusa.venus.backend.invoker.support.PerformanceHandler;
import com.meidusa.venus.backend.invoker.support.ResponseHandler;
import com.meidusa.venus.backend.invoker.sync.VenusEndpointInvocation;
import com.meidusa.venus.backend.services.EndpointInvocation;
import com.meidusa.venus.backend.support.Response;
import com.meidusa.venus.io.support.VenusStatus;
import com.meidusa.venus.backend.services.RequestContext;
import com.meidusa.venus.backend.support.UtilTimerStack;
import com.meidusa.venus.backend.services.Endpoint;
import com.meidusa.venus.backend.services.Service;
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
import com.meidusa.venus.service.monitor.MonitorRuntime;
import com.meidusa.venus.util.ThreadLocalConstant;
import com.meidusa.venus.util.ThreadLocalMap;
import com.meidusa.venus.util.Utils;
import com.meidusa.venus.util.VenusTracerUtil;
import com.meidusa.venus.util.concurrent.MultiQueueRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * venus服务调用任务
 * Created by huawei on 5/15/16.
 */
public class VenusInvokerTask extends MultiQueueRunnable {

    private static Logger logger = LoggerFactory.getLogger(VenusInvokerTask.class);

    private static Logger INVOKER_LOGGER = LoggerFactory.getLogger("venus.service.invoker");

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

    //TODO 包装参数相关信息，裁剪参数
    public VenusInvokerTask(VenusFrontendConnection conn, Endpoint endpoint,
                            RequestContext context, EndpointInvocation.ResultType resultType, ServiceFilter filter,
                            VenusRouterPacket routerPacket, SerializeServiceRequestPacket request,
                            short serializeType, RemotingInvocationListener<Serializable> invocationListener,
                            VenusExceptionFactory venusExceptionFactory, Tuple<Long, byte[]> data) {
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
    }

    @Override
    public void doRun() {
        boolean athenaFlag = endpoint.getService().getAthenaFlag();
        if (athenaFlag) {
            AthenaReporterDelegate.getDelegate().metric(apiName + ".invoke");
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
            // invoke service endpoint
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
                        logger.debug("error when invoke", e);
                    }
                } else {
                    try {
                        Method method = e.getClass().getMethod("getErrorCode");
                        int i = (Integer) method.invoke(e);
                        error.errorCode = i;
                        if (logger.isDebugEnabled()) {
                            logger.debug("error when invoke", e);
                        }
                    } catch (Exception e1) {
                        error.errorCode = VenusExceptionCodeConstant.UNKNOW_EXCEPTION;
                        if (logger.isWarnEnabled()) {
                            logger.warn("error when invoke", e);
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
            logger.error("error when invoke", e);
            throw e;
        } catch (Error e) {
            ErrorPacket error = new ErrorPacket();
            AbstractServicePacket.copyHead(request, error);
            error.errorCode = VenusExceptionCodeConstant.SERVICE_UNAVAILABLE_EXCEPTION;
            error.message = e.getMessage();
            resultPacket = error;
            responseHandler.postMessageBack(conn, routerPacket, request, error, athenaFlag);
            logger.error("error when invoke", e);
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
        Response result = doInvoke(context, endpoint);
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
        Response result = doInvoke(context, endpoint);
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
        Response result = doInvoke(context, endpoint);
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
    private Response doInvoke(RequestContext context, Endpoint endpoint) {
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

    @Override
    public String getName() {
        return apiName;
    }


}
