package com.meidusa.venus.client.invoker.venus;

import com.meidusa.fastjson.JSON;
import com.meidusa.fastmark.feature.SerializerFeature;
import com.meidusa.toolkit.net.*;
import com.meidusa.toolkit.util.TimeUtil;
import com.meidusa.venus.ClientInvocation;
import com.meidusa.venus.Invoker;
import com.meidusa.venus.Result;
import com.meidusa.venus.URL;
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
import com.meidusa.venus.support.*;
import com.meidusa.venus.util.UUID;
import com.meidusa.venus.util.VenusTracerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * venus协议服务调用实现
 * Created by Zhangzhihua on 2017/7/31.
 */
public class VenusClientInvoker extends AbstractClientInvoker implements Invoker{

    private static Logger logger = LoggerFactory.getLogger(VenusClientInvoker.class);

    private static Logger performanceLogger = LoggerFactory.getLogger("venus.client.performance");

    private static SerializerFeature[] JSON_FEATURE = new SerializerFeature[]{SerializerFeature.ShortString,SerializerFeature.IgnoreNonFieldGetter,SerializerFeature.SkipTransientField};

    private byte serializeType = PacketConstant.CONTENT_TYPE_JSON;

    /**
     * 远程连接配置，包含ip相关信息
     */
    private ClientRemoteConfig remoteConfig;

    //nio连接映射表
    private Map<String, BackendConnectionPool> nioPoolMap = new ConcurrentHashMap<String, BackendConnectionPool>();

    //rpcId-请求&响应映射表
    private Map<String, VenusReqRespWrapper> serviceReqRespMap = new ConcurrentHashMap<String, VenusReqRespWrapper>();

    //rpcId-请求&回调映射表
    private Map<String, ClientInvocation> serviceReqCallbackMap = new ConcurrentHashMap<String, ClientInvocation>();

    //NIO消息响应处理
    private VenusClientInvokerMessageHandler messageHandler = new VenusClientInvokerMessageHandler();

    //添加连接事件监听
    private VenusClientConnectionObserver connectionObserver = new VenusClientConnectionObserver();

    private static ConnectionConnector connector;

    private static ConnectionManager[] connectionManagers;

    //invoker列表
    private static List<Invoker> invokerList = new ArrayList<Invoker>();

    private boolean isInit = false;

    private static boolean isEnableRandomPrint = false;

    //mock返回线程池
    private static Executor mockReturnExecutor = null;

    public VenusClientInvoker(){
        synchronized (this){
            //构造连接
            if(connector == null && connectionManagers == null){
                try {
                    if(logger.isInfoEnabled()){
                        logger.info("###################init connector############");
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

            //添加到invoker列表，用于释放资源
            getInvokerList().add(this);
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

        if(isEnableRandomPrint){
            if(ThreadLocalRandom.current().nextInt(100000) > 99995){
                if(logger.isInfoEnabled()){
                    logger.info("build,send->fecth cost time:{}.",System.currentTimeMillis()-bWaitTime);
                }
            }
        }
        return result;
    }

    /**
     * mock接收处理线程
     */
    class MockReturnProcess implements Runnable{

        VenusReqRespWrapper reqRespWrapper;

        public MockReturnProcess(VenusReqRespWrapper reqRespWrapper){
            this.reqRespWrapper = reqRespWrapper;
        }

        @Override
        public void run() {
            try {
                reqRespWrapper.setResult(null);
            } finally {
                reqRespWrapper.getReqRespLatch().countDown();
            }
        }
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
        byte[] traceID = invocation.getTraceID();
        BackendConnectionPool nioConnPool = null;
        BackendConnection conn = null;
        try {
            //获取连接
            //TODO 心跳处理确认
            //TODO 失败重连确认
            //TODO 地址变化对连接的影响；地址未变但连接已断开其影响
            nioConnPool = getNioConnPool(url,invocation,null);
            conn = nioConnPool.borrowObject();
            if(!conn.isActive()){
                throw new RpcException(RpcException.NETWORK_EXCEPTION,"connetion not active.");
            }
            borrowed = System.currentTimeMillis();
            if(reqRespWrapper != null){
                reqRespWrapper.setBackendConnection(conn);
            }

            //发送请求消息，响应由handler类处理
            ByteBuffer buffer = serviceRequestPacket.toByteBuffer();
            VenusThreadContext.set(VenusThreadContext.CLIENT_OUTPUT_SIZE,Integer.valueOf(buffer.limit()));

            conn.write(buffer);
            VenusTracerUtil.logRequest(traceID, serviceRequestPacket.apiName, JSON.toJSONString(serviceRequestPacket.parameterMap,JSON_FEATURE));
        } catch (RpcException e){
            throw e;
        }catch (Throwable e){
            throw new RpcException(e);
        }finally {
            if (performanceLogger.isDebugEnabled()) {
                long end = TimeUtil.currentTimeMillis();
                long time = end - borrowed;
                StringBuilder buffer = new StringBuilder();
                buffer.append("[").append(borrowed - start).append(",").append(time).append("]ms (client-callback) traceID=").append(UUID.toString(traceID)).append(", api=").append(serviceRequestPacket.apiName);

                performanceLogger.debug(buffer.toString());
            }

            if (conn != null && nioConnPool != null) {
                nioConnPool.returnObject(conn);
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
        if(nioPoolMap.get(address) != null){
            return nioPoolMap.get(address);
        }else{
            //若不存在，则创建连接池
            synchronized (this){
                BackendConnectionPool backendConnectionPool = null;
                if(nioPoolMap.get(address) != null){
                    backendConnectionPool = nioPoolMap.get(address);
                }else{
                    backendConnectionPool = createNioPool(url,invocation,new ClientRemoteConfig());
                    nioPoolMap.put(address,backendConnectionPool);
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

        //初始化连接池 TODO 连接数双倍问题
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
                logger.warn("connection pool is invalid,close connection pool.");
                try {
                    nioPool.close();
                } catch (Exception e) {
                    //捕获关闭异常，避免影响处理流程
                    logger.error("close invalid connection pool error.");
                }
            }
            throw new RpcException(RpcException.NETWORK_EXCEPTION,"init connection pool failed.");
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


    public static List<Invoker> getInvokerList() {
        return invokerList;
    }

    void addInvoker(Invoker invoker){
        if(invoker != null){
            getInvokerList().add(invoker);
        }
    }

}
