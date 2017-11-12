/*
 * Copyright 2008-2108 amoeba.meidusa.com 
 * 
 * 	This program is free software; you can redistribute it and/or modify it under the terms of 
 * the GNU AFFERO GENERAL PUBLIC LICENSE as published by the Free Software Foundation; either version 3 of the License, 
 * or (at your option) any later version. 
 * 
 * 	This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU AFFERO GENERAL PUBLIC LICENSE for more details. 
 * 	You should have received a copy of the GNU AFFERO GENERAL PUBLIC LICENSE along with this program; 
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.meidusa.venus.bus.dispatch;

import com.meidusa.toolkit.net.MessageHandler;
import com.meidusa.venus.ClientInvocation;
import com.meidusa.venus.Result;
import com.meidusa.venus.client.invoker.venus.VenusReqRespWrapper;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.exception.XmlVenusExceptionFactory;
import com.meidusa.venus.io.handler.VenusClientMessageHandler;
import com.meidusa.venus.io.network.VenusBackendConnection;
import com.meidusa.venus.io.network.VenusFrontendConnection;
import com.meidusa.venus.io.packet.*;
import com.meidusa.venus.io.packet.serialize.SerializeServiceNofityPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceResponsePacket;
import com.meidusa.venus.io.serializer.Serializer;
import com.meidusa.venus.io.serializer.SerializerFactory;
import com.meidusa.venus.io.utils.RpcIdUtil;
import com.meidusa.venus.support.ErrorPacketConvert;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.*;

/**
 * 服务调用NIO消息响应处理
 */
public class BusDispatcherMessageHandler extends VenusClientMessageHandler implements MessageHandler<VenusBackendConnection, byte[]> {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger tracerLogger = VenusLoggerFactory.getTracerLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    /**
     * rpcId-前端请求连接映射表
     */
    private Map<String, VenusFrontendConnection> reqFrontConnMap;

    public void handle(final VenusBackendConnection conn, final byte[] message) {
        doHandle(conn, message);
    }

    /**
     * 处理消息
     * @param conn
     * @param message
     */
    void doHandle(VenusBackendConnection conn, byte[] message) {
        //获取序列化
        Serializer serializer = SerializerFactory.getSerializer(conn.getSerializeType());
        int type = AbstractServicePacket.getType(message);
        switch (type) {
            case PacketConstant.PACKET_TYPE_ERROR:
                //处理error响应消息
                handleForError(conn,message,serializer);
                break;
            case PacketConstant.PACKET_TYPE_OK:
                //处理ok响应消息
                handleForOk(conn,message,serializer);
                break;
            case PacketConstant.PACKET_TYPE_SERVICE_RESPONSE:
                //处理response响应消息
                handleForResponse(conn,message,serializer);
                break;
            case PacketConstant.PACKET_TYPE_NOTIFY_PUBLISH:
                //处理publish响应消息
                handleForPublish(conn,message,serializer);
                break;
            case PacketConstant.PACKET_TYPE_PONG:
                super.handle(conn, message);
                break;
            case PacketConstant.PACKET_TYPE_PING:
                super.handle(conn, message);
                break;
            default:
                super.handle(conn, message);
        }
    }

    /**
     * 处理error类型消息响应
     * @param conn
     * @param message
     * @param serializer
     */
    void handleForError(VenusBackendConnection conn, byte[] message,Serializer serializer){
        ErrorPacket errorPacket = new ErrorPacket();
        errorPacket.init(message);
        String rpcId = RpcIdUtil.getRpcId(errorPacket);
        if(tracerLogger.isInfoEnabled()){
            tracerLogger.info("recv error message,rpcId:{}.",rpcId);
        }

        //TODO 判断空，finally释放资源
        reqFrontConnMap.get(rpcId).write(message);
    }

    /**
     * 处理response类型响应消息
     * @param conn
     * @param message
     * @param serializer
     */
    void handleForResponse(VenusBackendConnection conn, byte[] message,Serializer serializer){
        AbstractServicePacket packet = parseServicePacket(message);
        String rpcId = RpcIdUtil.getRpcId(packet);
        if(tracerLogger.isInfoEnabled()){
            tracerLogger.info("recv reponse,rpcId:{}.",rpcId);
        }

        reqFrontConnMap.get(rpcId).write(message);
    }

    /**
     * 处理ok消息响应
     * @param conn
     * @param message
     * @param serializer
     */
    void handleForOk(VenusBackendConnection conn, byte[] message,Serializer serializer){
        OKPacket okPacket = new OKPacket();
        okPacket.init(message);
        String rpcId = RpcIdUtil.getRpcId(okPacket);
        if(tracerLogger.isInfoEnabled()){
            tracerLogger.info("recv ok message,rpcId:{}.",rpcId);
        }

        reqFrontConnMap.get(rpcId).write(message);
    }

    /**
     * 处理callback响应消息
     * @param conn
     * @param message
     * @param serializer
     */
    void handleForPublish(VenusBackendConnection conn, byte[] message,Serializer serializer){
        String rpcId = RpcIdUtil.getRpcId(parseServicePacket(message));
        if(tracerLogger.isInfoEnabled()){
            tracerLogger.info("recv notify message,rpcId:{}.",rpcId);
        }

        reqFrontConnMap.get(rpcId).write(message);
    }

    /**
     * 解析基本报文信息
     * @param message
     * @return
     */
    AbstractServicePacket parseServicePacket(byte[] message){
        OKPacket okPacket = new OKPacket();
        okPacket.init(message);
        return okPacket;
    }

    public Map<String, VenusFrontendConnection> getReqFrontConnMap() {
        return reqFrontConnMap;
    }

    public void setReqFrontConnMap(Map<String, VenusFrontendConnection> reqFrontConnMap) {
        this.reqFrontConnMap = reqFrontConnMap;
    }
}
