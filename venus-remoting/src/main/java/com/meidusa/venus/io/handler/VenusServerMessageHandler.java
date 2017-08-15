package com.meidusa.venus.io.handler;

import com.meidusa.toolkit.common.bean.util.Initialisable;
import com.meidusa.toolkit.common.bean.util.InitialisationException;
import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.toolkit.net.Connection;
import com.meidusa.toolkit.net.MessageHandler;
import com.meidusa.toolkit.net.util.InetAddressUtil;
import com.meidusa.toolkit.util.StringUtil;
import com.meidusa.toolkit.util.TimeUtil;
import com.meidusa.venus.exception.VenusExceptionCodeConstant;
import com.meidusa.venus.io.network.VenusFrontendConnection;
import com.meidusa.venus.io.packet.*;
import com.meidusa.venus.io.support.VenusStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * venus服务端消息处理类
 * @author structchen
 *
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class VenusServerMessageHandler implements MessageHandler<VenusFrontendConnection, Tuple<Long, byte[]>>, Initialisable {

    private static Logger logger = LoggerFactory.getLogger(VenusServerMessageHandler.class);

    @Override
    public void init() throws InitialisationException {
    }

    @Override
    public void handle(final VenusFrontendConnection conn,final Tuple<Long, byte[]> data) {
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

        switch (type) {
            case PacketConstant.PACKET_TYPE_PING:
                PingPacket ping = new PingPacket();
                ping.init(message);
                PongPacket pong = new PongPacket();
                AbstractServicePacket.copyHead(ping, pong);
                postMessageBack(conn, null, ping, pong);
                if (logger.isDebugEnabled()) {
                    logger.debug("receive ping packet from " + conn.getHost() + ":" + conn.getPort());
                }
                break;

            // ignore this packet
            case PacketConstant.PACKET_TYPE_PONG:
                break;
            case PacketConstant.PACKET_TYPE_VENUS_STATUS_REQUEST:
                VenusStatusRequestPacket sr = new VenusStatusRequestPacket();
                sr.init(message);
                VenusStatusResponsePacket response = new VenusStatusResponsePacket();
                AbstractServicePacket.copyHead(sr, response);
                if(sr.newStatus != 0){
                	VenusStatus.getInstance().setStatus(sr.newStatus);
                }
                
                response.status = VenusStatus.getInstance().getStatus();
                postMessageBack(conn, null, sr, response);

                break;
            case PacketConstant.PACKET_TYPE_SERVICE_REQUEST:
                //远程调用消息处理
                //TODO 不支持，需要子类实现 requestMessageHandler.handle(conn, data);
                break;
            default:
                StringBuilder buffer = new StringBuilder("receive unknown packet type=" + type + "  from ");
                buffer.append(conn.getHost() + ":" + conn.getPort()).append("\n");
                buffer.append("-------------------------------").append("\n");
                buffer.append(StringUtil.dumpAsHex(message, message.length)).append("\n");
                buffer.append("-------------------------------").append("\n");
                ServiceHeadPacket head = new ServiceHeadPacket();
                head.init(message);
                ErrorPacket error = new ErrorPacket();

                AbstractServicePacket.copyHead(head, error);
                error.errorCode = VenusExceptionCodeConstant.PACKET_DECODE_EXCEPTION;
                error.message = "receive unknown packet type=" + type + "  from " + conn.getHost() + ":" + conn.getPort();
                postMessageBack(conn, routerPacket, head, error);

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

}
