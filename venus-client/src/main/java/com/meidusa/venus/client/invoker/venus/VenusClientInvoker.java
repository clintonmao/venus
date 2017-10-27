package com.meidusa.venus.client.invoker.venus;

import com.chexiang.venus.demo.provider.model.Hello;
import com.meidusa.fastmark.feature.SerializerFeature;
import com.meidusa.toolkit.net.*;
import com.meidusa.venus.*;
import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.client.factory.xml.config.*;
import com.meidusa.venus.client.invoker.AbstractClientInvoker;
import com.meidusa.venus.exception.InvalidParameterException;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.exception.VenusExceptionFactory;
import com.meidusa.venus.io.network.AbstractBIOConnection;
import com.meidusa.venus.io.network.VenusBackendConnectionFactory;
import com.meidusa.venus.io.packet.*;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;
import com.meidusa.venus.io.serializer.Serializer;
import com.meidusa.venus.io.serializer.SerializerFactory;
import com.meidusa.venus.io.utils.RpcIdUtil;
import com.meidusa.venus.metainfo.EndpointParameter;
import com.meidusa.venus.monitor.athena.reporter.AthenaTransactionId;
import com.meidusa.venus.notify.InvocationListener;
import com.meidusa.venus.notify.ReferenceInvocationListener;
import com.meidusa.venus.support.EndpointWrapper;
import com.meidusa.venus.support.ServiceWrapper;
import com.meidusa.venus.support.VenusThreadContext;
import com.meidusa.venus.support.VenusUtil;
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

    private VenusExceptionFactory venusExceptionFactory;

    private boolean enableAsync = true;

    /*
    private static AtomicLong sequenceId = new AtomicLong(1);
    private boolean needPing = false;
    private XmlServiceFactory serviceFactory;
    */

    //TODO 确认此处线程池用途
    private int asyncExecutorSize = 10;

    /**
     * nio连接映射表
     */
    private Map<String, BackendConnectionPool> nioPoolMap = new ConcurrentHashMap<String, BackendConnectionPool>();

    //当前实例连接 TODO 【***】监听连接有效性，释放资源
    private ThreadLocal<BackendConnection> connectionThreadLocal = new ThreadLocal<BackendConnection>();

    /**
     * rpcId-请求&响应映射表，TODO 容量、有效期、清理机制
     */
    private Map<String, VenusReqRespWrapper> serviceReqRespMap = new ConcurrentHashMap<String, VenusReqRespWrapper>();

    /**
     * rpcId-请求&回调映射表
     */
    private Map<String, ClientInvocation> serviceReqCallbackMap = new ConcurrentHashMap<String, ClientInvocation>();

    /**
     * 调用监听容器
     */
    private InvocationListenerContainer container = new InvocationListenerContainer();

    /**
     * NIO消息响应处理
     */
    private VenusClientInvokerMessageHandler messageHandler = new VenusClientInvokerMessageHandler();

    private static ConnectionConnector connector;

    private boolean isInit = false;

    //默认连接数 TODO 连接数提醒配置 新建VenusConstants常量定义类
    private int coreConnections = 50;

    private static boolean isEnableRandomPrint = true;

    //mock返回线程池
    private static Executor mockReturnExecutor = null;

    static {
        if(connector == null){
            try {
                logger.error("###################init connector############");
                connector = new ConnectionConnector("connection connector-0");
                int ioThreads = 8;//Runtime.getRuntime().availableProcessors();
                ConnectionManager[] connectionManagers = new ConnectionManager[ioThreads];
                for(int i=0;i<ioThreads;i++){
                    ConnectionManager connManager = new ConnectionManager("connection manager-" + i, 10);
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

    @Override
    public void init() throws RpcException {
        if(!isInit){
            messageHandler.setVenusExceptionFactory(venusExceptionFactory);
            messageHandler.setContainer(this.container);
            messageHandler.setServiceReqCallbackMap(serviceReqCallbackMap);
            messageHandler.setServiceReqRespMap(serviceReqRespMap);
            isInit = true;
        }

    }

    @Override
    public Result doInvoke(ClientInvocation invocation, URL url) throws RpcException {
        long bTime = System.currentTimeMillis();
        try {
            if(!isCallbackInvocation(invocation)){
                return doInvokeWithSync(invocation, url);
            }else{
                return doInvokeWithCallback(invocation, url);
            }
        } catch (Exception e) {
            throw new RpcException(e);
        }finally {
            if(logger.isWarnEnabled()){
                logger.warn("request cost time:{}.",System.currentTimeMillis()-bTime);
            }
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
        Result result = null;

        long bWaitTime = System.currentTimeMillis();
        int timeout = invocation.getTimeout();

        //构造请求消息
        if("A".equalsIgnoreCase("B")){
            return new Result(new Hello("hi@","ok{invoker-doInvoke1}"));
        }
        SerializeServiceRequestPacket request = buildRequest(invocation);

        //添加rpcId -> reqResp映射表
        String rpcId = RpcIdUtil.getRpcId(request);
        VenusReqRespWrapper reqRespWrapper = new VenusReqRespWrapper(invocation);
        serviceReqRespMap.put(rpcId,reqRespWrapper);

        //发送消息
        if("A".equalsIgnoreCase("B")){
            return new Result(new Hello("hi@","ok{invoker-doInvoke2}"));
        }
        sendRequest(invocation, request, url);

        if("A".equalsIgnoreCase("B")){
            if(isEnableRandomPrint){
                if(ThreadLocalRandom.current().nextInt(100000) > 99995){
                    if(logger.isErrorEnabled()){
                        logger.error("build->send cost time:{}.",System.currentTimeMillis()-bWaitTime);
                    }
                }
            }
            return new Result(new Hello("hi@","ok{invoker-doInvoke3}"));
        }


        boolean isReturnMock = false;
        if(!isReturnMock){
            //latch阻塞等待
            //TODO #####超时时间提取配置#######
            reqRespWrapper.getReqRespLatch().await(timeout,TimeUnit.MILLISECONDS);
            //处理响应
            if("A".equalsIgnoreCase("B")){
                return new Result(new Hello("hi@","ok{invoker-doInvoke4}"));
            }
            result = fetchResponse(rpcId);
        }else{
            if(mockReturnExecutor == null){
                mockReturnExecutor = new ThreadPoolExecutor(10,20,0,TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(100000),new RejectedExecutionHandler(){
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        logger.error("mock return,exceed max process,maxThread:{},maxQueue:{}.",5,100);
                    }
                });
            }
            //mock接收消息处理
            mockReturnExecutor.execute(new MockReturnProcess(reqRespWrapper));

            reqRespWrapper.getReqRespLatch().await(timeout,TimeUnit.MILLISECONDS);

            result = fetchResponseFromMock(rpcId);
        }

        //TODO 改为methodPath
        if(result == null){
            throw new RpcException(String.format("invoke service:%s,timeout:%dms",url.getPath(),timeout));
        }

        if(isEnableRandomPrint){
            if(ThreadLocalRandom.current().nextInt(100000) > 99995){
                if(logger.isErrorEnabled()){
                    logger.error("build,send->fecth cost time:{}.",System.currentTimeMillis()-bWaitTime);
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
                reqRespWrapper.setPacket(null);
            } finally {
                reqRespWrapper.getReqRespLatch().countDown();
            }
        }
    }

    /**
     * 判断是否超时
     * @param totalWaitTime
     * @param bWaitTime
     * @return
     */
    /*
    boolean isTimeout(long totalWaitTime,long bWaitTime){
        long costTime = System.currentTimeMillis() - bWaitTime;
        return costTime >= totalWaitTime;
    }
    */

    /**
     * 获取剩余等待时间
     * @param totalWaitTime
     * @param bWaitTime
     * @return
     */
    /*
    long getRemainWaitTime(long totalWaitTime,long bWaitTime){
        long usedWaitTime = System.currentTimeMillis() - bWaitTime;
        long remainWaitTime = totalWaitTime - usedWaitTime;
        remainWaitTime = remainWaitTime < 1?1:remainWaitTime;
        return remainWaitTime;
    }
    */

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

        //添加rpcId-> reqResp映射表
        serviceReqCallbackMap.put(RpcIdUtil.getRpcId(request),invocation);

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
     * @param serviceRequestPacket
     * @param url 目标地址
     * @return
     * @throws Exception
     */
    void sendRequest(ClientInvocation invocation, SerializeServiceRequestPacket serviceRequestPacket, URL url) throws Exception{
        //long start = TimeUtil.currentTimeMillis();
        BackendConnectionPool nioConnPool = null;
        BackendConnection conn = null;
        try {
            //获取连接 TODO 地址变化情况
            nioConnPool = getNioConnPool(url,null);
            conn = nioConnPool.borrowObject();

            //发送请求消息，响应由handler类处理
            //String rpcId = RpcIdUtil.getRpcId(serviceRequestPacket);
            ByteBuffer buffer = serviceRequestPacket.toByteBuffer();
            VenusThreadContext.set(VenusThreadContext.CLIENT_OUTPUT_SIZE,Integer.valueOf(buffer.limit()));

            if("A".equalsIgnoreCase("B")){
                return;
            }
            conn.write(buffer);

            //logger.warn("send buffer cost time:{}.",System.currentTimeMillis()-bTime);
            //logger.warn("send request,rpcId:{},buff len:{},message:{}.",rpcId, buffer.limit(),JSONUtil.toJSONString(serviceRequestPacket));
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
                nioConnPool.returnObject(conn);
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
            BackendConnectionPool backendConnectionPool = null;
            //锁定，避免重复创建 TODO 存在并发问题
            synchronized (nioPoolMap){
                if(nioPoolMap.get(address) != null){
                    backendConnectionPool = nioPoolMap.get(address);
                }else{
                    backendConnectionPool = createNioPool(url,new ClientRemoteConfig());
                    nioPoolMap.put(address,backendConnectionPool);
                }
            }
            return backendConnectionPool;
        }

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
        logger.error("#########create nio pool#############:{}.",url);
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
            //BeanUtils.copyProperties(nioFactory, factoryConfig);
        }
        nioFactory.setConnector(connector);
        nioFactory.setMessageHandler(messageHandler);
        //nioFactory.setSendBufferSize(2);
        //nioFactory.setReceiveBufferSize(4);
        //nioFactory.setWriteQueueCapcity(16);

        //初始化连接池 TODO 连接数双倍问题
        BackendConnectionPool nioPool = new PollingBackendConnectionPool("N-" + url.getHost(), nioFactory, coreConnections);
        PoolConfig poolConfig = remoteConfig.getPool();
        if (poolConfig != null) {
            //BeanUtils.copyProperties(nioPool, poolConfig);
        }
        nioPool.init();
        //若连接池初始化失败，则释放连接池（TODO fix 此时心跳检测已启动）
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
        AbstractServicePacket response = serviceReqRespMap.get(rpcId).getPacket();
        if(response == null){
            return null;
        }

        //删除映射数据
        if(serviceReqRespMap.get(rpcId) != null){
            serviceReqRespMap.remove(rpcId);
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
    }

    /**
     * mock获取
     * @param rpcId
     * @return
     */
    Result fetchResponseFromMock(String rpcId){
        return new Result(new Hello("@hi","@mock result"));
    }

    /**
     * 设置transactionId TODO 监控上报用，确认是否保留？
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
        /*
        if (connManager != null) {
            if (connManager.isAlive()) {
                connManager.shutdown();
            }
        }
        */
    }

    public int getAsyncExecutorSize() {
        return asyncExecutorSize;
    }

    public void setAsyncExecutorSize(int asyncExecutorSize) {
        this.asyncExecutorSize = asyncExecutorSize;
    }
}
