package com.meidusa.venus.backend.support;

/**
 * Created by godzillahua on 7/4/16.
 */

import com.meidusa.toolkit.net.Connection;
import com.meidusa.venus.Result;
import com.meidusa.venus.backend.services.EndpointItem;
import com.meidusa.venus.exception.CodedException;
import com.meidusa.venus.exception.DefaultVenusException;
import com.meidusa.venus.exception.VenusExceptionCodeConstant;
import com.meidusa.venus.io.network.VenusFrontendConnection;
import com.meidusa.venus.io.packet.*;
import com.meidusa.venus.io.packet.serialize.SerializeServiceNofityPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceResponsePacket;
import com.meidusa.venus.io.serializer.Serializer;
import com.meidusa.venus.io.serializer.SerializerFactory;
import com.meidusa.venus.notify.ReferenceInvocationListener;
import com.meidusa.venus.util.ThreadLocalMap;
import com.meidusa.venus.util.Utils;
import com.meidusa.venus.util.VenusLoggerFactory;
import com.meidusa.venus.util.VenusTracerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Map;

/**
 * 服务端响应处理类
 */
public class ServerResponseHandler {

    private static Logger logger = LoggerFactory.getLogger(ServerResponseHandler.class);

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    /**
     * 处理response同步类型调用
     * @param wrapper
     * @throws Exception
     */
    public void writeResponseForResponse(ServerResponseWrapper wrapper) throws Exception{
        VenusFrontendConnection conn = wrapper.getConn();
        VenusRouterPacket routerPacket = wrapper.getRouterPacket();
        ServiceAPIPacket apiPacket = wrapper.getApiPacket();
        SerializeServiceRequestPacket request = wrapper.getRequest();
        EndpointItem endpoint = wrapper.getEndpoint();
        Result result = wrapper.getResult();
        short serializeType = wrapper.getSerializeType();
        Serializer serializer = SerializerFactory.getSerializer(serializeType);

        if (result.getErrorCode() == 0) {
            ServiceResponsePacket response = new SerializeServiceResponsePacket(serializer, endpoint.getMethod()
                    .getGenericReturnType());
            if(request != null){
                AbstractServicePacket.copyHead(request, response);
            }else if(apiPacket != null){
                AbstractServicePacket.copyHead(apiPacket, response);
            }
            response.result = result.getResult();

            postMessageBack(conn, routerPacket, response);
        }else{
            ErrorPacket error = toErrorPacket(result,request,apiPacket,serializer);
            postMessageBack(conn, routerPacket, error);
        }
    }

    /**
     * 处理OK类型调用
     * @param wrapper
     * @throws Exception
     */
    public void writeResponseForOk(ServerResponseWrapper wrapper) throws Exception{
        VenusFrontendConnection conn = wrapper.getConn();
        VenusRouterPacket routerPacket = wrapper.getRouterPacket();
        ServiceAPIPacket apiPacket = wrapper.getApiPacket();
        SerializeServiceRequestPacket request = wrapper.getRequest();
        Result result = wrapper.getResult();
        short serializeType = wrapper.getSerializeType();
        Serializer serializer = SerializerFactory.getSerializer(serializeType);

        if (result.getErrorCode() == 0) {
            OKPacket ok = new OKPacket();
            if(request != null){
                AbstractServicePacket.copyHead(request, ok);
            }else if(apiPacket != null){
                AbstractServicePacket.copyHead(apiPacket, ok);
            }
            postMessageBack(conn, routerPacket, ok);
        }else{
            ErrorPacket error = toErrorPacket(result,request,apiPacket,serializer);
            postMessageBack(conn, routerPacket, error);
        }
    }

    /**
     * 将exception转换为errorPacket
     * @param result
     * @param request
     * @param serializer
     * @return
     * @throws Exception
     */
    ErrorPacket toErrorPacket(Result result, AbstractServicePacket request, ServiceAPIPacket apiPacket,Serializer serializer) throws Exception{
        ErrorPacket error = new ErrorPacket();
        if(request != null){
            AbstractServicePacket.copyHead(request, error);
        }else if(apiPacket != null){
            AbstractServicePacket.copyHead(apiPacket, error);
        }
        error.errorCode = result.getErrorCode();
        error.message = result.getErrorMessage();
        Throwable ex = result.getException();
        if (ex != null) {
            Map<String, PropertyDescriptor> mpd = Utils.getBeanPropertyDescriptor(ex.getClass());
            Map<String, Object> additionalData = new HashMap<String, Object>();

            for (Map.Entry<String, PropertyDescriptor> entry : mpd.entrySet()) {
                additionalData.put(entry.getKey(), entry.getValue().getReadMethod().invoke(ex));
            }
            error.additionalData = serializer.encode(additionalData);
        }
        return error;
    }

    /**
     * 处理listener调用
     * @param wrapper
     */
    public void writeResponseForNotify(ServerResponseWrapper wrapper) {
        VenusFrontendConnection conn = wrapper.getConn();
        VenusRouterPacket routerPacket = wrapper.getRouterPacket();
        ServiceAPIPacket apiPacket = wrapper.getApiPacket();
        SerializeServiceRequestPacket request = wrapper.getRequest();
        Result result = wrapper.getResult();
        Serializer serializer = SerializerFactory.getSerializer(conn.getSerializeType());
        ReferenceInvocationListener referenceInvocationListener = (ReferenceInvocationListener) wrapper.getInvocationListener();

        if (result.getErrorCode() == 0) {
            ServiceNofityPacket response = new SerializeServiceNofityPacket(serializer, null);
            if(request != null){
                AbstractServicePacket.copyHead(request, response);
            }else if(apiPacket != null){
                AbstractServicePacket.copyHead(apiPacket, response);
            }
            response.callbackObject = result.getResult();
            response.apiName = request.apiName;
            response.identityData = referenceInvocationListener.getIdentityData();

            byte[] traceID = (byte[]) ThreadLocalMap.get(VenusTracerUtil.REQUEST_TRACE_ID);
            if (traceID == null) {
                traceID = VenusTracerUtil.randomUUID();
                ThreadLocalMap.put(VenusTracerUtil.REQUEST_TRACE_ID, traceID);
            }
            response.traceId = traceID;
            postMessageBack(conn, routerPacket, response);
        }else{
            ServiceNofityPacket response = toNotifyPacket(result,request,apiPacket,referenceInvocationListener,serializer);
            postMessageBack(conn, routerPacket, response);
        }
    }

    /**
     * 将exception转化为notifyPacket
     * @param result
     * @param request
     * @param serializer
     * @return
     */
    ServiceNofityPacket toNotifyPacket(Result result, AbstractServicePacket request, ServiceAPIPacket apiPacket,ReferenceInvocationListener referenceInvocationListener, Serializer serializer) {
        Throwable e = null;
        if (result.getException() == null) {
            e = new DefaultVenusException(result.getErrorCode(), result.getErrorMessage());
        } else {
            e = result.getException();
        }

        ServiceNofityPacket response = new SerializeServiceNofityPacket(serializer, null);
        if(request != null){
            AbstractServicePacket.copyHead(request, response);
        }else if(apiPacket != null){
            AbstractServicePacket.copyHead(apiPacket, response);
        }
        if (e instanceof CodedException) {
            CodedException codedException = (CodedException) e;
            response.errorCode = codedException.getErrorCode();
        } else {
            response.errorCode = VenusExceptionCodeConstant.UNKNOW_EXCEPTION;
        }

        if (e != null) {
            Map<String, PropertyDescriptor> mpd = Utils.getBeanPropertyDescriptor(e.getClass());
            Map<String, Object> additionalData = new HashMap<String, Object>();

            for (Map.Entry<String, PropertyDescriptor> entry : mpd.entrySet()) {
                try {
                    additionalData.put(entry.getKey(), entry.getValue().getReadMethod().invoke(e));
                } catch (Exception ex) {
                    if(logger.isErrorEnabled()){
                        exceptionLogger.error("read config properpty error", ex);
                    }
                }
            }
            response.additionalData = serializer.encode(additionalData);
            response.errorMessage = e.getMessage();
        }

        response.apiName = ((SerializeServiceRequestPacket)request).apiName;
        response.identityData = referenceInvocationListener.getIdentityData();
        byte[] traceID = VenusTracerUtil.getTracerID();
        if (traceID == null) {
            traceID = VenusTracerUtil.randomTracerID();
        }
        response.traceId = traceID;

        return response;
    }

    /**
     * 响应消息
     * @param conn
     * @param routerPacket
     * @param response
     */
    void postMessageBack(Connection conn, VenusRouterPacket routerPacket, AbstractServicePacket response) {
        if (routerPacket == null) {
            conn.write(response.toByteBuffer());
        } else {
            routerPacket.data = response.toByteArray();
            conn.write(routerPacket.toByteBuffer());
        }
    }

}
