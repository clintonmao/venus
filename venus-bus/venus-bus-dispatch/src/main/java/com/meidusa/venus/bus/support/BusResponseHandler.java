package com.meidusa.venus.bus.support;

import com.meidusa.venus.bus.dispatch.BusDispatcher;
import com.meidusa.venus.bus.network.BusFrontendConnection;
import com.meidusa.venus.io.packet.ErrorPacket;
import com.meidusa.venus.io.packet.OKPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceResponsePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * bus响应工具类 TODO 与server输出响应统一
 * Created by Zhangzhihua on 2017/9/25.
 */
public class BusResponseHandler {

    private static Logger logger = LoggerFactory.getLogger(BusResponseHandler.class);

    /**
     * error响应
     * @param connection
     * @param errorPacket
     */
    public static void writeResponseForError(BusFrontendConnection connection, ErrorPacket errorPacket){
        try {
            connection.write(errorPacket.toByteBuffer());
        } catch (Exception e) {
            logger.error("write response for error failed:{}.",e);
        }
    }

    /**
     * ok响应
     * @param connection
     * @param okPacket
     */
    public static void writeResponseForOk(BusFrontendConnection connection, OKPacket okPacket){
        try {
            connection.write(okPacket.toByteBuffer());
        } catch (Exception e) {
            logger.error("write response for ok failed:{}.",e);
        }
    }

    /**
     * response响应
     * @param connection
     * @param serviceResponsePacket
     */
    public static void writeResponseForResponse(BusFrontendConnection connection, SerializeServiceResponsePacket serviceResponsePacket){
        try {
            connection.write(serviceResponsePacket.toByteBuffer());
        } catch (Exception e) {
            logger.error("write response for error failed:{}.",e);
        }
    }
}
