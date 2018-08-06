package com.meidusa.venus.client.invoker.venus.encode;

import com.meidusa.fastjson.JSON;
import com.meidusa.venus.client.ClientInvocation;
import com.meidusa.venus.io.packet.AbstractServiceRequestPacket;
import com.meidusa.venus.io.packet.ServiceNofityPacket;
import com.meidusa.venus.io.packet.ServiceResponsePacket;
import com.meidusa.venus.io.packet.json.JsonVenusNotifyPacket;
import com.meidusa.venus.io.packet.json.JsonVenusRequestPacket;
import com.meidusa.venus.io.packet.json.JsonVenusResponsePacket;
import com.meidusa.venus.io.serializer.Serializer;

import java.lang.reflect.Type;
import java.util.HashMap;

public class HttpEncoder implements BaseEncoder {

    @Override
    public AbstractServiceRequestPacket encode(Object object, Serializer serializer) {
        ClientInvocation invocation = (ClientInvocation)object;
        JsonVenusRequestPacket serviceRequestPacket = new JsonVenusRequestPacket();

        serviceRequestPacket.clientId = invocation.getClientId();
        serviceRequestPacket.clientRequestId = invocation.getClientRequestId();
        //设置traceId
        serviceRequestPacket.traceId = invocation.getTraceID();
        //设置athenaId
        if (invocation.getAthenaId() != null) {
            serviceRequestPacket.rootId = invocation.getAthenaId();
        }
        if (invocation.getParentId() != null) {
            serviceRequestPacket.parentId = invocation.getParentId();
        }
        if (invocation.getMessageId() != null) {
            serviceRequestPacket.messageId = invocation.getMessageId();
        }
        serviceRequestPacket.apiName = invocation.getApiName();
        serviceRequestPacket.serviceVersion = Integer.parseInt(invocation.getVersion());
        serviceRequestPacket.params = JSON.toJSONString(invocation.getParameterMap());

        return serviceRequestPacket;
    }

    @Override
    public ServiceResponsePacket decode(byte[] message, Type retType, Serializer serializer) {
        JsonVenusResponsePacket responsePacket = new JsonVenusResponsePacket();
        responsePacket.init(message);
        return responsePacket;
    }

    @Override
    public ServiceNofityPacket decodeForNotify(byte[] message, Type retType, Serializer serializer) {
        JsonVenusNotifyPacket notifyPacket = new JsonVenusNotifyPacket();
        notifyPacket.init(message);
        return notifyPacket;
    }
}
