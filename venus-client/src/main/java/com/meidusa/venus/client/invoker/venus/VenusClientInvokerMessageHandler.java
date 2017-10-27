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
package com.meidusa.venus.client.invoker.venus;

import com.meidusa.toolkit.net.MessageHandler;
import com.meidusa.venus.ClientInvocation;
import com.meidusa.venus.Result;
import com.meidusa.venus.RpcException;
import com.meidusa.venus.exception.VenusExceptionFactory;
import com.meidusa.venus.io.handler.VenusClientMessageHandler;
import com.meidusa.venus.io.network.VenusBackendConnection;
import com.meidusa.venus.io.packet.*;
import com.meidusa.venus.io.packet.serialize.SerializeServiceNofityPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceResponsePacket;
import com.meidusa.venus.io.serializer.Serializer;
import com.meidusa.venus.io.serializer.SerializerFactory;
import com.meidusa.venus.io.utils.RpcIdUtil;
import com.meidusa.venus.util.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 服务调用NIO消息响应处理
 */
public class VenusClientInvokerMessageHandler extends VenusClientMessageHandler implements MessageHandler<VenusBackendConnection, byte[]> {

    private static Logger logger = LoggerFactory.getLogger(VenusClientInvokerMessageHandler.class);

    private VenusExceptionFactory venusExceptionFactory;

    private InvocationListenerContainer container;

    /**
     * rpcId-请求&响应映射表
     */
    private Map<String, VenusReqRespWrapper> serviceReqRespMap;

    /**
     * rpcId-请求&回调映射表 TODO 完成、异常清理问题；及监控大小问题
     */
    private Map<String, ClientInvocation> serviceReqCallbackMap;

    private static boolean isEnableRandomPrint = false;

    public void handle(VenusBackendConnection conn, byte[] message) {
        if("A".equalsIgnoreCase("B")){
            return;
        }
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
        /* TODO 异常处理
        Exception e = venusExceptionFactory.getException(error.errorCode, error.message);
        if (e == null) {
            logger.error("receive error packet,errorCode=" + error.errorCode + ",message=" + error.message);
        } else {
            if (error.additionalData != null) {
                Object obj = serializer.decode(error.additionalData, Utils.getBeanFieldType(e.getClass(), Exception.class));
                try {
                    BeanUtils.copyProperties(e, obj);
                } catch (Exception e1) {
                    logger.error("copy properties error", e1);
                }
            }
            logger.error("receive error packet", e);
        }
        */

        VenusReqRespWrapper reqRespWrapper = null;

        try {
            ErrorPacket errorPacket = new ErrorPacket();
            errorPacket.init(message);
            if(logger.isInfoEnabled()){
                logger.info("recv error response,rpcId:{},response:{}.",RpcIdUtil.getRpcId(errorPacket), JSONUtil.toJSONString(errorPacket));
            }

            String rpcId = RpcIdUtil.getRpcId(errorPacket);
            reqRespWrapper = serviceReqRespMap.get(rpcId);
            if(reqRespWrapper == null){
                //TODO 处理此种情况，记录异常？
                return;
            }
            reqRespWrapper.setPacket(errorPacket);
        } catch (Exception e) {
            if(logger.isErrorEnabled()){
                logger.error("recv and handle error message error.",e);
            }
            ErrorPacket errorPacket = new ErrorPacket();
            errorPacket.errorCode = 500;
            errorPacket.message = e.getLocalizedMessage();
            reqRespWrapper.setPacket(errorPacket);
        } finally {
            if(reqRespWrapper != null){
                reqRespWrapper.getReqRespLatch().countDown();
            }
        }
    }

    /**
     * 处理response类型响应消息
     * @param conn
     * @param message
     * @param serializer
     */
    void handleForResponse(VenusBackendConnection conn, byte[] message,Serializer serializer){
        if("A".equalsIgnoreCase("B")){
            return;
        }

        VenusReqRespWrapper reqRespWrapper = null;

        try {
            AbstractServicePacket packet = parseServicePacket(message);
            String rpcId = RpcIdUtil.getRpcId(packet);
            //获取clientId/clientRequestId，用于获取invocation请求信息
            reqRespWrapper = serviceReqRespMap.get(rpcId);
            if(reqRespWrapper == null){
                //TODO 处理此种情况，记录异常？
                return;
            }

            ClientInvocation syncInvocation = reqRespWrapper.getInvocation();

            ServiceResponsePacket responsePacket = new SerializeServiceResponsePacket(serializer, syncInvocation.getMethod().getGenericReturnType());
            responsePacket.init(message);
            if(logger.isWarnEnabled()){
                logger.warn("recv resp response,rpcId:{},thread:{},response:{}.",rpcId,Thread.currentThread(),JSONUtil.toJSONString(responsePacket));
            }

            if(isEnableRandomPrint){
                if(ThreadLocalRandom.current().nextInt(50000) > 49990){
                    if(logger.isErrorEnabled()){
                        logger.error("recv resp response,rpcId:{},thread:{},instance:{}.",rpcId,Thread.currentThread(),this);
                    }
                }
            }
            //添加rpcId->response映射表
            reqRespWrapper.setPacket(responsePacket);
            //TODO 处理已经超时的记录
        } catch (Exception e) {
            logger.error("recv and handle message error.",e);
            ErrorPacket errorPacket = new ErrorPacket();
            errorPacket.errorCode = 500;
            errorPacket.message = e.getLocalizedMessage();
            reqRespWrapper.setPacket(errorPacket);
        } finally {
            if(reqRespWrapper != null){
                reqRespWrapper.getReqRespLatch().countDown();
            }
        }
    }

    /**
     * 处理ok消息响应
     * @param conn
     * @param message
     * @param serializer
     */
    void handleForOk(VenusBackendConnection conn, byte[] message,Serializer serializer){
        VenusReqRespWrapper reqRespWrapper = null;

        try {
            OKPacket okPacket = new OKPacket();
            okPacket.init(message);
            if(logger.isInfoEnabled()){
                logger.info("recv ok response,rpcId:{},response:{}.",RpcIdUtil.getRpcId(okPacket),JSONUtil.toJSONString(okPacket));
            }

            String rpcId = RpcIdUtil.getRpcId(okPacket);
            reqRespWrapper = serviceReqRespMap.get(rpcId);
            if(reqRespWrapper == null){
                //TODO 处理此种情况，记录异常？
                return;
            }

            reqRespWrapper.setPacket(okPacket);
        } catch (Exception e) {
            logger.error("recv and handle message error.",e);
            ErrorPacket errorPacket = new ErrorPacket();
            errorPacket.errorCode = 500;
            errorPacket.message = e.getLocalizedMessage();
            reqRespWrapper.setPacket(errorPacket);
        } finally {
            if(reqRespWrapper != null){
                reqRespWrapper.getReqRespLatch().countDown();
            }
        }
    }

    /**
     * 处理callback响应消息
     * @param conn
     * @param message
     * @param serializer
     */
    void handleForPublish(VenusBackendConnection conn, byte[] message,Serializer serializer){
        String rpcId = null;
        ClientInvocation asyncInvocation = null;

        try {
            rpcId = RpcIdUtil.getRpcId(parseServicePacket(message));
            asyncInvocation = serviceReqCallbackMap.get(rpcId);

            ServicePacketBuffer buffer = new ServicePacketBuffer(message);
            buffer.setPosition(PacketConstant.SERVICE_HEADER_SIZE + 4);
            //原来用于标识callback请求的listenerClass与identityHashCode统一改为根据rpcId来处理
            String listenerClass = buffer.readLengthCodedString("utf-8");
            int identityHashCode = buffer.readInt();
            /*
            Tuple<InvocationListener, Type> tuple = container.getInvocationListener(listenerClass, identityHashCode);
            */

            SerializeServiceNofityPacket nofityPacket = new SerializeServiceNofityPacket(serializer, asyncInvocation.getType());
            nofityPacket.init(message);
            logger.info("recv notify response,rpcId:{},response:{}.",RpcIdUtil.getRpcId(nofityPacket),JSONUtil.toJSONString(nofityPacket));

            /* TODO 确认代码功能
            VenusTracerUtil.logRequest(packet.traceId, packet.apiName);
            */

            if (nofityPacket.errorCode != 0) {
                //TODO 异常处理方式及additionalData信息
                /*
                Exception exception = venusExceptionFactory.getException(packet.errorCode, packet.errorMessage);
                if (exception == null) {
                    exception = new DefaultVenusException(packet.errorCode, packet.errorMessage);
                } else {
                    if (packet.additionalData != null) {
                        Object obj = serializer.decode(packet.additionalData, Utils.getBeanFieldType(exception.getClass(), Exception.class));
                        try {
                            BeanUtils.copyProperties(exception, obj);
                        } catch (Exception e1) {
                            logger.error("copy properties error", e1);
                        }
                    }
                }
                */
                //TODO 确认异常构造方式
                RpcException exception = new RpcException(String.format("%s-%s",String.valueOf(nofityPacket.errorCode),nofityPacket.errorMessage));
                //TODO 改获取listener方式
                //tuple.left.onException(exception);
                asyncInvocation.getInvocationListener().onException(exception);
            } else {
                //tuple.left.callback(packet.callbackObject);
                asyncInvocation.getInvocationListener().callback(nofityPacket.callbackObject);
            }
        } catch (Exception e) {
            logger.error("recv and handle message error.",e);
            //TODO rpcException设置属性
            RpcException exception = new RpcException(String.format("%s-%s",String.valueOf(500),e.getLocalizedMessage()));
            asyncInvocation.getInvocationListener().onException(exception);
        } finally {
            if(rpcId != null && asyncInvocation != null){
                serviceReqCallbackMap.remove(rpcId);
            }

        }
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

    public VenusExceptionFactory getVenusExceptionFactory() {
        return venusExceptionFactory;
    }

    public void setVenusExceptionFactory(VenusExceptionFactory venusExceptionFactory) {
        this.venusExceptionFactory = venusExceptionFactory;
    }

    public InvocationListenerContainer getContainer() {
        return container;
    }

    public void setContainer(InvocationListenerContainer container) {
        this.container = container;
    }

    public Map<String, ClientInvocation> getServiceReqCallbackMap() {
        return serviceReqCallbackMap;
    }

    public void setServiceReqCallbackMap(Map<String, ClientInvocation> serviceReqCallbackMap) {
        this.serviceReqCallbackMap = serviceReqCallbackMap;
    }

    public Map<String, VenusReqRespWrapper> getServiceReqRespMap() {
        return serviceReqRespMap;
    }

    public void setServiceReqRespMap(Map<String, VenusReqRespWrapper> serviceReqRespMap) {
        this.serviceReqRespMap = serviceReqRespMap;
    }
}
