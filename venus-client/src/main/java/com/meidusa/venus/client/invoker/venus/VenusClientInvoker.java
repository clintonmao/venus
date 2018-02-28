package com.meidusa.venus.client.invoker.venus;

import com.meidusa.toolkit.net.*;
import com.meidusa.toolkit.util.TimeUtil;
import com.meidusa.venus.Invoker;
import com.meidusa.venus.Result;
import com.meidusa.venus.URL;
import com.meidusa.venus.VenusApplication;
import com.meidusa.venus.client.ClientInvocation;
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
import com.meidusa.venus.util.VenusLoggerFactory;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
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

    private static ConnectionConnector connector;

    private static ConnectionManager[] connectionManagers;

    //nio连接映射表
    private static Map<String, BackendConnectionPool> connectionPoolMap = new ConcurrentHashMap<String, BackendConnectionPool>();

    //rpcId-请求&响应映射表
    private static Map<String, VenusReqRespWrapper> serviceReqRespMap = new ConcurrentHashMap<String, VenusReqRespWrapper>();

    //rpcId-请求&回调映射表
    private static Map<String, ClientInvocation> serviceReqCallbackMap = new ConcurrentHashMap<String, ClientInvocation>();

    public VenusClientInvoker(){
        synchronized (this){
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
                        VenusClientConnectionObserver connectionObserver = new VenusClientConnectionObserver();
                        connManager.addConnectionObserver(connectionObserver);
                        connectionManagers[i] = connManager;
                        connManager.start();
                    }
                    connector.setProcessors(connectionManagers);
                    connector.start();
                } catch (IOException e) {
                    throw new RpcException(e);
                }

                //添加invoker资源
                VenusApplication.addInvoker(this);
                //设置invoker到上下文
                VenusContext.getInstance().setInvoker(this);
            }

        }
    }


    @Override
    public void init() throws RpcException {
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
            throw new RpcException(RpcException.TIMEOUT_EXCEPTION,String.format("invoke api:%s,service:%s timeout,timeout:%dms",invocation.getApiName(),url.getPath(),timeout));
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
            BackendConnectionWrapper connectionWrapper = getConnection(url,invocation,remoteConfig);
            nioConnPool = connectionWrapper.getBackendConnectionPool();
            conn = connectionWrapper.getBackendConnection();
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
            //athena调用输出到default
            Logger trLogger = tracerLogger;
            if(VenusUtil.isAthenaInterface(invocation)){
                trLogger = logger;
            }

            if(exception != null){
                //输出异常日志
                if (trLogger.isErrorEnabled()) {
                    String tpl = "[C] [failed,{}] send request failed,rpcId:{},api:{},method:{},targetIp:{},exception:{}.";
                    Object[] arguments = new Object[]{
                            totalTime + "ms," + connTime+"ms",
                            rpcId,
                            invocation.getApiName(),
                            invocation.getMethodPath(),
                            url.getHost(),
                            exception
                    };
                    trLogger.error(tpl,arguments);
                    //错误日志
                    exceptionLogger.error(tpl,arguments);
                }
            }else{
                if(trLogger.isInfoEnabled()){
                    String tpl = "[C] [{}] send request,rpcId:{},api:{},method:{},targetIp:{}.";
                    Object[] arguments = new Object[]{
                            totalTime + "ms," + connTime+"ms",
                            rpcId,
                            invocation.getApiName(),
                            invocation.getMethodPath(),
                            url.getHost(),
                    };
                    trLogger.info(tpl,arguments);
                }
            }
        }
    }


    /**
     * 获取connection
     * @param url
     * @param invocation
     * @param remoteConfig
     * @return
     */
    BackendConnectionWrapper getConnection(URL url,ClientInvocation invocation,ClientRemoteConfig remoteConfig){
        //获取连接
        BackendConnection conn = null;
        BackendConnectionPool nioConnPool = getNioConnPool(url,invocation,null);
        try {
            conn = nioConnPool.borrowObject();
        } catch (Exception e) {
            String address = new StringBuilder()
                    .append(url.getHost())
                    .append(":")
                    .append(url.getHost())
                    .toString();
            throw new RpcException(RpcException.NETWORK_EXCEPTION,String.format("borrow connection:[%s] failed",address));
        }
        return new BackendConnectionWrapper(conn,nioConnPool);
    }

    @Override
    public void releaseConnection(Connection conn) {
        if(conn != null && conn instanceof BackendConnection){
            BackendConnection backendConnection = (BackendConnection)conn;
            //释放latch信息
            releaseCountDownLatch(conn);
            //释放连接池资源
            String address = new StringBuilder().append(backendConnection.getHost()).append(":").append(backendConnection.getPort()).toString();
            releaseNioConnPool(address);
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
        }
        //若不存在，则创建连接池
        synchronized (connectionPoolMap){
            BackendConnectionPool backendConnectionPool = null;
            if(connectionPoolMap.get(address) != null){
                backendConnectionPool = connectionPoolMap.get(address);
            }else{
                backendConnectionPool = createNioConnPool(url,invocation,new ClientRemoteConfig());
                connectionPoolMap.put(address,backendConnectionPool);
            }
            return backendConnectionPool;
        }
    }

    /**
     * 创建连接池
     * @param url
     * @param remoteConfig
     * @return
     * @throws Exception
     */
    private BackendConnectionPool createNioConnPool(URL url, ClientInvocation invocation, ClientRemoteConfig remoteConfig){
        String address = new StringBuilder()
                .append(url.getHost())
                .append(":")
                .append(url.getPort())
                .toString();
        if(logger.isInfoEnabled()){
            logger.info("#########create nio pool:[{}]#############",address);
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

        //初始化messageHandler
        VenusClientInvokerMessageHandler messageHandler = new VenusClientInvokerMessageHandler();
        messageHandler.setServiceReqRespMap(serviceReqRespMap);
        messageHandler.setServiceReqCallbackMap(serviceReqCallbackMap);
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
        try {
            nioPool.init();
        } catch (Exception e) {
            if(!nioPool.isClosed()){
                try {
                    //fix 无法正常关闭pool
                    for(int i=0;i<20;i++){
                        if(nioPool != null){
                            nioPool.close();
                        }
                        Thread.sleep(10);
                    }
                } catch (Exception ex) {}
            }
            throw new RpcException(RpcException.NETWORK_EXCEPTION,"init connection pool failed:" + address);
        }

        //若连接池初始化失败，则释放连接池（fix 此时心跳检测已启动）
        if(!nioPool.isValid()){
            if(!nioPool.isClosed()){
                try {
                    //fix 无法正常关闭pool
                    for(int i=0;i<20;i++){
                        if(nioPool != null){
                            nioPool.close();
                        }
                        Thread.sleep(10);
                    }
                } catch (Exception e) {}
            }
            throw new RpcException(RpcException.NETWORK_EXCEPTION,"create connection pool invalid:" + address);
        }
        return nioPool;
    }


    /**
     * 释放连接池
     * @param address
     */
    void releaseNioConnPool(String address){
        BackendConnectionPool connectionPool = connectionPoolMap.get(address);
        if(connectionPool == null || connectionPool.isClosed()){
            return;
        }

        if(false){//连接池有效，存在可用连接
           logger.info("connection pool:[{}] is valid.",address);
        }else{//连接池无效，所有连接不可用
            try {
                logger.info("connection pool:[{}] is invalid,release connection pool.",address);
                //循环关闭，解决并发冲突无法正常关闭问题
                for(int i=0;i<20;i++){
                    if(connectionPool != null){
                        connectionPool.close();
                        connectionPoolMap.remove(address);
                    }
                }
            } catch (Exception e) {
                exceptionLogger.error("close connection pool failed:" + address,e);
            }
        }
    }

    /**
     * 判断连接池是否有效(因组件内部判断连接池有效性接口存在延时，不准确)
     * @param connectionPool
     * @return
     */
    boolean isValidConnPool(BackendConnectionPool connectionPool){
        int size = VenusConstants.CONNECTION_DEFAULT_COUNT*2;
        for(int i=0;i<size;i++){
            BackendConnection connection = null;
            try {
                connection = connectionPool.borrowObject();
            } catch (Exception e) {
                //无法获取连接
                if(logger.isDebugEnabled()){
                    logger.debug("not borrow conn from conn pool:" + connectionPool.getName(),e);
                }
                return false;
            }finally {
                if(connectionPool != null && connection != null){
                    try {
                        connectionPool.returnObject(connection);
                    } catch (Exception e) {}
                }
            }
        }
        return true;
    }

    /**
     * 释放latch wait
     * @param conn
     */
    void releaseCountDownLatch(Connection conn){
        try {
            if(MapUtils.isEmpty(serviceReqRespMap)){
                return;
            }
            //非正常关闭，释放所有使用此连接latch wait
            Collection<VenusReqRespWrapper> reqRespWrapperCollection = serviceReqRespMap.values();
            for(VenusReqRespWrapper reqRespWrapper:reqRespWrapperCollection){
                if(conn == reqRespWrapper.getBackendConnection()){
                    if(reqRespWrapper.getReqRespLatch() != null && reqRespWrapper.getReqRespLatch().getCount() > 0){
                        if(logger.isWarnEnabled()){
                            logger.warn("release latch:{}.",reqRespWrapper.getReqRespLatch());
                        }
                        reqRespWrapper.getReqRespLatch().countDown();
                    }
                }
            }
        } catch (Exception e) {
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error("release countDown latch error.",e);
            }
        }
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

    //connection wraaper
    class BackendConnectionWrapper{
        private BackendConnection backendConnection;

        private BackendConnectionPool backendConnectionPool;

        public BackendConnectionWrapper(BackendConnection backendConnection,BackendConnectionPool backendConnectionPool){
            this.backendConnection = backendConnection;
            this.backendConnectionPool = backendConnectionPool;
        }

        public BackendConnection getBackendConnection() {
            return backendConnection;
        }

        public void setBackendConnection(BackendConnection backendConnection) {
            this.backendConnection = backendConnection;
        }

        public BackendConnectionPool getBackendConnectionPool() {
            return backendConnectionPool;
        }

        public void setBackendConnectionPool(BackendConnectionPool backendConnectionPool) {
            this.backendConnectionPool = backendConnectionPool;
        }
    }


}
