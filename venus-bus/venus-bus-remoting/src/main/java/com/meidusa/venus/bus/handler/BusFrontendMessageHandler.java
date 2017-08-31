package com.meidusa.venus.bus.handler;

import com.meidusa.toolkit.net.ConnectionConnector;
import com.meidusa.toolkit.net.MessageHandler;
import com.meidusa.toolkit.util.StringUtil;
import com.meidusa.venus.bus.network.BusFrontendConnection;
import com.meidusa.venus.bus.util.VenusTrafficCollector;
import com.meidusa.venus.io.packet.*;
import com.meidusa.venus.io.support.ShutdownListener;
import com.meidusa.venus.io.support.VenusStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 前端消息处理,负责接收服务请求
 * 
 * @author structchen
 * 
 */
public class BusFrontendMessageHandler implements MessageHandler<BusFrontendConnection, byte[]> {

    private static Logger logger = LoggerFactory.getLogger(BusFrontendMessageHandler.class);

    private static ShutdownListener listener = new ShutdownListener();

    private static Logger REUEST_LOGGER = LoggerFactory.getLogger("venus.tracer");

    static {
        Runtime.getRuntime().addShutdownHook(listener);
    }

    private ConnectionConnector connector;

    public ConnectionConnector getConnector() {
        return connector;
    }

    public void setConnector(ConnectionConnector connector) {
        this.connector = connector;
    }

    @Override
    public void handle(BusFrontendConnection srcConn, final byte[] message) {
    	VenusTrafficCollector.getInstance().addInput(message.length);
        int type = AbstractServicePacket.getType(message);
        switch (type) {
            case PacketConstant.PACKET_TYPE_PING:
                PingPacket ping = new PingPacket();
                ping.init(message);
                PongPacket pong = new PongPacket();
                AbstractServicePacket.copyHead(ping, pong);
                srcConn.write(pong.toByteBuffer());
                if (logger.isDebugEnabled()) {
                    logger.debug("receive ping packet from " + srcConn.getId());
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

                response.status = VenusStatus.getInstance().getStatus();
                srcConn.write(response.toByteBuffer());
                break;
            case PacketConstant.PACKET_TYPE_SERVICE_REQUEST: {
                //TODO 由业务实现
                break;
            }
            case PacketConstant.AUTHEN_TYPE_PASSWORD:

                break;
            default:
                StringBuilder buffer = new StringBuilder("receive unknown type packet from ");
                buffer.append(srcConn.getId()).append("\n");
                buffer.append("-------------------------------").append("\n");
                buffer.append(StringUtil.dumpAsHex(message, message.length)).append("\n");
                buffer.append("-------------------------------").append("\n");
                logger.warn(buffer.toString());

        }
    }

}
