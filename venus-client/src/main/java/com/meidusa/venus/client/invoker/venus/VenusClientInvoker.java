package com.meidusa.venus.client.invoker.venus;

import com.meidusa.fastmark.feature.SerializerFeature;
import com.meidusa.toolkit.net.*;
import com.meidusa.toolkit.util.TimeUtil;
import com.meidusa.venus.*;
import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.annotations.Service;
import com.meidusa.venus.ClientInvocation;
import com.meidusa.venus.client.factory.xml.XmlServiceFactory;
import com.meidusa.venus.client.factory.xml.config.*;
import com.meidusa.venus.client.invoker.AbstractClientInvoker;
import com.meidusa.venus.exception.InvalidParameterException;
import com.meidusa.venus.exception.VenusExceptionFactory;
import com.meidusa.venus.io.utils.RpcIdUtil;
import com.meidusa.venus.monitor.athena.reporter.AthenaTransactionId;
import com.meidusa.venus.io.network.AbstractBIOConnection;
import com.meidusa.venus.io.network.VenusBackendConnectionFactory;
import com.meidusa.venus.io.packet.*;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;
import com.meidusa.venus.io.serializer.Serializer;
import com.meidusa.venus.io.serializer.SerializerFactory;
import com.meidusa.venus.metainfo.EndpointParameter;
import com.meidusa.venus.notify.InvocationListener;
import com.meidusa.venus.notify.ReferenceInvocationListener;
import com.meidusa.venus.util.JSONUtil;
import com.meidusa.venus.util.VenusAnnotationUtils;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * venus协议服务调用实现
 * Created by Zhangzhihua on 2017/7/31.
 */
public class VenusClientInvoker extends AbstractClientInvoker implements Invoker{

    private static Logger logger = LoggerFactory.getLogger(VenusClientInvoker.class);

    private static Logger performanceLogger = LoggerFactory.getLogger("venus.client.performance");

    private static SerializerFeature[] JSON_FEATURE = new SerializerFeature[]{SerializerFeature.ShortString,SerializerFeature.IgnoreNonFieldGetter,SerializerFeature.SkipTransientField};

    private byte serializeType = PacketConstant.CONTENT_TYPE_JSON;

    private static AtomicLong sequenceId = new AtomicLong(1);

    /**
     * 远程连接配置，包含ip相关信息
     */
    private ClientRemoteConfig remoteConfig;

    private VenusExceptionFactory venusExceptionFactory;

    private XmlServiceFactory serviceFactory;

    private boolean enableAsync = true;

    private boolean needPing = false;

    private int asyncExecutorSize = 10;

    private static ConnectionConnector connector;

    private static ConnectionManager connManager;

    /**
     * nio连接映射表
     */
    private Map<String, BackendConnectionPool> nioPoolMap = new HashMap<String, BackendConnectionPool>();

    //TODO 优化锁对象
    private Object lock = new Object();

    /**
     * rpcId-请求映射表 TODO 完成、异常清理问题；及监控大小问题
     */
    private Map<String, ClientInvocation> serviceInvocationMap = new ConcurrentHashMap<String, ClientInvocation>();

    /**
     * rpcId-响应映射表 TODO 完成、异常清理问题；及监控大小问题
     */
    private Map<String,AbstractServicePacket> serviceResponseMap = new ConcurrentHashMap<String, AbstractServicePacket>();

    /**
     * 调用监听容器
     */
    private InvocationListenerContainer container = new InvocationListenerContainer();

    /**
     * NIO消息响应处理
     */
    private VenusClientInvokerMessageHandler messageHandler = new VenusClientInvokerMessageHandler();

    private static boolean isInitConnector = false;

    @Override
    public void init() throws RpcException {
        if(!isInitConnector){
            if (enableAsync) {//TODO 开启async意义？
                if (connector == null) {
                    try {
                        connector = new ConnectionConnector("connection Connector");
                    } catch (IOException e) {
                        throw new RpcException(e);
                    }
                    connector.setDaemon(true);

                }

                if (connManager == null) {
                    try {
                        connManager = new ConnectionManager("Connection Manager", this.getAsyncExecutorSize());
                    } catch (IOException e) {
                        throw new RpcException(e);
                    }
                    connManager.setDaemon(true);
                    connManager.start();
                }

                connector.setProcessors(new ConnectionManager[]{connManager});
                connector.start();
            }

            isInitConnector = true;
        }

        //TODO 实例化对应关系
        messageHandler.setVenusExceptionFactory(venusExceptionFactory);
        messageHandler.setContainer(this.container);
        messageHandler.setLock(lock);
        messageHandler.setServiceInvocationMap(serviceInvocationMap);
        messageHandler.setServiceResponseMap(serviceResponseMap);

    }

    @Override
    public Result doInvoke(ClientInvocation invocation, URL url) throws RpcException {
        try {
            if(!isCallbackInvocation(invocation)){
                return doInvokeWithSync(invocation, url);
            }else{
                return doInvokeWithCallback(invocation, url);
            }
        } catch (Exception e) {
            throw new RpcException(e);
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
    public Result doInvokeWithSync(ClientInvocation invocation, URL url) throws Exception {
        //构造请求消息
        SerializeServiceRequestPacket request = buildRequest(invocation);
        //添加rpcId -> invocation映射表
        serviceInvocationMap.put(RpcIdUtil.getRpcId(request),invocation);

        //发送消息
        sendRequest(invocation, request, url);

        //阻塞等待并处理响应结果
        int timeout = 15000;
        synchronized (lock){
            //logger.info("lock wait begin...");
            lock.wait(timeout);//TODO 超时时间
            //logger.info("lock wait end...");
        }

        Result result = fetchResponse(RpcIdUtil.getRpcId(request));
        //TODO 改为methodPath
        String servicePath = url.getPath();
        logger.info("fecth response,rpcId:{},response:{}.",RpcIdUtil.getRpcId(request),JSONUtil.toJSONString(result));
        if(result == null){
            throw new RpcException(String.format("invoke service:%s,timeout:%dms",servicePath,timeout));
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
    public Result doInvokeWithCallback(ClientInvocation invocation, URL url) throws Exception {
        //构造请求消息
        SerializeServiceRequestPacket request = buildRequest(invocation);
        //添加rpcId-> invocation映射表
        serviceInvocationMap.put(RpcIdUtil.getRpcId(request),invocation);

        //发送消息
        sendRequest(invocation, request, url);

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
        Service service = invocation.getService();
        Endpoint endpoint = invocation.getEndpoint();
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
        serviceRequestPacket.apiName = VenusAnnotationUtils.getApiname(method, service, endpoint);
        serviceRequestPacket.serviceVersion = service.version();
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
                        //TODO 兼容旧版本方案 改由invocation保存回调信息 是否允许多个listener?
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
     * @param serviceRequestPacket TODO 想办法合并invocation/request
     * @param url 目标地址
     * @return
     * @throws Exception
     */
    void sendRequest(ClientInvocation invocation, SerializeServiceRequestPacket serviceRequestPacket, URL url) throws Exception{
        byte[] traceID = invocation.getTraceID();
        Service service = invocation.getService();
        Endpoint endpoint = invocation.getEndpoint();

        long start = TimeUtil.currentTimeMillis();
        long borrowed = start;

        BackendConnectionPool nioConnPool = null;
        BackendConnection conn = null;
        try {
            //获取连接 TODO 地址变化情况
            nioConnPool = getNioConnPool(url,null);
            conn = nioConnPool.borrowObject();
            borrowed = TimeUtil.currentTimeMillis();
            ByteBuffer buffer = serviceRequestPacket.toByteBuffer();
            VenusThreadContext.set(VenusThreadContext.CLIENT_OUTPUT_SIZE,Integer.valueOf(buffer.limit()));

            //发送请求消息，响应由handler类处理
            conn.write(buffer);
            logger.info("send request,rpcId:{},buff len:{},message:{}.",RpcIdUtil.getRpcId(serviceRequestPacket), buffer.limit(),JSONUtil.toJSONString(serviceRequestPacket));
            /* TODO tracer log
            VenusTracerUtil.logRequest(traceID, serviceRequestPacket.apiName, JSON.toJSONString(serviceRequestPacket.parameterMap,JSON_FEATURE));
            */
        } catch (Exception e){
            logger.error("send request error.",e);
            throw e;
        }finally {
            /* TODO logger
            if (performanceLogger.isDebugEnabled()) {
                long end = TimeUtil.currentTimeMillis();
                long time = end - borrowed;
                StringBuffer buffer = new StringBuffer();
                buffer.append("[").append(borrowed - start).append(",").append(time).append("]ms (client-callback) traceID=").append(UUID.toString(traceID)).append(", api=").append(serviceRequestPacket.apiName);

                performanceLogger.debug(buffer.toString());
            }
            */

            //TODO 长连接，心跳处理，确认？
            if (conn != null && nioConnPool != null) {
                if(logger.isDebugEnabled()){
                    logger.debug("conn pool active:{}.",nioConnPool.getActive());
                }
                nioConnPool.returnObject(conn);
                if(logger.isDebugEnabled()){
                    logger.debug("conn pool active:{}.",nioConnPool.getActive());
                }
            }
        }
    }



    /**
     * 根据远程配置获取nio连接池 TODO 地址变化对连接的影响；地址未变但连接已断开其影响
     * @return
     * @throws Exception
     * @param url
     */
    public BackendConnectionPool getNioConnPool(URL url,ClientRemoteConfig remoteConfig) throws Exception {
        String address = String.format("%s:%s",url.getHost(),String.valueOf(url.getPort()));
        if(address.contains("10.47.16.172")){
            logger.info("get 10.47.16.172 connection pool.");
        }
        //若存在，则直接使用连接池
        if(nioPoolMap.get(address) != null){
            return nioPoolMap.get(address);
        }

        //若不存在，则创建连接池
        BackendConnectionPool backendConnectionPool = createNioPool(url,new ClientRemoteConfig());
        nioPoolMap.put(address,backendConnectionPool);
        return backendConnectionPool;
        /*
        String ipAddressList = remoteConfig.getFactory().getIpAddressList();
        if(nioPoolMap.get(ipAddressList) != null){
            return nioPoolMap.get(ipAddressList);
        }

        BackendConnectionPool backendConnectionPool = createNioPool(remoteConfig, realPoolMap);
        nioPoolMap.put(remoteConfig.getFactory().getIpAddressList(),backendConnectionPool);
        return backendConnectionPool;
        */
    }

    /**
     * 创建连接池
     * @param url
     * @param remoteConfig
     * @return
     * @throws Exception
     */
    private BackendConnectionPool createNioPool(URL url,ClientRemoteConfig remoteConfig) throws Exception {
        //初始化连接工厂
        VenusBackendConnectionFactory nioFactory = new VenusBackendConnectionFactory();
        nioFactory.setHost(url.getHost());
        nioFactory.setPort(Integer.valueOf(url.getPort()));
        //TODO auth信息
        if (remoteConfig.getAuthenticator() != null) {
            nioFactory.setAuthenticator(remoteConfig.getAuthenticator());
        }
        FactoryConfig factoryConfig = remoteConfig.getFactory();
        if (factoryConfig != null) {
            BeanUtils.copyProperties(nioFactory, factoryConfig);
        }
        nioFactory.setConnector(connector);
        nioFactory.setMessageHandler(messageHandler);

        //初始化连接池
        BackendConnectionPool nioPool = new PollingBackendConnectionPool("N-" + url.getHost(), nioFactory, 8);
        PoolConfig poolConfig = remoteConfig.getPool();
        if (poolConfig != null) {
            BeanUtils.copyProperties(nioPool, poolConfig);
        }
        nioPool.init();
        //若连接池创建失败，则释放连接池（fix 此时心跳检测已启动）
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
            throw new RpcException("init connection pool failed.");
        }
        return nioPool;
    }


    /**
     * 设置连接超时时间
     * @param conn
     * @param serviceConfig
     * @param endpoint
     * @throws SocketException
     */
    void setConnectionConfig(AbstractBIOConnection conn, ServiceConfig serviceConfig, Endpoint endpoint) throws SocketException {
        int soTimeout = 0;
        int oldTimeout = 0;

        oldTimeout = conn.getSoTimeout();
        if (serviceConfig != null) {
            EndpointConfig endpointConfig = serviceConfig.getEndpointConfig(endpoint.name());
            if (endpointConfig != null) {
                int eTimeOut = endpointConfig.getTimeWait();
                if (eTimeOut > 0) {
                    soTimeout = eTimeOut;
                }
            } else {
                if (serviceConfig.getTimeWait() > 0) {
                    soTimeout = serviceConfig.getTimeWait();
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
        if (soTimeout > 0) {
            conn.setSoTimeout(soTimeout);
        }
    }

    /**
     * 获取对应请求的响应结果
     * @param rpcId
     * @return
     */
    Result fetchResponse(String rpcId){
        AbstractServicePacket response = serviceResponseMap.get(rpcId);
        if(response == null){
            return null;
        }

        if(response instanceof OKPacket){//无返回值
            return new Result(null);
        }else if(response instanceof ServiceResponsePacket){//有返回值
            ServiceResponsePacket serviceResponsePacket = (ServiceResponsePacket)response;
            return new Result(serviceResponsePacket.result);
        }else if(response instanceof ErrorPacket){//调用出错
            ErrorPacket errorPacket = (ErrorPacket)response;
            Result result = new Result();
            result.setErrorCode(errorPacket.errorCode);
            result.setErrorMessage(errorPacket.message);
            return result;
        }else{
            return null;
        }
        //return serviceResponsePacket;
    }

    /**
     * 设置transactionId
     * @param athenaTransactionId
     * @param serviceRequestPacket
     * @param invocation
     */
    private void setTransactionId(AthenaTransactionId athenaTransactionId, SerializeServiceRequestPacket serviceRequestPacket, ClientInvocation invocation) {
        if (athenaTransactionId != null) {
            if (athenaTransactionId.getRootId() != null) {
                byte[] athenaId = athenaTransactionId.getRootId().getBytes();
                serviceRequestPacket.rootId = athenaId;
                invocation.setAthenaId(athenaId);
            }

            if (athenaTransactionId.getParentId() != null) {
                byte[] parentId = athenaTransactionId.getParentId().getBytes();
                serviceRequestPacket.parentId = parentId;
                invocation.setParentId(parentId);
            }

            if (athenaTransactionId.getMessageId() != null) {
                byte[] messageId = athenaTransactionId.getMessageId().getBytes();
                serviceRequestPacket.messageId = messageId;
                invocation.setMessageId(messageId);
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

    public ConnectionConnector getConnector() {
        return connector;
    }

    public void setConnector(ConnectionConnector connector) {
        VenusClientInvoker.connector = connector;
    }

    @Override
    public void destroy() throws RpcException{
        if (connector != null) {
            if (connector.isAlive()) {
                connector.shutdown();
            }
        }
        if (connManager != null) {
            if (connManager.isAlive()) {
                connManager.shutdown();
            }
        }
    }

    public int getAsyncExecutorSize() {
        return asyncExecutorSize;
    }

    public void setAsyncExecutorSize(int asyncExecutorSize) {
        this.asyncExecutorSize = asyncExecutorSize;
    }
}
