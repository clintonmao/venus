package com.meidusa.venus.client.invoker.venus;

import com.meidusa.toolkit.common.heartbeat.HeartbeatManager;
import com.meidusa.toolkit.common.heartbeat.Status;
import com.meidusa.toolkit.net.*;
import com.meidusa.toolkit.net.factory.BackendConnectionFactory;
import com.meidusa.venus.ConnectionFactory;
import com.meidusa.venus.URL;
import com.meidusa.venus.client.ClientInvocation;
import com.meidusa.venus.client.factory.xml.config.ClientRemoteConfig;
import com.meidusa.venus.client.factory.xml.config.FactoryConfig;
import com.meidusa.venus.client.factory.xml.config.PoolConfig;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.io.network.Venus4BackendConnectionFactory;
import com.meidusa.venus.io.packet.*;
import com.meidusa.venus.support.VenusContext;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * venus连接工厂
 * Created by Zhangzhihua on 2018/3/1.
 */
public class VenusClientConnectionFactory implements ConnectionFactory {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    private static ConnectionConnector connector;

    private static ConnectionManager[] connectionManagers;

    //nio连接映射表
    private static Map<String, BackendConnectionPool> connectionPoolMap = new ConcurrentHashMap<String, BackendConnectionPool>();

    //rpcId-请求&响应映射表
    private Map<String, VenusReqRespWrapper> serviceReqRespMap;

    //rpcId-请求映射表
    private Map<String, ClientInvocation> serviceReqCallbackMap;

    public VenusClientConnectionFactory() {
        init();
    }

    /**
     * 初始化，初始化连接相关
     */
    void init() {
        synchronized (this) {
            //构造连接
            if (connector == null && connectionManagers == null) {
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("#########init connector#############");
                    }
                    connector = new ConnectionConnector("connection connector-0");
                    int ioThreads = Runtime.getRuntime().availableProcessors();
                    connectionManagers = new ConnectionManager[ioThreads];
                    for (int i = 0; i < ioThreads; i++) {
                        ConnectionManager connManager = new ConnectionManager("connection manager-" + i, -1);
                        //添加连接监听
                        connManager.addConnectionObserver(new VenusClientConnectionObserver());
                        connectionManagers[i] = connManager;
                        connManager.start();
                    }
                    connector.setProcessors(connectionManagers);
                    connector.start();
                } catch (IOException e) {
                    throw new RpcException(e);
                }
            }

            //设置connectionFactory
            VenusContext.getInstance().setConnectionFactory(this);
        }
    }

    /**
     * 获取connection
     *
     * @param url
     * @param invocation
     * @param remoteConfig
     * @return
     */
    BackendConnectionWrapper getConnection(URL url, ClientInvocation invocation, ClientRemoteConfig remoteConfig) {
        //获取连接
        BackendConnectionPool nioConnPool = null;
        BackendConnection conn = null;
        try {
            nioConnPool = getNioConnPool(url, invocation, null);
            conn = nioConnPool.borrowObject();
            if (conn != null && conn.isClosed()) {
                String address = url.getHost() + ":" + url.getPort();
                throw new RpcException(RpcException.NETWORK_EXCEPTION, String.format("get connection:%s failed,conn is closed.", address));
            }
            return new BackendConnectionWrapper(conn, nioConnPool);
        } catch (Exception e) {
            if(e instanceof RpcException){
                throw (RpcException)e;
            }
            String address = new StringBuilder()
                    .append(url.getHost())
                    .append(":")
                    .append(url.getHost())
                    .toString();
            throw new RpcException(RpcException.NETWORK_EXCEPTION, String.format("get connection:%s failed,exception:%s", address, e.getMessage()));
        }
    }

    /**
     * 根据远程配置获取nio连接池
     *
     * @param url
     * @return
     * @throws Exception
     */
    public BackendConnectionPool getNioConnPool(URL url, ClientInvocation invocation, ClientRemoteConfig remoteConfig) {
        String address = new StringBuilder()
                .append(url.getHost())
                .append(":")
                .append(url.getPort())
                .toString();
        //若存在相应地址的连接池
        if (connectionPoolMap.get(address) != null) {
            BackendConnectionPool connectionPool = connectionPoolMap.get(address);
            if (connectionPool.isValid()) {//若连接池有效
                if(!connectionPool.isClosed()){
                    return connectionPool;
                }
            } else {
                //若连接池无效
                if (!connectionPool.isClosed()) {
                    connectionPool.close();
                    connectionPoolMap.remove(address);
                }
            }
        }

        //若不存在可用连接池，则新建
        synchronized (connectionPoolMap) {
            //高并发场景，double check
            if (connectionPoolMap.get(address) != null) {
                BackendConnectionPool connectionPool = connectionPoolMap.get(address);
                if (connectionPool.isValid() && !connectionPool.isClosed()) {
                    return connectionPool;
                } else {
                    throw new RpcException(RpcException.NETWORK_EXCEPTION, "connection pool invalid or closed.");
                }
            } else {
                BackendConnectionPool connectionPool = createNioConnPool(url, invocation, new ClientRemoteConfig());
                connectionPoolMap.put(address, connectionPool);
                return connectionPool;
            }
        }
    }

    /**
     * 创建连接池
     *
     * @param url
     * @param remoteConfig
     * @return
     * @throws Exception
     */
    private BackendConnectionPool createNioConnPool(URL url, ClientInvocation invocation, ClientRemoteConfig remoteConfig) {
        String address = new StringBuilder()
                .append(url.getHost())
                .append(":")
                .append(url.getPort())
                .toString();
        if (logger.isInfoEnabled()) {
            logger.info("#########create nio pool:[{}]#############", address);
        }
        //初始化连接工厂
        Venus4BackendConnectionFactory nioFactory = new Venus4BackendConnectionFactory();
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
        BackendConnectionPool nioPool = new Venus4BackendConnectionPool("N-" + url.getHost(), nioFactory, connectionCount);
        PoolConfig poolConfig = remoteConfig.getPool();
        if (poolConfig != null) {
            //BeanUtils.copyProperties(nioPool, poolConfig);
        }
        try {
            nioPool.init();
            //若连接池初始化失败，则释放连接池（fix 此时心跳检测已启动）
            if (!nioPool.isValid()) {
                if (!nioPool.isClosed()) {
                    nioPool.close();
                }
                throw new RpcException(RpcException.NETWORK_EXCEPTION, "create connection pool invalid:" + address);
            }
        } catch (Exception e) {
            if(e instanceof RpcException){
                throw (RpcException)e;
            }
            if (nioPool != null && !nioPool.isClosed()) {
                nioPool.close();
            }
            throw new RpcException(RpcException.NETWORK_EXCEPTION, "init connection pool failed:" + address);
        }

        return nioPool;
    }


    @Override
    public void releaseConnection(Connection conn) {
        if (conn != null && conn instanceof BackendConnection) {
            BackendConnection backendConnection = (BackendConnection) conn;
            //释放latch信息
            releaseCountDownLatch(conn);

            //判断连接池状态若无效，则关闭连接池
            String address = new StringBuilder().append(backendConnection.getHost()).append(":").append(backendConnection.getPort()).toString();
            BackendConnectionPool connectionPool = connectionPoolMap.get(address);
            if (connectionPool != null && !connectionPool.isValid()) {
                releaseNioConnPool(address);
            }
        }
    }

    @Override
    public void releaseConnection(String address) {
        //判断连接池状态若无效，则关闭连接池
        BackendConnectionPool connectionPool = connectionPoolMap.get(address);
        if (connectionPool != null && !connectionPool.isValid()) {
            releaseNioConnPool(address);
        }
    }

    /**
     * 释放连接池
     *
     * @param address
     */
    void releaseNioConnPool(String address) {
        BackendConnectionPool connectionPool = connectionPoolMap.get(address);
        if (connectionPool == null || connectionPool.isClosed()) {
            return;
        }

        try {
            logger.info("connection pool:[{}] is invalid,release connection pool.", address);
            connectionPool.close();
            connectionPoolMap.remove(address);
        } catch (Exception e) {
            exceptionLogger.error("close connection pool failed:" + address, e);
        }
    }

    /**
     * 释放latch wait
     *
     * @param conn
     */
    void releaseCountDownLatch(Connection conn) {
        try {
            if (MapUtils.isEmpty(serviceReqRespMap)) {
                return;
            }
            //非正常关闭，释放所有使用此连接latch wait
            Collection<VenusReqRespWrapper> reqRespWrapperCollection = serviceReqRespMap.values();
            for (VenusReqRespWrapper reqRespWrapper : reqRespWrapperCollection) {
                if (conn == reqRespWrapper.getBackendConnection()) {
                    if (reqRespWrapper.getReqRespLatch() != null && reqRespWrapper.getReqRespLatch().getCount() > 0) {
                        if (logger.isWarnEnabled()) {
                            logger.warn("release latch:{}.", reqRespWrapper.getReqRespLatch());
                        }
                        reqRespWrapper.getReqRespLatch().countDown();
                    }
                }
            }
        } catch (Exception e) {
            if (exceptionLogger.isErrorEnabled()) {
                exceptionLogger.error("release countDown latch error.", e);
            }
        }
    }

    public Map<String, VenusReqRespWrapper> getServiceReqRespMap() {
        return serviceReqRespMap;
    }

    public void setServiceReqRespMap(Map<String, VenusReqRespWrapper> serviceReqRespMap) {
        this.serviceReqRespMap = serviceReqRespMap;
    }

    public Map<String, ClientInvocation> getServiceReqCallbackMap() {
        return serviceReqCallbackMap;
    }

    public void setServiceReqCallbackMap(Map<String, ClientInvocation> serviceReqCallbackMap) {
        this.serviceReqCallbackMap = serviceReqCallbackMap;
    }

    @Override
    public void destroy() {
        //释放连接
        if (connector != null) {
            if (connector.isAlive()) {
                connector.shutdown();
            }
        }

        if (connectionManagers != null && connectionManagers.length > 0) {
            for (ConnectionManager connManager : connectionManagers) {
                if (connManager != null) {
                    if (connManager.isAlive()) {
                        connManager.shutdown();
                    }
                }
            }
        }
    }

    //connection wraaper
    class BackendConnectionWrapper {
        private BackendConnection backendConnection;

        private BackendConnectionPool backendConnectionPool;

        public BackendConnectionWrapper(BackendConnection backendConnection, BackendConnectionPool backendConnectionPool) {
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

    /**
     * venus4实现，扩展pool状态检查功能
     * @param <F>
     * @param <V>
     */
    class Venus4BackendConnectionPool<F extends BackendConnectionFactory, V extends BackendConnection> implements BackendConnectionPool {

        private final Logger LOGGER = LoggerFactory.getLogger(Venus4BackendConnectionPool.class);

        private int HEATBEAT_INTERVAL = Integer.getInteger("heartbeat.interval", HeartbeatManager.DEFAULT_HEATBEAT_INTERVAL);
        /**
         * 非严格的计数器
         */
        private volatile long currentCounter = 0L;
        private final F factory;
        private final int size;
        private final BackendConnection[] items;
        private String name;
        private boolean valid = true;
        private boolean closed = false;
        private ConnectionPoolHeartbeatChecker heartbeatChecker;
        private final Map<Integer, Object> lockMap = new HashMap<Integer, Object>();

        public Venus4BackendConnectionPool(String name, F factory, int size) {
            this.size = size;
            this.items = new BackendConnection[size];
            this.factory = factory;
            this.name = name;
        }

        @Override
        public void init() {
            for (int i = 0; i < items.length; i++) {
                this.lockMap.put(i, new Object());
            }
            for (int i = 0; i < items.length; i++) {
                try {
                    items[i] = factory.make();
                } catch (IOException e) {
                    LOGGER.error("init pool error ,name=" + this.name, e);
                    this.setValid(false);
                    break;
                }
            }

            //连接池检查
            heartbeatChecker = new ConnectionPoolHeartbeatChecker(HEATBEAT_INTERVAL, TimeUnit.SECONDS, this, this.factory, this.size);
            HeartbeatManager.addHeartbeat(heartbeatChecker);
        }

        @Override
        public BackendConnection borrowObject() throws Exception {
            BackendConnection conn = null;

            /**
             * 循环次数为 size+1，主要目的是避免目标服务重启以后，当前的所有连接已经处于closed状态，
             * 必须要再进行创建一次连接，如果该连接有效则说明pool有效，否则视为无效
             *
             */
            for (int j = 0; j < size + 1; j++) {
                int i = (int) ((currentCounter++) % size);
                conn = items[i];
                if (conn == null) {
                    Object lockObject = lockMap.get(i);
                    synchronized (lockObject) {
                        conn = items[i];
                        if (conn == null) {
                            conn = items[i] = factory.make();
                            conn.setPool(this);
                        }
                    }
                }

                if (!conn.isClosed()) {
                    conn.setActive(true);
                    return conn;
                } else {
                    items[i] = null;
                }
            }

            this.setValid(false);
            throw new InvalidObjectException("invalid pool=" + this.name);
        }

        @Override
        public void returnObject(BackendConnection c) {
            c.setActive(false);
        }

        @Override
        public synchronized void close() {
            if (closed) {
                return;
            }
            closed = true;
            if (heartbeatChecker != null) {
                HeartbeatManager.removeHeartbeat(heartbeatChecker);
            }
            for (BackendConnection conn : items) {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            }
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getActive() {
            return items.length;
        }

        @Override
        public void deActive(BackendConnection c) {
            //polling属于循环，该pool中连接可以重用，因此无所谓是否被使用
            c.setActive(false);
        }

        @Override
        //扩展判断，改为实时判断
        public boolean isValid() {
            if(this.items == null || this.items.length == 0){
                return false;
            }

            //若有未关闭的，则连接池为可用
            for(BackendConnection item:items){
                if(item != null && !item.isClosed()){
                    return true;
                }
            }
            return false;
            //return this.valid;
        }

        @Override
        public void setValid(boolean b) {
            this.valid = b;
        }

        public boolean isClosed() {
            return closed;
        }

    }


    /**
     * 覆写状态检查checker
     */
    class ConnectionPoolHeartbeatChecker extends BackendConnectionPool.ObjectPoolHeartbeatDelayed {
        Status last = Status.VALID;
        private BackendConnectionFactory factory;
        private BackendConnection idleConn;
        private ConnectionPoolHeartbeatHandler idleHandler;
        private int size;

        public ConnectionPoolHeartbeatChecker(long nsTime, TimeUnit timeUnit, BackendConnectionPool pool, BackendConnectionFactory factory, int size) {
            super(nsTime, timeUnit, pool);
            this.factory = factory;
            this.size = size;
        }

        public Status doCheck() {
            Status status = null;
            try {
                status = doCheckEx();
                if (last == Status.INVALID && status == Status.VALID) {
                    //do init pool
                    for (int i = 0; i < size; i++) {
                        status = doCheckEx();
                        if (status == Status.INVALID) {
                            return status;
                        }
                    }
                    return status;
                }
                return status;
            } catch (Exception ex){
                return Status.INVALID;
            }finally {
                last = status;
            }
        }

        private Status doCheckEx() {
            BackendConnection conn = null;
            try {
                conn = pool.borrowObject();
                if (conn != null) {
                    if (conn.isClosed()) {
                        pool.setValid(false);
                        return Status.INVALID;
                    } else {
                        pool.setValid(true);
                        return Status.VALID;
                    }
                } else {
                    return Status.INVALID;
                }
            } catch (Exception e) {
                logger.warn("connection pool check error.", e);
                pool.setValid(false);
                return Status.INVALID;
            } finally {
                if (conn != null) {
                    pool.returnObject(conn);
                }
            }
        }

        /**
         * 通过发送状态消息包检查
         * @return
         * @throws Exception
         */
        Status doHeartbeatCheck() throws Exception{
            if(idleConn == null || idleConn.isClosed()){
                idleConn = factory.make();
                idleHandler = new ConnectionPoolHeartbeatHandler();
                idleConn.setHandler(idleHandler);
            }
            idleHandler.sendMsg(idleConn);
            Status status = idleHandler.getResult(idleConn);
            if (status == Status.VALID) {
                pool.setValid(true);
                return Status.VALID;
            } else {
                pool.setValid(false);
                return status;
            }
        }

        public boolean isCycle() {
            return true;
        }

    }

    /**
     * 扩展状态检查handler
     *
     * @param <T>
     */
    class ConnectionPoolHeartbeatHandler<T> implements MessageHandler<BackendConnection, T> {

        private CountDownLatch latch = new CountDownLatch(1);
        private Status status;

        protected void setStatus(Status status) {
            this.status = status;
            latch.countDown();
        }

        public void sendMsg(BackendConnection conn) {
            VenusStatusRequestPacket packet = new VenusStatusRequestPacket();
            conn.write(packet.toByteBuffer());
        }

        protected Status getResult(BackendConnection conn) {
            try {
                latch.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }
            return status;
        }

        @Override
        public void handle(BackendConnection conn, T data) {
            byte[] message = (byte[]) data;
            int type = AbstractServicePacket.getType(message);
            if (type == AbstractVenusPacket.PACKET_TYPE_PONG) {
                this.setStatus(Status.VALID);
            } else if (type == AbstractVenusPacket.PACKET_TYPE_VENUS_STATUS_RESPONSE) {
                VenusStatusResponsePacket packet = new VenusStatusResponsePacket();
                packet.init(message);
                if ((packet.status & PacketConstant.VENUS_STATUS_OUT_OF_MEMORY) > 0) {
                    this.setStatus(Status.OUT_OF_MEMORY);
                } else if ((packet.status & PacketConstant.VENUS_STATUS_SHUTDOWN) > 0) {
                    this.setStatus(Status.INVALID);
                } else {
                    this.setStatus(Status.VALID);
                }
            }
        }

    }


}
