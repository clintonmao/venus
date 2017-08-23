package com.meidusa.venus.backend.invoker;

import com.meidusa.venus.Result;
import com.meidusa.venus.backend.invoker.support.ResponseEntityWrapper;
import com.meidusa.venus.backend.invoker.support.ResponseHandler;
import com.meidusa.venus.backend.invoker.support.RpcInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.meidusa.venus.io.network.VenusFrontendConnection;
import com.meidusa.venus.io.packet.VenusRouterPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;
import com.meidusa.venus.notify.InvocationListener;
import com.meidusa.venus.notify.ReferenceInvocationListener;

/**
 * callback远程调用处理
 * @param <T>
 */
public class VenusServerInvocationListener<T> implements InvocationListener<T> {

    private static Logger logger = LoggerFactory.getLogger(VenusServerInvocationListener.class);

    private VenusFrontendConnection conn;

    private VenusRouterPacket routerPacket;

    private ReferenceInvocationListener<T> source;

    private SerializeServiceRequestPacket request;

    private RpcInvocation invocation;

    private boolean isResponsed = false;

    private ResponseHandler responseHandler;

    public boolean isResponsed() {
        return isResponsed;
    }

    public VenusServerInvocationListener(VenusFrontendConnection conn, ReferenceInvocationListener<T> source, SerializeServiceRequestPacket request,
                                         VenusRouterPacket routerPacket, RpcInvocation invocation) {
        this.conn = conn;
        this.source = source;
        this.request = request;
        this.routerPacket = routerPacket;
        this.invocation = invocation;
    }

    @Override
    public void callback(T object) {
        ResponseEntityWrapper responseEntityWrapper = ResponseEntityWrapper.parse(invocation,new Result(object),false);
        try {
            responseHandler.writeResponseForNotify(responseEntityWrapper);
        } catch (Exception e) {
            logger.error("response callback error.",e);
        }
    }

    @Override
    public void onException(Exception e) {
        //TODO 细化异常
        Result result = new Result();
        result.setErrorCode(500);
        result.setErrorMessage(e.getMessage());
        result.setException(e);
        ResponseEntityWrapper responseEntityWrapper = ResponseEntityWrapper.parse(invocation,result,false);
        try {
            responseHandler.writeResponseForNotify(responseEntityWrapper);
        } catch (Exception ex) {
            logger.error("response onException error.",ex);
        }
    }

    //    @Override
//    public void callback(T object) {
//        Serializer serializer = SerializerFactory.getSerializer(conn.getSerializeType());
//        ServiceNofityPacket response = new SerializeServiceNofityPacket(serializer, null);
//        AbstractServicePacket.copyHead(request, response);
//        response.callbackObject = object;
//        response.apiName = request.apiName;
//        response.identityData = source.getIdentityData();
//
//        byte[] traceID = (byte[]) ThreadLocalMap.get(VenusTracerUtil.REQUEST_TRACE_ID);
//
//        if (traceID == null) {
//            traceID = VenusTracerUtil.randomUUID();
//            ThreadLocalMap.put(VenusTracerUtil.REQUEST_TRACE_ID, traceID);
//        }
//
//        response.traceId = traceID;
//
//        if (routerPacket != null) {
//            routerPacket.data = response.toByteArray();
//            conn.write(routerPacket.toByteBuffer());
//        } else {
//            conn.write(response.toByteBuffer());
//        }
//        isResponsed = true;
//    }
//
//    @Override
//    public void onException(Exception e) {
//        Serializer serializer = SerializerFactory.getSerializer(conn.getSerializeType());
//        ServiceNofityPacket response = new SerializeServiceNofityPacket(serializer, null);
//        AbstractServicePacket.copyHead(request, response);
//        if (e instanceof CodedException) {
//            CodedException codedException = (CodedException) e;
//            response.errorCode = codedException.getErrorCode();
//        } else {
//            response.errorCode = VenusExceptionCodeConstant.UNKNOW_EXCEPTION;
//        }
//
//        if (e != null) {
//            Map<String, PropertyDescriptor> mpd = Utils.getBeanPropertyDescriptor(e.getClass());
//            Map<String, Object> additionalData = new HashMap<String, Object>();
//
//            for (Map.Entry<String, PropertyDescriptor> entry : mpd.entrySet()) {
//                try {
//                    additionalData.put(entry.getKey(), entry.getValue().getReadMethod().invoke(e));
//                } catch (Exception e1) {
//                    logger.error("read config properpty error", e1);
//                }
//            }
//            response.additionalData = serializer.encode(additionalData);
//            response.errorMessage = e.getMessage();
//        }
//
//        response.identityData = source.getIdentityData();
//
//        response.apiName = request.apiName;
//
//        byte[] traceID = VenusTracerUtil.getTracerID();
//
//        if (traceID == null) {
//            traceID = VenusTracerUtil.randomTracerID();
//        }
//
//        response.traceId = traceID;
//
//        if (routerPacket != null) {
//            routerPacket.data = response.toByteArray();
//            conn.write(routerPacket.toByteBuffer());
//        } else {
//            conn.write(response.toByteBuffer());
//        }
//
//        isResponsed = true;
//    }

    public ResponseHandler getResponseHandler() {
        return responseHandler;
    }

    public void setResponseHandler(ResponseHandler responseHandler) {
        this.responseHandler = responseHandler;
    }


}
