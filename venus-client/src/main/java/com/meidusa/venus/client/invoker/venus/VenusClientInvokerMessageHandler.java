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
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.exception.XmlVenusExceptionFactory;
import com.meidusa.venus.io.handler.VenusClientMessageHandler;
import com.meidusa.venus.io.network.VenusBackendConnection;
import com.meidusa.venus.io.packet.*;
import com.meidusa.venus.io.packet.serialize.SerializeServiceNofityPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceResponsePacket;
import com.meidusa.venus.io.serializer.Serializer;
import com.meidusa.venus.io.serializer.SerializerFactory;
import com.meidusa.venus.io.utils.RpcIdUtil;
import com.meidusa.venus.support.ErrorPacketConvert;
import com.meidusa.venus.util.JSONUtil;
import com.meidusa.venus.util.VenusLoggerFactory;
import com.meidusa.venus.util.VenusTracerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;

/**
 * 服务调用NIO消息响应处理
 */
public class VenusClientInvokerMessageHandler extends VenusClientMessageHandler implements MessageHandler<VenusBackendConnection, byte[]> {

    private static Logger logger = LoggerFactory.getLogger(VenusClientInvokerMessageHandler.class);

    private static Logger tracerLogger = VenusLoggerFactory.getClientTracerLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getClientExceptionLogger();

    /**
     * rpcId-请求&响应映射表
     */
    private Map<String, VenusReqRespWrapper> serviceReqRespMap;

    /**
     * rpcId-请求&回调映射表 TODO 完成、异常清理问题；及监控大小问题
     */
    private Map<String, ClientInvocation> serviceReqCallbackMap;

    private static boolean isEnableRandomPrint = false;

    //默认不使用业务线程池模式
    private static boolean isEnableExecuteModel = false;
    private static Executor executor = null;
    private int coreThread = Runtime.getRuntime().availableProcessors();
    private int maxThread = 50;
    private int maxQueue = 50000;

    public VenusClientInvokerMessageHandler(){
        init();
    }

    void init() {
        if(isEnableExecuteModel){
            synchronized (VenusClientInvokerMessageHandler.class){
                if (executor == null) {
                    executor = new ThreadPoolExecutor(coreThread,maxThread,0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(maxQueue),new RejectedExecutionHandler(){
                        @Override
                        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                            logger.error("exceed max process,maxThread:{},maxQueue:{}.",maxThread,maxQueue);
                        }
                    });
                }
            }
        }
    }

    public void handle(final VenusBackendConnection conn, final byte[] message) {
        if(isEnableExecuteModel){
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    doHandle(conn, message);
                }
            };
            executor.execute(task);
        }else{
            doHandle(conn, message);
        }
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
        VenusReqRespWrapper reqRespWrapper = null;

        try {
            ErrorPacket errorPacket = new ErrorPacket();
            errorPacket.init(message);
            String rpcId = RpcIdUtil.getRpcId(errorPacket);
            if(tracerLogger.isInfoEnabled()){
                tracerLogger.info("recv error message,rpcId:{}.",rpcId);
            }

            reqRespWrapper = serviceReqRespMap.get(rpcId);
            if(reqRespWrapper == null){
                if(exceptionLogger.isErrorEnabled()){
                    exceptionLogger.error("handle error message failed,rpcId:{},reason:{}.",rpcId,"Already handled.");
                }
                return;
            }

            Throwable exception = ErrorPacketConvert.toExceptionFromErrorPacket(errorPacket,serializer, XmlVenusExceptionFactory.getInstance());
            reqRespWrapper.setResult(new Result().setException(exception));
        } catch (Exception e) {
            reqRespWrapper.setResult(new Result().setException(e));
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
        VenusReqRespWrapper reqRespWrapper = null;

        try {
            AbstractServicePacket packet = parseServicePacket(message);
            String rpcId = RpcIdUtil.getRpcId(packet);
            if(tracerLogger.isInfoEnabled()){
                tracerLogger.info("recv reponse message,rpcId:{}.",rpcId);
            }

            //获取clientId/clientRequestId，用于获取invocation请求信息
            reqRespWrapper = serviceReqRespMap.get(rpcId);
            if(reqRespWrapper == null){
                if(exceptionLogger.isErrorEnabled()){
                    exceptionLogger.error("handle response message failed,rpcId:{},reason:{}.",rpcId,"Already handled.");
                }
                return;
            }

            ClientInvocation syncInvocation = reqRespWrapper.getInvocation();

            ServiceResponsePacket responsePacket = new SerializeServiceResponsePacket(serializer, syncInvocation.getMethod().getGenericReturnType());
            responsePacket.init(message);

            //添加rpcId->response映射表
            reqRespWrapper.setResult(new Result(responsePacket.result));
        } catch (Exception e) {
            reqRespWrapper.setResult(new Result().setException(e));
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
            String rpcId = RpcIdUtil.getRpcId(okPacket);
            if(tracerLogger.isInfoEnabled()){
                tracerLogger.info("recv ok message,rpcId:{}.",rpcId);
            }

            reqRespWrapper = serviceReqRespMap.get(rpcId);
            if(reqRespWrapper == null){
                if(exceptionLogger.isErrorEnabled()){
                    exceptionLogger.error("handle error message failed,rpcId:{},reason:{}.",rpcId,"Already handled.");
                }
                return;
            }

            reqRespWrapper.setResult(new Result(null));
        } catch (Exception e) {
            reqRespWrapper.setResult(new Result().setException(e));
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
            if(tracerLogger.isInfoEnabled()){
                tracerLogger.info("recv notify message,rpcId:{}.",rpcId);
            }

            asyncInvocation = serviceReqCallbackMap.get(rpcId);

            ServicePacketBuffer buffer = new ServicePacketBuffer(message);
            buffer.setPosition(PacketConstant.SERVICE_HEADER_SIZE + 4);
            //原来用于标识callback请求的listenerClass与identityHashCode统一改为根据rpcId来处理
            String listenerClass = buffer.readLengthCodedString("utf-8");
            int identityHashCode = buffer.readInt();
            SerializeServiceNofityPacket nofityPacket = new SerializeServiceNofityPacket(serializer, asyncInvocation.getType());
            nofityPacket.init(message);

            if (nofityPacket.errorCode != 0) {
                Throwable t = ErrorPacketConvert.toExceptionFromNotifyPacket(nofityPacket,serializer,XmlVenusExceptionFactory.getInstance());
                Exception exception = null;
                if(t instanceof  Exception){
                    exception = (Exception)t;
                }else{
                    exception = new RpcException(nofityPacket.errorCode,nofityPacket.errorMessage);
                }
                asyncInvocation.getInvocationListener().onException(exception);
            } else {
                asyncInvocation.getInvocationListener().callback(nofityPacket.callbackObject);
            }
        } catch (Exception e) {
            asyncInvocation.getInvocationListener().onException(e);
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
