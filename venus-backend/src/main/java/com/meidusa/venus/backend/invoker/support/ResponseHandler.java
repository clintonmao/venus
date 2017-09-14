package com.meidusa.venus.backend.invoker.support;

/**
 * Created by godzillahua on 7/4/16.
 */
import com.meidusa.toolkit.net.Connection;
import com.meidusa.venus.Result;
import com.meidusa.venus.VenusThreadContext;
import com.meidusa.venus.backend.services.Endpoint;
import com.meidusa.venus.exception.CodedException;
import com.meidusa.venus.exception.DefaultVenusException;
import com.meidusa.venus.exception.VenusExceptionCodeConstant;
import com.meidusa.venus.monitor.athena.reporter.AthenaTransactionDelegate;
import com.meidusa.venus.io.network.VenusFrontendConnection;
import com.meidusa.venus.io.packet.*;
import com.meidusa.venus.io.packet.serialize.SerializeServiceNofityPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceResponsePacket;
import com.meidusa.venus.io.serializer.Serializer;
import com.meidusa.venus.io.serializer.SerializerFactory;
import com.meidusa.venus.util.ThreadLocalMap;
import com.meidusa.venus.util.Utils;
import com.meidusa.venus.util.VenusTracerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * 服务端响应处理类
 */
public class ResponseHandler {

    private static Logger logger = LoggerFactory.getLogger(ResponseHandler.class);

    /**
     * 处理response同步类型调用
     * @param wrapper
     * @throws Exception
     */
    public void writeResponseForResponse(ResponseEntityWrapper wrapper) throws Exception{
        VenusFrontendConnection conn = wrapper.getConn();
        VenusRouterPacket routerPacket = wrapper.getRouterPacket();
        Endpoint endpoint = wrapper.getEndpoint();
        SerializeServiceRequestPacket request = wrapper.getRequest();
        Result result = wrapper.getResult();
        short serializeType = wrapper.getSerializeType();
        boolean athenaFlag = wrapper.isAthenaFlag();

        if (result.getErrorCode() == 0) {
            Serializer serializer = SerializerFactory.getSerializer(serializeType);
            ServiceResponsePacket response = new SerializeServiceResponsePacket(serializer, endpoint.getMethod()
                    .getGenericReturnType());
            AbstractServicePacket.copyHead(request, response);
            response.result = result.getResult();
            AbstractServicePacket resultPacket = response;

            postMessageBack(conn, routerPacket, request, response, athenaFlag);
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
            AbstractServicePacket resultPacket = error;

            postMessageBack(conn, routerPacket, request, error, athenaFlag);
        }
    }

    /**
     * 处理OK类型调用
     * @param wrapper
     * @throws Exception
     */
    public void writeResponseForOk(ResponseEntityWrapper wrapper) throws Exception{
        VenusFrontendConnection conn = wrapper.getConn();
        VenusRouterPacket routerPacket = wrapper.getRouterPacket();
        Endpoint endpoint = wrapper.getEndpoint();
        SerializeServiceRequestPacket request = wrapper.getRequest();
        Result result = wrapper.getResult();
        short serializeType = wrapper.getSerializeType();
        boolean athenaFlag = wrapper.isAthenaFlag();

        if (result.getErrorCode() == 0) {
            OKPacket ok = new OKPacket();
            AbstractServicePacket.copyHead(request, ok);
            AbstractServicePacket resultPacket = ok;
            postMessageBack(conn, routerPacket, request, ok, athenaFlag);
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
            AbstractServicePacket resultPacket = error;
            postMessageBack(conn, routerPacket, request, error, athenaFlag);
        }
    }

    /**
     * 处理listener调用
     * @param wrapper
     * @throws Exception
     */
    public void writeResponseForNotify(ResponseEntityWrapper wrapper) throws Exception{
        VenusFrontendConnection conn = wrapper.getConn();
        VenusRouterPacket routerPacket = wrapper.getRouterPacket();
        Endpoint endpoint = wrapper.getEndpoint();
        SerializeServiceRequestPacket request = wrapper.getRequest();
        Result result = wrapper.getResult();
        short serializeType = wrapper.getSerializeType();
        boolean athenaFlag = wrapper.isAthenaFlag();

        if (result.getErrorCode() == 0) {
            Serializer serializer = SerializerFactory.getSerializer(conn.getSerializeType());
            ServiceNofityPacket response = new SerializeServiceNofityPacket(serializer, null);
            AbstractServicePacket.copyHead(request, response);
            response.callbackObject = result.getResult();
            response.apiName = request.apiName;
            response.identityData = new byte[]{};
            //TODO 处理版本兼容问题 listener请求标识数据

            byte[] traceID = (byte[]) ThreadLocalMap.get(VenusTracerUtil.REQUEST_TRACE_ID);
            if (traceID == null) {
                traceID = VenusTracerUtil.randomUUID();
                ThreadLocalMap.put(VenusTracerUtil.REQUEST_TRACE_ID, traceID);
            }
            response.traceId = traceID;
            postMessageBack(conn, routerPacket, request, response, athenaFlag);
        }else{
            Exception e = null;
            if (result.getException() == null) {
                e = new DefaultVenusException(result.getErrorCode(), result.getErrorMessage());
            } else {
                e = result.getException();
            }

            Serializer serializer = SerializerFactory.getSerializer(conn.getSerializeType());
            ServiceNofityPacket response = new SerializeServiceNofityPacket(serializer, null);
            AbstractServicePacket.copyHead(request, response);
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
                    } catch (Exception e1) {
                        logger.error("read config properpty error", e1);
                    }
                }
                response.additionalData = serializer.encode(additionalData);
                response.errorMessage = e.getMessage();
            }

            //TODO listener请求标识数据
            response.apiName = request.apiName;
            response.identityData = new byte[]{};
            byte[] traceID = VenusTracerUtil.getTracerID();
            if (traceID == null) {
                traceID = VenusTracerUtil.randomTracerID();
            }
            response.traceId = traceID;

            postMessageBack(conn, routerPacket, request, response, athenaFlag);
        }
    }

    /**
     * 响应消息
     * @param conn
     * @param routerPacket
     * @param source
     * @param result
     * @param athenaFlag
     */
    void postMessageBack(Connection conn, VenusRouterPacket routerPacket, AbstractServicePacket source, AbstractServicePacket result, boolean athenaFlag) {
        ByteBuffer byteBuffer;
        if (routerPacket == null) {
            byteBuffer = result.toByteBuffer();
            conn.write(byteBuffer);
        } else {
            routerPacket.data = result.toByteArray();
            byteBuffer = routerPacket.toByteBuffer();
            conn.write(byteBuffer);
        }
    }

}
