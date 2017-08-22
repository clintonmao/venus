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

import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.toolkit.net.MessageHandler;
import com.meidusa.venus.Invocation;
import com.meidusa.venus.exception.DefaultVenusException;
import com.meidusa.venus.exception.VenusExceptionFactory;
import com.meidusa.venus.io.handler.VenusClientMessageHandler;
import com.meidusa.venus.io.network.VenusBackendConnection;
import com.meidusa.venus.io.packet.*;
import com.meidusa.venus.io.packet.serialize.SerializeServiceNofityPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceResponsePacket;
import com.meidusa.venus.io.serializer.Serializer;
import com.meidusa.venus.io.serializer.SerializerFactory;
import com.meidusa.venus.notify.InvocationListener;
import com.meidusa.venus.util.Utils;
import com.meidusa.venus.util.VenusTracerUtil;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * 服务调用NIO消息响应处理
 */
public class VenusClientInvokerMessageHandler extends VenusClientMessageHandler implements MessageHandler<VenusBackendConnection, byte[]> {

    private static Logger logger = LoggerFactory.getLogger(VenusClientInvokerMessageHandler.class);

    private VenusExceptionFactory venusExceptionFactory;

    private InvocationListenerContainer container;

    //TODO lock传递
    private Object lock;

    /**
     * 消息标识-请求映射表
     */
    private Map<String, Invocation> serviceInvocationMap;

    /**
     * 消息标识-响应映射表
     */
    private Map<String,AbstractServicePacket> serviceResponseMap;

    public void handle(VenusBackendConnection conn, byte[] message) {
        Method method = null;//invocation.getMethod();
        Serializer serializer = SerializerFactory.getSerializer(conn.getSerializeType());

        int type = AbstractServicePacket.getType(message);
        switch (type) {
            case PacketConstant.PACKET_TYPE_ERROR:
                ErrorPacket error = new ErrorPacket();
                error.init(message);
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
                logger.info("recv error response, clientId:{},messageId:{},response:{}.",error.clientId,error.clientRequestId,error);
                serviceResponseMap.put(getMessageId(error),error);
                synchronized (lock){
                    lock.notify();
                }

                break;
            case PacketConstant.PACKET_TYPE_OK:
                OKPacket ok = new OKPacket();
                ok.init(message);
                logger.info("recv ok response, clientId:{},messageId:{},response:{}.",ok.clientId,ok.clientRequestId,ok);
                serviceResponseMap.put(getMessageId(ok),ok);
                synchronized (lock){
                    lock.notify();
                }
                break;
            case PacketConstant.PACKET_TYPE_SERVICE_RESPONSE:
                //获取clientId/clientRequestId，用于获取invocation请求信息
                OKPacket resp = new OKPacket();
                resp.init(message);
                Invocation invocation = serviceInvocationMap.get(getMessageId(resp));

                ServiceResponsePacket response = new SerializeServiceResponsePacket(serializer, invocation.getMethod().getGenericReturnType());
                response.init(message);
                logger.info("recv resp response,clientId:{},messageId:{},response:{}.",response.clientId,response.clientRequestId,response);
                serviceResponseMap.put(getMessageId(response),response);
                synchronized (lock){
                    lock.notify();
                }
                break;
            case PacketConstant.PACKET_TYPE_NOTIFY_PUBLISH:
                SerializeServiceNofityPacket packet = null;
                ServicePacketBuffer buffer = new ServicePacketBuffer(message);
                buffer.setPosition(PacketConstant.SERVICE_HEADER_SIZE + 4);

                String listenerClass = buffer.readLengthCodedString("utf-8");
                int identityHashCode = buffer.readInt();

                Tuple<InvocationListener, Type> tuple = container.getInvocationListener(listenerClass, identityHashCode);

                packet = new SerializeServiceNofityPacket(serializer, tuple.right);

                packet.init(message);

                VenusTracerUtil.logRequest(packet.traceId, packet.apiName);
                if (packet.errorCode != 0) {
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
                    tuple.left.onException(exception);
                } else {
                    tuple.left.callback(packet.callbackObject);
                }
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

    String getMessageId(AbstractServicePacket response){
        return String.format("%s-%s",String.valueOf(response.clientId),String.valueOf(response.clientRequestId));
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

    public Object getLock() {
        return lock;
    }

    public void setLock(Object lock) {
        this.lock = lock;
    }

    public Map<String, AbstractServicePacket> getServiceResponseMap() {
        return serviceResponseMap;
    }

    public void setServiceResponseMap(Map<String, AbstractServicePacket> serviceResponseMap) {
        this.serviceResponseMap = serviceResponseMap;
    }

    public Map<String, Invocation> getServiceInvocationMap() {
        return serviceInvocationMap;
    }

    public void setServiceInvocationMap(Map<String, Invocation> serviceInvocationMap) {
        this.serviceInvocationMap = serviceInvocationMap;
    }
}
