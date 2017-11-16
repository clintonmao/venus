package com.meidusa.venus.client.invoker.venus;

import com.meidusa.toolkit.net.*;
import com.meidusa.toolkit.util.TimeUtil;
import com.meidusa.venus.*;
import com.meidusa.venus.client.factory.xml.config.ClientRemoteConfig;
import com.meidusa.venus.client.factory.xml.config.FactoryConfig;
import com.meidusa.venus.client.factory.xml.config.PoolConfig;
import com.meidusa.venus.client.invoker.AbstractClientInvoker;
import com.meidusa.venus.exception.InvalidParameterException;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.io.network.VenusBackendConnectionFactory;
import com.meidusa.venus.io.packet.PacketConstant;
import com.meidusa.venus.io.packet.ServicePacketBuffer;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;
import com.meidusa.venus.io.serializer.Serializer;
import com.meidusa.venus.io.serializer.SerializerFactory;
import com.meidusa.venus.metainfo.EndpointParameter;
import com.meidusa.venus.notify.InvocationListener;
import com.meidusa.venus.notify.ReferenceInvocationListener;
import com.meidusa.venus.support.EndpointWrapper;
import com.meidusa.venus.support.ServiceWrapper;
import com.meidusa.venus.support.VenusThreadContext;
import com.meidusa.venus.support.VenusUtil;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * venus协议服务调用实现
 * Created by Zhangzhihua on 2017/7/31.
 */
public class VenusClientInvoker extends AbstractClientInvoker implements Invoker{

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger tracerLogger = VenusLoggerFactory.getTracerLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    private byte serializeType = PacketConstant.CONTENT_TYPE_JSON;

    /**
     * 远程连接配置，包含ip相关信息
     */
    private ClientRemoteConfig remoteConfig;

    //rpcId-请求&响应映射表
    private Map<String, VenusReqRespWrapper> serviceReqRespMap = new ConcurrentHashMap<String, VenusReqRespWrapper>();

    //rpcId-请求&回调映射表
    private Map<String, ClientInvocation> serviceReqCallbackMap = new ConcurrentHashMap<String, ClientInvocation>();

    //NIO消息响应处理
    private VenusClientInvokerMessageHandler messageHandler = new VenusClientInvokerMessageHandler();

    //nio连接映射表
    private Map<String, BackendConnectionPool> connectionPoolMap = new ConcurrentHashMap<String, BackendConnectionPool>();

    //连接事件监听
    private VenusClientConnectionObserver connectionObserver = new VenusClientConnectionObserver();

    private static ConnectionConnector connector;

    private static ConnectionManager[] connectionManagers;

    private boolean isInit = false;

    public VenusClientInvoker(){
        synchronized (this){
            //添加invoker资源
            Application.addInvoker(this);

            //构造连接
            if(connector == null && connectionManagers == null){
                try {
                    if(logger.isInfoEnabled()){
                        logger.info("#########init connector#############");
                    }
                    connector = new ConnectionConnector("connection connector-0");
                    int ioThreads = Runtime.getRuntime().availableProcessors();
                    connectionManagers = new ConnectionManager[ioThreads];
                    for(int i=0;i<ioThreads;i++){
                        ConnectionManager connManager = new ConnectionManager("connection manager-" + i, -1);
                        //添加连接监听
                        connectionObserver.setServiceReqRespMap(serviceReqRespMap);
                        connManager.addConnectionObserver(connectionObserver);
                        connectionManagers[i] = connManager;
                        connManager.start();
                    }
                    connector.setProcessors(connectionManagers);
                    connector.start();
                } catch (IOException e) {
                    throw new RpcException(e);
                }
            }

        }
    }


    @Override
    public void init() throws RpcException {
        if(!isInit){
            messageHandler.setServiceReqCallbackMap(serviceReqCallbackMap);
            messageHandler.setServiceReqRespMap(serviceReqRespMap);
            isInit = true;
        }

    }

    @Override
    public Result doInvoke(ClientInvocation invocation, URL url) throws RpcException {
        if(!isCallbackInvocation(invocation)){
            return doInvokeWithSync(invocation, url);
        }else{
            return doInvokeWithCallback(invocation, url);
        }
    }

    /**
     * 判断是否callback异步调用
     * @param invocation
     * @return
     */
    boolean isCallbackInvocation(ClientInvocation invocation){
        EndpointParameter[] params = invocation.getParams();
        if (params != null) {
            Object[] args = invocation.getArgs();
            for (int i = 0; i < params.length; i++) {
                if (args[i] instanceof InvocationListener) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * sync同步调用
     * @param invocation
     * @param url
     * @return
     * @throws Exception
     */
    public Result doInvokeWithSync(ClientInvocation invocation, URL url) throws RpcException {
        Result result = null;

        long bWaitTime = System.currentTimeMillis();
        int timeout = invocation.getTimeout();

        //构造请求消息
        SerializeServiceRequestPacket request = buildRequest(invocation);

        //添加rpcId -> reqResp映射表
        String rpcId = invocation.getRpcId();
        VenusReqRespWrapper reqRespWrapper = new VenusReqRespWrapper(invocation);
        serviceReqRespMap.put(rpcId,reqRespWrapper);

        //发送消息
        sendRequest(invocation, request, url,reqRespWrapper);

        //latch阻塞等待
        boolean isAwaitException = false;
        try {
            reqRespWrapper.getReqRespLatch().await(timeout,TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            isAwaitException = true;
            throw new RpcException(e);
        }finally {
            if(isAwaitException){
                if(serviceReqRespMap.get(rpcId) != null){
                    serviceReqRespMap.remove(rpcId);
                }
            }
        }

        //处理响应
        result = fetchResponse(rpcId);

        if(result == null){
            throw new RpcException(RpcException.TIMEOUT_EXCEPTION,String.format("invoke service:%s,timeout:%dms",url.getPath(),timeout));
        }
        return result;
    }

    /**
     * callback异步调用
     * @param invocation
     * @param url
     * @return
     * @throws Exception
     */
    public Result doInvokeWithCallback(ClientInvocation invocation, URL url) throws RpcException {
        //构造请求消息
        SerializeServiceRequestPacket request = buildRequest(invocation);

        //添加rpcId-> reqResp映射表
        serviceReqCallbackMap.put(invocation.getRpcId(),invocation);

        //发送消息
        sendRequest(invocation, request, url,null);

        //立即返回，响应由invocationListener处理
        return new Result(null);
    }

    /**
     * 构造请求消息
     * @param invocation
     * @return
     */
    SerializeServiceRequestPacket buildRequest(ClientInvocation invocation){
        Method method = invocation.getMethod();
        ServiceWrapper service = invocation.getService();
        EndpointWrapper endpoint = invocation.getEndpoint();
        EndpointParameter[] params = invocation.getParams();
        Object[] args = invocation.getArgs();

        //构造请求报文
        Serializer serializer = SerializerFactory.getSerializer(serializeType);
        SerializeServiceRequestPacket serviceRequestPacket = new SerializeServiceRequestPacket(serializer, null);
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
        serviceRequestPacket.apiName = VenusUtil.getApiName(method,service,endpoint);//VenusAnnotationUtils.getApiname(method, service, endpoint);
        serviceRequestPacket.serviceVersion = service.getVersion();
        serviceRequestPacket.parameterMap = new HashMap<String, Object>();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if (args[i] instanceof InvocationListener) {
                    ReferenceInvocationListener listener = new ReferenceInvocationListener();
                    ServicePacketBuffer buffer = new ServicePacketBuffer(16);
                    buffer.writeLengthCodedString(args[i].getClass().getName(), "utf-8");
                    buffer.writeInt(System.identityHashCode(args[i]));
                    listener.setIdentityData(buffer.toByteBuffer().array());
                    Type type = method.getGenericParameterTypes()[i];
                    if (type instanceof ParameterizedType) {
                        ParameterizedType genericType = ((ParameterizedType) type);
                        //container.putInvocationListener((InvocationListener) args[i], genericType.getActualTypeArguments()[0]);
                        invocation.setInvocationListener((InvocationListener)args[i]);
                        invocation.setType(genericType.getActualTypeArguments()[0]);
                    } else {
                        throw new InvalidParameterException("invocationListener is not generic");
                    }
                    serviceRequestPacket.parameterMap.put(params[i].getParamName(), listener);
                } else {
                    serviceRequestPacket.parameterMap.put(params[i].getParamName(), args[i]);
                }

            }
        }
        return serviceRequestPacket;
    }

    /**
     * 发送远程调用消息
     * @param invocation
     * @param serviceRequestPacket
     * @param url 目标地址
     * @return
     * @throws Exception
     */
    void sendRequest(ClientInvocation invocation, SerializeServiceRequestPacket serviceRequestPacket, URL url,VenusReqRespWrapper reqRespWrapper) throws RpcException{
        long start = TimeUtil.currentTimeMillis();
        long borrowed = start;
        BackendConnectionPool nioConnPool = null;
        BackendConnection conn = null;
        String rpcId = invocation.getRpcId();
        Throwable exception = null;
        try {
            //获取连接
            nioConnPool = getNioConnPool(url,invocation,null);
            conn = nioConnPool.borrowObject();
            if(!conn.isActive()){
                throw new RpcException(RpcException.NETWORK_EXCEPTION,"get connetion failed.");
            }
            borrowed = System.currentTimeMillis();
            if(reqRespWrapper != null){
                reqRespWrapper.setBackendConnection(conn);
            }

            //发送请求消息，响应由handler类处理
            ByteBuffer buffer = serviceRequestPacket.toByteBuffer();
            VenusThreadContext.set(VenusThreadContext.CLIENT_OUTPUT_SIZE,Integer.valueOf(buffer.limit()));

            conn.write(buffer);
        } catch (RpcException e){
            exception = e;
            throw e;
        }catch (Throwable e){
            exception = e;
            throw new RpcException(e);
        }finally {
            //返连接
            if (conn != null && nioConnPool != null) {
                nioConnPool.returnObject(conn);
            }

            //打印trace logger
            long connTime = borrowed - start;
            long totalTime = System.currentTimeMillis() - start;
            if(exception != null){
                //输出异常日志
                if (tracerLogger.isErrorEnabled()) {
                    String tpl = "send request failed,rpcId:{},method:{},targetIp:{},used time:{},exception:{}.";
                    Object[] arguments = new Object[]{
                            rpcId,
                            invocation.getMethodPath(),
                            url.getHost(),
                            "[" + totalTime + "," + connTime + "]",
                            exception
                    };
                    //错误日志
                    exceptionLogger.error(tpl,arguments);
                    tracerLogger.error(tpl,arguments);
                }
            }else{
                if(tracerLogger.isInfoEnabled()){
                    String tpl = "send request,rpcId:{},method:{},targetIp:{},used time:{}ms.";
                    Object[] arguments = new Object[]{
                            rpcId,
                            invocation.getMethodPath(),
                            url.getHost(),
                            "[" + totalTime + "," + connTime + "]"
                    };
                    tracerLogger.info(tpl,arguments);
                }
            }
        }
    }



    /**
     * 根据远程配置获取nio连接池
     * @return
     * @throws Exception
     * @param url
     */
    public BackendConnectionPool getNioConnPool(URL url,ClientInvocation invocation,ClientRemoteConfig remoteConfig){
        String address = new StringBuilder()
                .append(url.getHost())
                .append(":")
                .append(url.getPort())
                .toString();
        //若存在，则直接使用连接池
        if(connectionPoolMap.get(address) != null){
            return connectionPoolMap.get(address);
        }else{
            //若不存在，则创建连接池
            synchronized (this){
                BackendConnectionPool backendConnectionPool = null;
                if(connectionPoolMap.get(address) != null){
                    backendConnectionPool = connectionPoolMap.get(address);
                }else{
                    backendConnectionPool = createNioPool(url,invocation,new ClientRemoteConfig());
                    connectionPoolMap.put(address,backendConnectionPool);
                }
                return backendConnectionPool;
            }
        }
    }

    /**
     * 创建连接池
     * @param url
     * @param remoteConfig
     * @return
     * @throws Exception
     */
    private BackendConnectionPool createNioPool(URL url,ClientInvocation invocation,ClientRemoteConfig remoteConfig){
        if(logger.isInfoEnabled()){
            logger.info("#########create nio pool#############:{}.",url);
        }
        //初始化连接工厂
        VenusBackendConnectionFactory nioFactory = new VenusBackendConnectionFactory();
        nioFactory.setHost(url.getHost());
        nioFactory.setPort(Integer.valueOf(url.getPort()));
        if (remoteConfig.getAuthenticator() != null) {
            nioFactory.setAuthenticator(remoteConfig.getAuthenticator());
        }
        FactoryConfig factoryConfig = remoteConfig.getFactory();
        if (factoryConfig != null) {
            //BeanUtils.copyProperties(nioFactory, factoryConfig);
        }
        nioFactory.setConnector(connector);
        nioFactory.setMessageHandler(messageHandler);
        //nioFactory.setSendBufferSize(2);
        //nioFactory.setReceiveBufferSize(4);
        //nioFactory.setWriteQueueCapcity(16);

        //初始化连接池
        int connectionCount = invocation.getCoreConnections();
        BackendConnectionPool nioPool = new PollingBackendConnectionPool("N-" + url.getHost(), nioFactory, connectionCount);
        PoolConfig poolConfig = remoteConfig.getPool();
        if (poolConfig != null) {
            //BeanUtils.copyProperties(nioPool, poolConfig);
        }
        nioPool.init();
        //若连接池初始化失败，则释放连接池（fix 此时心跳检测已启动）
        boolean isValid = nioPool.isValid();
        if(!isValid){
            boolean isClosed = nioPool.isClosed();
            if(!isClosed){
                if(logger.isWarnEnabled()){
                    logger.warn("connection pool is invalid,close connection pool.");
                }
                try {
                    nioPool.close();
                } catch (Exception e) {
                    //捕获关闭异常，避免影响处理流程
                    if(logger.isErrorEnabled()){
                        logger.error("close invalid connection pool error.");
                    }
                }
            }
            throw new RpcException(RpcException.NETWORK_EXCEPTION,"create connection pool failed.");
        }
        return nioPool;
    }



    /**
     * 获取对应请求的响应结果
     * @param rpcId
     * @return
     */
    Result fetchResponse(String rpcId){
        VenusReqRespWrapper reqRespWrapper = serviceReqRespMap.get(rpcId);
        if(reqRespWrapper == null){
            return null;
        }

        Result result = reqRespWrapper.getResult();
        if(result == null){
            serviceReqRespMap.remove(rpcId);
            return null;
        }else {
            //删除映射数据
            serviceReqRespMap.remove(rpcId);
            return result;
        }
    }

    public short getSerializeType() {
        return serializeType;
    }

    public void setSerializeType(byte serializeType) {
        this.serializeType = serializeType;
    }

    public ClientRemoteConfig getRemoteConfig() {
        return remoteConfig;
    }

    public void setRemoteConfig(ClientRemoteConfig remoteConfig) {
        this.remoteConfig = remoteConfig;
    }

    public VenusClientInvokerMessageHandler getMessageHandler() {
        return messageHandler;
    }

    public void setMessageHandler(VenusClientInvokerMessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    @Override
    public void destroy() throws RpcException{
        if(logger.isInfoEnabled()){
            logger.info("destroy invoker:{}.",this);
        }
        //释放连接
        if (connector != null) {
            if (connector.isAlive()) {
                connector.shutdown();
            }
        }

        if(connectionManagers != null && connectionManagers.length > 0){
            for(ConnectionManager connManager:connectionManagers){
                if (connManager != null) {
                    if (connManager.isAlive()) {
                        connManager.shutdown();
                    }
                }
            }
        }
    }


}
