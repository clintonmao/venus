package com.meidusa.venus.backend.support;

import com.meidusa.toolkit.net.util.InetAddressUtil;
import com.meidusa.venus.ServerInvocation;
import com.meidusa.venus.backend.context.RequestContext;
import com.meidusa.venus.backend.serializer.MediaTypes;
import com.meidusa.venus.backend.services.Endpoint;
import com.meidusa.venus.backend.services.RequestInfo;
import com.meidusa.venus.io.packet.PacketConstant;
import com.meidusa.venus.io.packet.VenusRouterPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;

/**
 * Created by GodzillaHua on 7/4/16.
 */
public class ServerRequestHandler {

    public RequestInfo getRequestInfo(byte packetSerializeType, VenusRouterPacket routerPacket, ServerInvocation invocation) {
        RequestInfo info = new RequestInfo();
        if (routerPacket != null) {
            info.setRemoteIp(InetAddressUtil.intToAddress(routerPacket.srcIP));
        } else {
            info.setRemoteIp(invocation.getHost());
        }
        info.setProtocol(RequestInfo.Protocol.SOCKET);
        info.setClientId(invocation.getClientId());
        if (packetSerializeType == PacketConstant.CONTENT_TYPE_JSON) {
            info.setAccept(MediaTypes.APPLICATION_JSON);
        } else if (packetSerializeType == PacketConstant.CONTENT_TYPE_BSON) {
            // info.setAccept(MediaTypes.APPLICATION_XML);
        } else if (packetSerializeType == PacketConstant.CONTENT_TYPE_OBJECT) {
            info.setAccept(MediaTypes.APPLICATION_XML);
        }

        return info;
    }

    public RequestContext createContext(RequestInfo info, Endpoint endpoint, SerializeServiceRequestPacket request) {
        RequestContext context = new RequestContext();
        context.setParameters(request.parameterMap);
        context.setEndPointer(endpoint);
        context.setRequestInfo(info);
        if (request.rootId != null && request.rootId.length > 0) {
            context.setRootId(new String(request.rootId));
        }

        if (request.parentId != null && request.parentId.length > 0) {
            context.setParentId(new String(request.parentId));
        }

        if (request.messageId != null && request.messageId.length > 0) {
            context.setMessageId(new String(request.messageId));
        }
        return context;
    }
}
