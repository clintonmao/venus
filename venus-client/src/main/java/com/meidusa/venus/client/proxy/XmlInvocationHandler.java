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

package com.meidusa.venus.client.proxy;

import com.meidusa.fastjson.JSON;
import com.meidusa.fastmark.feature.SerializerFeature;
import com.meidusa.toolkit.common.bean.config.ConfigurationException;
import com.meidusa.toolkit.common.bean.util.InitialisationException;
import com.meidusa.toolkit.common.poolable.MultipleLoadBalanceObjectPool;
import com.meidusa.toolkit.common.poolable.ObjectPool;
import com.meidusa.toolkit.common.poolable.PoolableObjectPool;
import com.meidusa.toolkit.net.*;
import com.meidusa.toolkit.util.StringUtil;
import com.meidusa.toolkit.util.TimeUtil;
import com.meidusa.venus.annotations.*;
import com.meidusa.venus.annotations.util.AnnotationUtil;
import com.meidusa.venus.client.InvocationListenerContainer;
import com.meidusa.venus.client.config.*;
import com.meidusa.venus.client.factory.xml.XmlServiceFactory;
import com.meidusa.venus.client.factory.xml.support.VenusNIOMessageHandler;
import com.meidusa.venus.exception.*;
import com.meidusa.venus.extension.athena.AthenaTransactionId;
import com.meidusa.venus.extension.athena.delegate.AthenaTransactionDelegate;
import com.meidusa.venus.io.network.AbstractBIOConnection;
import com.meidusa.venus.io.network.VenusBIOConnectionFactory;
import com.meidusa.venus.io.network.VenusBackendConnectionFactory;
import com.meidusa.venus.io.packet.*;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceResponsePacket;
import com.meidusa.venus.io.serializer.Serializer;
import com.meidusa.venus.io.serializer.SerializerFactory;
import com.meidusa.venus.metainfo.EndpointParameter;
import com.meidusa.venus.notify.InvocationListener;
import com.meidusa.venus.notify.ReferenceInvocationListener;
import com.meidusa.venus.poolable.RequestLoadbalanceObjectPool;
import com.meidusa.venus.util.UUID;
import com.meidusa.venus.util.Utils;
import com.meidusa.venus.util.VenusAnnotationUtils;
import com.meidusa.venus.util.VenusTracerUtil;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 
 * @author Struct
 * 
 */
public class XmlInvocationHandler extends AbstractInvocationHandler {

    private static Logger logger = LoggerFactory.getLogger(XmlInvocationHandler.class);

    private static Logger performanceLogger = LoggerFactory.getLogger("venus.client.performance");

    private static SerializerFeature[] JSON_FEATURE = new SerializerFeature[]{SerializerFeature.ShortString,SerializerFeature.IgnoreNonFieldGetter,SerializerFeature.SkipTransientField};

    private byte serializeType = PacketConstant.CONTENT_TYPE_JSON;

    private static AtomicLong sequenceId = new AtomicLong(1);

    private InvocationListenerContainer container;

    private XmlServiceFactory serviceFactory;

    private VenusExceptionFactory venusExceptionFactory;

    private boolean enableAsync = true;

    private boolean needPing = false;

    private VenusNIOMessageHandler handler;

    private ConnectionConnector connector;

    /**
     * 远程连接配置，包含ip相关信息
     */
    private RemoteConfig remoteConfig;

    /**
     * bio连接池映射表
     */
    private Map<String, ObjectPool> bioPoolMap = new HashMap<String, ObjectPool>(); // NOPMD


    /**
     * nio连接映射表
     */
    private Map<String, BackendConnectionPool> nioPoolMap = new HashMap<String, BackendConnectionPool>(); // NOPMD

    /**
     * 连接池映射表
     */
    private Map<String, Object> realPoolMap = new HashMap<String, Object>();

    public void init() throws InitialisationException {}

    /**
     * 远程调用
     * @param service
     * @param endpoint
     * @param method
     * @param params
     * @param args
     * @return
     * @throws Exception
     */
    protected Object invokeRemoteService(Service service, Endpoint endpoint, Method method, EndpointParameter[] params, Object[] args) throws Exception {
        byte[] traceID = VenusTracerUtil.getTracerID();
        if (traceID == null) {
            traceID = VenusTracerUtil.randomTracerID();
        }

        boolean async = false;
        if (endpoint.async()) {
            async = true;
        }

        Serializer serializer = SerializerFactory.getSerializer(serializeType);

        //构造请求消息体
        SerializeServiceRequestPacket serviceRequestPacket = buildInvocation(service,endpoint,method,params,args,traceID,async,serializer);

        //调用
        if (async) {
            return doInvokeWithAsync(traceID, service, endpoint, serviceRequestPacket);
        } else {
            return doInvokeWithSync(traceID, service, endpoint, method, serviceRequestPacket, serializer);
        }
    }

    /**
     * 构造请求消息体
     * @param service
     * @param endpoint
     * @param method
     * @param params
     * @param args
     * @param async
     * @return
     */
    SerializeServiceRequestPacket buildInvocation(Service service, Endpoint endpoint, Method method,EndpointParameter[] params, Object[] args,byte[] traceID,boolean async,Serializer serializer){
        String apiName = VenusAnnotationUtils.getApiname(method, service, endpoint);

        AthenaTransactionId athenaTransactionId = null;
        if (service.athenaFlag()) {
            athenaTransactionId = AthenaTransactionDelegate.getDelegate().startClientTransaction(apiName);
        }

        SerializeServiceRequestPacket serviceRequestPacket = new SerializeServiceRequestPacket(serializer, null);

        serviceRequestPacket.clientId = PacketConstant.VENUS_CLIENT_ID;
        serviceRequestPacket.clientRequestId = sequenceId.getAndIncrement();
        serviceRequestPacket.traceId = traceID;
        serviceRequestPacket.apiName = apiName;
        serviceRequestPacket.serviceVersion = service.version();
        serviceRequestPacket.parameterMap = new HashMap<String, Object>();

        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if (args[i] instanceof InvocationListener) {
                    async = true;
                    ReferenceInvocationListener listener = new ReferenceInvocationListener();
                    ServicePacketBuffer buffer = new ServicePacketBuffer(16);
                    buffer.writeLengthCodedString(args[i].getClass().getName(), "utf-8");
                    buffer.writeInt(System.identityHashCode(args[i]));
                    listener.setIdentityData(buffer.toByteBuffer().array());
                    Type type = method.getGenericParameterTypes()[i];
                    if (type instanceof ParameterizedType) {
                        ParameterizedType genericType = ((ParameterizedType) type);
                        container.putInvocationListener((InvocationListener) args[i], genericType.getActualTypeArguments()[0]);
                    } else {
                        throw new InvalidParameterException("invocationListener is not generic");
                    }

                    serviceRequestPacket.parameterMap.put(params[i].getParamName(), listener);
                } else {
                    serviceRequestPacket.parameterMap.put(params[i].getParamName(), args[i]);
                }

            }
        }
        setTransactionId(serviceRequestPacket, athenaTransactionId);
        return serviceRequestPacket;
    }

    /**
     * async异步调用
     * @param traceID
     * @param service
     * @param endpoint
     * @param serviceRequestPacket
     * @return
     * @throws Exception
     */
    Object doInvokeWithAsync(byte[] traceID, Service service, Endpoint endpoint, SerializeServiceRequestPacket serviceRequestPacket) throws Exception{
        if (!this.isEnableAsync()) {
            throw new VenusConfigException("service async call disabled");
        }

        long start = TimeUtil.currentTimeMillis();
        long borrowed = start;

        BackendConnectionPool nioConnPool = null;
        BackendConnection conn = null;
        try {
            nioConnPool = getNioConnPool();
            //TODO 处理loadbanlance问题
            if (nioConnPool instanceof RequestLoadbalanceObjectPool) {
                conn = (BackendConnection) ((RequestLoadbalanceObjectPool) nioConnPool).borrowObject(serviceRequestPacket.parameterMap, endpoint);
            } else {
                conn = nioConnPool.borrowObject();
            }
            borrowed = TimeUtil.currentTimeMillis();
            ByteBuffer buffer = serviceRequestPacket.toByteBuffer();
            if(service.athenaFlag()) {
                AthenaTransactionDelegate.getDelegate().setClientOutputSize(buffer.limit());
            }
            conn.write(buffer);
            VenusTracerUtil.logRequest(traceID, serviceRequestPacket.apiName, JSON.toJSONString(serviceRequestPacket.parameterMap,JSON_FEATURE));
            return null;
        } finally {
            if (service.athenaFlag()) {
                AthenaTransactionDelegate.getDelegate().completeClientTransaction();
            }
            if (performanceLogger.isDebugEnabled()) {
                long end = TimeUtil.currentTimeMillis();
                long time = end - borrowed;
                StringBuffer buffer = new StringBuffer();
                buffer.append("[").append(borrowed - start).append(",").append(time).append("]ms (client-async) traceID=").append(UUID.toString(traceID)).append(", api=").append(serviceRequestPacket.apiName);

                performanceLogger.debug(buffer.toString());
            }

            if (conn != null && nioConnPool != null) {
                nioConnPool.returnObject(conn);
            }
        }
    }

    /**
     * sync同步调用
     * @param traceID
     * @param service
     * @param endpoint
     * @param method
     * @param serviceRequestPacket
     * @param serializer
     * @return
     * @throws Exception
     */
    Object doInvokeWithSync(byte[] traceID, Service service, Endpoint endpoint, Method method, SerializeServiceRequestPacket serviceRequestPacket, Serializer serializer) throws Exception{
        long start = TimeUtil.currentTimeMillis();
        long borrowed = start;
        int soTimeout = 0;
        int oldTimeout = 0;
        boolean success = true;
        int errorCode = 0;
        AbstractServicePacket packet = null;
        String remoteAddress = null;
        boolean invalided = false;
        boolean nullForSystemException = false;

        ObjectPool bioConnPool = null;
        AbstractBIOConnection conn = null;
        try {
            bioConnPool = getBioConnPool();
            if (bioConnPool instanceof RequestLoadbalanceObjectPool) {
                conn = (AbstractBIOConnection) ((RequestLoadbalanceObjectPool) bioConnPool).borrowObject(serviceRequestPacket.parameterMap, endpoint);
            } else {
                conn = (AbstractBIOConnection) bioConnPool.borrowObject();
            }
            remoteAddress =  conn.getRemoteAddress();
            borrowed = TimeUtil.currentTimeMillis();
            ServiceConfig config = this.serviceFactory.getServiceConfig(method.getDeclaringClass());

            oldTimeout = conn.getSoTimeout();
            if (config != null) {
                EndpointConfig endpointConfig = config.getEndpointConfig(endpoint.name());
                if (endpointConfig != null) {
                    int eTimeOut = endpointConfig.getTimeWait();
                    if (eTimeOut > 0) {
                        soTimeout = eTimeOut;
                    }
                } else {
                    if (config.getTimeWait() > 0) {
                        soTimeout = config.getTimeWait();
                    } else {
                        if (endpoint.timeWait() > 0) {
                            soTimeout = endpoint.timeWait();
                        }
                    }
                }

            } else {

                if (endpoint.timeWait() > 0) {
                    soTimeout = endpoint.timeWait();
                }
            }

            byte[] bts;

            try {

                if (soTimeout > 0) {
                    conn.setSoTimeout(soTimeout);
                }

                byte[] buff = serviceRequestPacket.toByteArray();
                if(service.athenaFlag() && buff != null) {
                    AthenaTransactionDelegate.getDelegate().setClientOutputSize(buff.length);
                }
                conn.write(buff);
                VenusTracerUtil.logRequest(traceID, serviceRequestPacket.apiName, JSON.toJSONString(serviceRequestPacket.parameterMap,JSON_FEATURE));
                bts = conn.read();
                if(service.athenaFlag() && bts != null) {
                    AthenaTransactionDelegate.getDelegate().setClientInputSize(bts.length);
                }
            }catch(IOException e){
                try {
                    conn.close();
                } catch (Exception e1) {
                    // ignore
                }

                bioConnPool.invalidateObject(conn);
                invalided = true;
                Class<?>[] eClass = method.getExceptionTypes();

                if(eClass != null && eClass.length > 0){
                    for(Class<?> clazz : eClass){
                        if(e.getClass().isAssignableFrom(clazz)){
                            throw e;
                        }
                    }
                }

                throw new RemoteSocketIOException("api="+serviceRequestPacket.apiName+ ", remoteIp=" + conn.getRemoteAddress()+",("+e.getMessage() + ")",e);
            }

            int type = AbstractServicePacket.getType(bts);
            switch (type) {
                case PacketConstant.PACKET_TYPE_ERROR:
                    ErrorPacket error = new ErrorPacket();
                    error.init(bts);
                    packet = error;
                    Exception e = venusExceptionFactory.getException(error.errorCode, error.message);
                    if (e == null) {
                        throw new DefaultVenusException(error.errorCode, error.message);
                    } else {
                        if (error.additionalData != null) {
                            Map<String, Type> tmap = Utils.getBeanFieldType(e.getClass(), Exception.class);
                            if (tmap != null && tmap.size() > 0) {
                                Object obj = serializer.decode(error.additionalData, tmap);
                                BeanUtils.copyProperties(e, obj);
                            }
                        }
                        throw e;
                    }
                case PacketConstant.PACKET_TYPE_OK:
                    OKPacket ok = new OKPacket();
                    ok.init(bts);
                    packet = ok;
                    return null;
                case PacketConstant.PACKET_TYPE_SERVICE_RESPONSE:
                    ServiceResponsePacket response = new SerializeServiceResponsePacket(serializer, method.getGenericReturnType());
                    response.init(bts);
                    packet = response;
                    return response.result;
                default: {
                    logger.warn("unknow response type=" + type);
                    success = false;
                    return null;
                }
            }
        } catch (Exception e) {
            success = false;

            if (e instanceof CodedException || (errorCode = venusExceptionFactory.getErrorCode(e.getClass())) != 0 || e instanceof RuntimeException) {
                if(e instanceof CodedException){
                    errorCode = ((CodedException) e).getErrorCode();
                }
                throw e;
            } else {
                RemoteException code = e.getClass().getAnnotation(RemoteException.class);
                if(code != null){
                    errorCode = code.errorCode();
                    throw e;
                }else{
                    ExceptionCode eCode =   e.getClass().getAnnotation(ExceptionCode.class);
                    if(eCode != null){
                        errorCode = eCode.errorCode();
                        throw e;
                    }else{
                        errorCode = -1;
                        if (conn == null) {
                            throw new DefaultVenusException(e.getMessage(), e);
                        } else {
                            throw new DefaultVenusException(e.getMessage() + ". remoteAddress=" + conn.getRemoteAddress(), e);
                        }
                    }
                }
            }
        } finally {
            if (service.athenaFlag()) {
                AthenaTransactionDelegate.getDelegate().completeClientTransaction();
            }
            long end = TimeUtil.currentTimeMillis();
            long time = end - borrowed;
            StringBuffer buffer = new StringBuffer();
            buffer.append("[").append(borrowed - start).append(",").append(time).append("]ms (client-sync) traceID=").append(UUID.toString(traceID)).append(", api=").append(serviceRequestPacket.apiName);
            if (remoteAddress != null) {
                buffer.append(", remote=").append(remoteAddress);
            }else{
                buffer.append(", pool=").append(bioConnPool.toString());
            }
            buffer.append(", clientID=").append(PacketConstant.VENUS_CLIENT_ID).append(", requestID=").append(serviceRequestPacket.clientRequestId);

            if(packet != null){
                if(packet instanceof ErrorPacket){
                    buffer.append(", errorCode=").append(((ErrorPacket) packet).errorCode);
                    buffer.append(", message=").append(((ErrorPacket) packet).message);
                }else {
                    buffer.append(", errorCode=0");
                }
            }

            PerformanceLevel pLevel = AnnotationUtil.getAnnotation(method.getAnnotations(), PerformanceLevel.class);
            if (pLevel != null) {

                if (pLevel.printParams()) {
                    buffer.append(", params=");
                    buffer.append(JSON.toJSONString(serviceRequestPacket.parameterMap,JSON_FEATURE));
                }

                if (time > pLevel.error() && pLevel.error() > 0) {
                    if (performanceLogger.isErrorEnabled()) {
                        performanceLogger.error(buffer.toString());
                    }
                } else if (time > pLevel.warn() && pLevel.warn() > 0) {
                    if (performanceLogger.isWarnEnabled()) {
                        performanceLogger.warn(buffer.toString());
                    }
                } else if (time > pLevel.info() && pLevel.info() > 0) {
                    if (performanceLogger.isInfoEnabled()) {
                        performanceLogger.info(buffer.toString());
                    }
                } else {
                    if (performanceLogger.isDebugEnabled()) {
                        performanceLogger.debug(buffer.toString());
                    }
                }
            } else {
                buffer.append(", params=");
                buffer.append(JSON.toJSONString(serviceRequestPacket.parameterMap,JSON_FEATURE));

                if (time >= 30 * 1000) {
                    if (performanceLogger.isErrorEnabled()) {
                        performanceLogger.error(buffer.toString());
                    }
                } else if (time >= 10 * 1000) {
                    if (performanceLogger.isWarnEnabled()) {
                        performanceLogger.warn(buffer.toString());
                    }
                } else if (time >= 5 * 1000) {
                    if (performanceLogger.isInfoEnabled()) {
                        performanceLogger.info(buffer.toString());
                    }
                } else {
                    if (performanceLogger.isDebugEnabled()) {
                        performanceLogger.debug(buffer.toString());
                    }
                }
            }

            if (conn != null && bioConnPool != null && !invalided) {
                if (!conn.isClosed() && soTimeout > 0) {
                    conn.setSoTimeout(oldTimeout);
                }
                bioConnPool.returnObject(conn);
            }

        }
    }

    /**
     * 根据远程配置获取bio连接池
     * @return
     * @throws Exception
     */
    public ObjectPool getBioConnPool() throws Exception {
        //从本地内存查找，若不存在则创建
        String ipAddressList = remoteConfig.getFactory().getIpAddressList();
        if(bioPoolMap.get(ipAddressList) != null){
            return bioPoolMap.get(ipAddressList);
        }

        ObjectPool objectPool = createBioPool(remoteConfig, realPoolMap);
        bioPoolMap.put(remoteConfig.getFactory().getIpAddressList(),objectPool);
        return objectPool;
    }

    /**
     * 创建bio连接池
     * @param remoteConfig
     * @param realPools
     * @return
     * @throws Exception
     */
    private ObjectPool createBioPool(RemoteConfig remoteConfig, Map<String, Object> realPools) throws Exception {
        FactoryConfig factoryConfig = remoteConfig.getFactory();
        if (factoryConfig == null) {
            throw new ConfigurationException(remoteConfig.getName() + " factory cannot be null");
        }
        String ipAddress = factoryConfig.getIpAddressList();
        if(StringUtils.isEmpty(ipAddress)) {
            throw new IllegalArgumentException("remtoe=" + remoteConfig.getName() + ", ipaddress cannot be null");
        }
        PoolConfig poolConfig = remoteConfig.getPool();
        //TODO 地址合法性校验
        String ipList[] = StringUtil.split(ipAddress, ", ");
        PoolableObjectPool bioPools[] = new PoolableObjectPool[ipList.length];

        for (int i = 0; i < ipList.length; i++) {
            String shareName = remoteConfig.isShare() ? "SHARED-" : "";
            if (remoteConfig.isShare()) {
                bioPools[i] = (PoolableObjectPool) realPools.get("B-" + shareName + ipList[i]);
            }

            //TODO 处理bio/nio/client/server连接复用问题
            VenusBIOConnectionFactory bioFactory = new VenusBIOConnectionFactory();
            if (remoteConfig.getAuthenticator() != null) {
                bioFactory.setAuthenticator(remoteConfig.getAuthenticator());
            }

            bioPools[i] = new PoolableObjectPool();
            if (poolConfig != null) {
                BeanUtils.copyProperties(bioPools[i], poolConfig);
            } else {
                bioPools[i].setTestOnBorrow(true);
                bioPools[i].setTestWhileIdle(true);
            }
            if (remoteConfig.getAuthenticator() != null) {
                bioFactory.setAuthenticator(remoteConfig.getAuthenticator());
            }
            bioFactory.setNeedPing(needPing);
            if (factoryConfig != null) {
                BeanUtils.copyProperties(bioFactory, factoryConfig);
            }

            String temp[] = StringUtil.split(ipList[i], ":");
            if (temp.length > 1) {
                bioFactory.setHost(temp[0]);
                bioFactory.setPort(Integer.valueOf(temp[1]));
            } else {
                bioFactory.setHost(temp[0]);
                bioFactory.setPort(16800);
            }

            bioPools[i].setName("B-" + shareName + bioFactory.getHost() + ":" + bioFactory.getPort());
            bioPools[i].setFactory(bioFactory);
            bioPools[i].init();
            realPools.put(bioPools[i].getName(), bioPools[i]);
        }//end for

        if (ipList.length > 1) {
            MultipleLoadBalanceObjectPool multipleLoadBalanceObjectPool = new MultipleLoadBalanceObjectPool(remoteConfig.getLoadbalance(), bioPools);
            multipleLoadBalanceObjectPool.setName("B-V-" + remoteConfig.getName());

            multipleLoadBalanceObjectPool.init();

            realPools.put(multipleLoadBalanceObjectPool.getName(), multipleLoadBalanceObjectPool);
            return multipleLoadBalanceObjectPool;
        } else {
            return bioPools[0];
        }
    }


    /**
     * 根据远程配置获取nio连接池
     * @return
     * @throws Exception
     */
    public BackendConnectionPool getNioConnPool() throws Exception {
        String ipAddressList = remoteConfig.getFactory().getIpAddressList();
        if(nioPoolMap.get(ipAddressList) != null){
            return nioPoolMap.get(ipAddressList);
        }

        BackendConnectionPool backendConnectionPool = createNioPool(remoteConfig, realPoolMap);
        nioPoolMap.put(remoteConfig.getFactory().getIpAddressList(),backendConnectionPool);
        return backendConnectionPool;
    }

    /**
     * 创建nio连接池
     * @param remoteConfig
     * @param realPools
     * @return
     * @throws Exception
     */
    private BackendConnectionPool createNioPool(RemoteConfig remoteConfig, Map<String, Object> realPools) throws Exception {
        //RemoteContainer container = new RemoteContainer();
        FactoryConfig factoryConfig = remoteConfig.getFactory();
        if (factoryConfig == null) {
            throw new ConfigurationException(remoteConfig.getName() + " factory cannot be null");
        }
        String ipAddress = factoryConfig.getIpAddressList();
        if(StringUtils.isEmpty(ipAddress)) {
            throw new IllegalArgumentException("remtoe=" + remoteConfig.getName() + ", ipaddress cannot be null");
        }
        PoolConfig poolConfig = remoteConfig.getPool();
        //TODO 地址合法性校验
        String ipList[] = StringUtil.split(ipAddress, ", ");
        BackendConnectionPool nioPools[] = new BackendConnectionPool[ipList.length];

        for (int i = 0; i < ipList.length; i++) {
            String shareName = remoteConfig.isShare() ? "SHARED-" : "";
            if (remoteConfig.isShare()) {
                nioPools[i] = (PollingBackendConnectionPool) realPools.get("N-" + shareName + ipList[i]);
                if (nioPools[i] != null) {
                    continue;
                }
            }

            VenusBackendConnectionFactory nioFactory = new VenusBackendConnectionFactory();

            nioPools[i] = new PollingBackendConnectionPool("N-" + shareName + ipList[i], nioFactory, 8);
            if (poolConfig != null) {
                BeanUtils.copyProperties(nioPools[i], poolConfig);
            }
            if (remoteConfig.getAuthenticator() != null) {
                nioFactory.setAuthenticator(remoteConfig.getAuthenticator());
            }
            if (factoryConfig != null) {
                BeanUtils.copyProperties(nioFactory, factoryConfig);
            }

            String temp[] = StringUtil.split(ipList[i], ":");
            if (temp.length > 1) {
                nioFactory.setHost(temp[0]);
                nioFactory.setPort(Integer.valueOf(temp[1]));
            } else {
                nioFactory.setHost(temp[0]);
                nioFactory.setPort(16800);
            }

            if (this.isEnableAsync()) {
                nioFactory.setConnector(this.connector);
                nioFactory.setMessageHandler(handler);
                // nioPools[i].setName("n-connPool-"+nioFactory.getIpAddress());
                nioPools[i].init();
                realPools.put(nioPools[i].getName(), nioPools[i]);
            }
        }//end for

        if (ipList.length > 1) {
            MultipleLoadBalanceBackendConnectionPool multipleLoadBalanceBackendConnectionPool = new MultipleLoadBalanceBackendConnectionPool(remoteConfig.getName(), remoteConfig.getLoadbalance(),
                    nioPools);
            multipleLoadBalanceBackendConnectionPool.init();

            realPools.put(multipleLoadBalanceBackendConnectionPool.getName(), multipleLoadBalanceBackendConnectionPool);
            return multipleLoadBalanceBackendConnectionPool;
        } else {
            return nioPools[0];
        }
    }

    /**
     * 设置transactionId
     * @param serviceRequestPacket
     * @param athenaTransactionId
     */
    private void setTransactionId(SerializeServiceRequestPacket serviceRequestPacket, AthenaTransactionId athenaTransactionId) {
        if (athenaTransactionId != null) {
            if (athenaTransactionId.getRootId() != null) {
                serviceRequestPacket.rootId = athenaTransactionId.getRootId().getBytes();
            }

            if (athenaTransactionId.getParentId() != null) {
                serviceRequestPacket.parentId = athenaTransactionId.getParentId().getBytes();
            }

            if (athenaTransactionId.getMessageId() != null) {
                serviceRequestPacket.messageId = athenaTransactionId.getMessageId().getBytes();
            }
        }
    }


    public boolean isEnableAsync() {
        return enableAsync;
    }

    public void setEnableAsync(boolean enableAsync) {
        this.enableAsync = enableAsync;
    }

    public VenusExceptionFactory getVenusExceptionFactory() {
        return venusExceptionFactory;
    }

    public void setVenusExceptionFactory(VenusExceptionFactory venusExceptionFactory) {
        this.venusExceptionFactory = venusExceptionFactory;
    }

    public void setServiceFactory(XmlServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
    }

    public short getSerializeType() {
        return serializeType;
    }

    public void setSerializeType(byte serializeType) {
        this.serializeType = serializeType;
    }

    public InvocationListenerContainer getContainer() {
        return container;
    }

    public void setContainer(InvocationListenerContainer container) {
        this.container = container;
    }

    public RemoteConfig getRemoteConfig() {
        return remoteConfig;
    }

    public void setRemoteConfig(RemoteConfig remoteConfig) {
        this.remoteConfig = remoteConfig;
    }

    public VenusNIOMessageHandler getHandler() {
        return handler;
    }

    public void setHandler(VenusNIOMessageHandler handler) {
        this.handler = handler;
    }

    public ConnectionConnector getConnector() {
        return connector;
    }

    public void setConnector(ConnectionConnector connector) {
        this.connector = connector;
    }
}
