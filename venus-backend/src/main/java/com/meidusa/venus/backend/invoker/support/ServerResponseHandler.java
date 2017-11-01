package com.meidusa.venus.backend.invoker.support;

/**
 * Created by godzillahua on 7/4/16.
 */
import com.meidusa.toolkit.net.Connection;
import com.meidusa.venus.Result;
import com.meidusa.venus.backend.services.Endpoint;
import com.meidusa.venus.io.network.VenusFrontendConnection;
import com.meidusa.venus.io.packet.*;
import com.meidusa.venus.io.packet.serialize.SerializeServiceNofityPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceResponsePacket;
import com.meidusa.venus.io.serializer.Serializer;
import com.meidusa.venus.io.serializer.SerializerFactory;
import com.meidusa.venus.support.ErrorPacketConvert;
import com.meidusa.venus.util.ThreadLocalMap;
import com.meidusa.venus.util.VenusTracerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * 服务端响应处理类
 */
public class ServerResponseHandler {

    private static Logger logger = LoggerFactory.getLogger(ServerResponseHandler.class);

    /**
     * 处理response同步类型调用
     * @param wrapper
     * @throws Exception
     */
    public void writeResponseForResponse(ServerResponseWrapper wrapper) throws Exception{
        VenusFrontendConnection conn = wrapper.getConn();
        VenusRouterPacket routerPacket = wrapper.getRouterPacket();
        Endpoint endpoint = wrapper.getEndpoint();
        SerializeServiceRequestPacket request = wrapper.getRequest();
        Result result = wrapper.getResult();
        short serializeType = wrapper.getSerializeType();
        Serializer serializer = SerializerFactory.getSerializer(serializeType);
        boolean athenaFlag = wrapper.isAthenaFlag();

        if (result.getErrorCode() == 0) {
            ServiceResponsePacket response = new SerializeServiceResponsePacket(serializer, endpoint.getMethod()
                    .getGenericReturnType());
            AbstractServicePacket.copyHead(request, response);
            response.result = result.getResult();
            AbstractServicePacket resultPacket = response;

            postMessageBack(conn, routerPacket, request, response, athenaFlag);
        }else{
            ErrorPacket error = ErrorPacketConvert.toErrorPacket(result,request,serializer);
            postMessageBack(conn, routerPacket, request, error, athenaFlag);
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
        Endpoint endpoint = wrapper.getEndpoint();
        SerializeServiceRequestPacket request = wrapper.getRequest();
        Result result = wrapper.getResult();
        short serializeType = wrapper.getSerializeType();
        Serializer serializer = SerializerFactory.getSerializer(serializeType);
        boolean athenaFlag = wrapper.isAthenaFlag();

        if (result.getErrorCode() == 0) {
            OKPacket ok = new OKPacket();
            AbstractServicePacket.copyHead(request, ok);
            AbstractServicePacket resultPacket = ok;
            postMessageBack(conn, routerPacket, request, ok, athenaFlag);
        }else{
            ErrorPacket error = ErrorPacketConvert.toErrorPacket(result,request,serializer);
            postMessageBack(conn, routerPacket, request, error, athenaFlag);
        }
    }

    /**
     * 处理listener调用
     * @param wrapper
     * @throws Exception
     */
    public void writeResponseForNotify(ServerResponseWrapper wrapper) throws Exception{
        VenusFrontendConnection conn = wrapper.getConn();
        VenusRouterPacket routerPacket = wrapper.getRouterPacket();
        Endpoint endpoint = wrapper.getEndpoint();
        SerializeServiceRequestPacket request = wrapper.getRequest();
        Result result = wrapper.getResult();
        short serializeType = wrapper.getSerializeType();
        Serializer serializer = SerializerFactory.getSerializer(conn.getSerializeType());
        boolean athenaFlag = wrapper.isAthenaFlag();

        if (result.getErrorCode() == 0) {
            ServiceNofityPacket response = new SerializeServiceNofityPacket(serializer, null);
            AbstractServicePacket.copyHead(request, response);
            response.callbackObject = result.getResult();
            response.apiName = request.apiName;
            response.identityData = new byte[]{};

            byte[] traceID = (byte[]) ThreadLocalMap.get(VenusTracerUtil.REQUEST_TRACE_ID);
            if (traceID == null) {
                traceID = VenusTracerUtil.randomUUID();
                ThreadLocalMap.put(VenusTracerUtil.REQUEST_TRACE_ID, traceID);
            }
            response.traceId = traceID;
            postMessageBack(conn, routerPacket, request, response, athenaFlag);
        }else{
            ServiceNofityPacket response = ErrorPacketConvert.toNotifyPacket(result,request,serializer);
            postMessageBack(conn, routerPacket, request, response, athenaFlag);
        }
    }

    /**
     * 响应消息
     * @param conn
     * @param routerPacket
     * @param source
     * @param response
     * @param athenaFlag
     */
    void postMessageBack(Connection conn, VenusRouterPacket routerPacket, AbstractServicePacket source, AbstractServicePacket response, boolean athenaFlag) {
        ByteBuffer byteBuffer;
        if (routerPacket == null) {
            byteBuffer = response.toByteBuffer();
            conn.write(byteBuffer);
        } else {
            routerPacket.data = response.toByteArray();
            byteBuffer = routerPacket.toByteBuffer();
            conn.write(byteBuffer);
        }
    }

}
