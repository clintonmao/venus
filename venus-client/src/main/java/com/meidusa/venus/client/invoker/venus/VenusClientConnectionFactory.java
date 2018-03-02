package com.meidusa.venus.client.invoker.venus;

import com.meidusa.toolkit.net.*;
import com.meidusa.venus.ConnectionFactory;
import com.meidusa.venus.URL;
import com.meidusa.venus.VenusApplication;
import com.meidusa.venus.client.ClientInvocation;
import com.meidusa.venus.client.factory.xml.config.ClientRemoteConfig;
import com.meidusa.venus.client.factory.xml.config.FactoryConfig;
import com.meidusa.venus.client.factory.xml.config.PoolConfig;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.io.network.VenusBackendConnectionFactory;
import com.meidusa.venus.io.packet.PacketConstant;
import com.meidusa.venus.support.VenusContext;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    public VenusClientConnectionFactory(){
        init();
    }

    /**
     * 初始化，初始化连接相关
     */
    void init(){
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
     * @param url
     * @param invocation
     * @param remoteConfig
     * @return
     */
    BackendConnectionWrapper getConnection(URL url, ClientInvocation invocation, ClientRemoteConfig remoteConfig){
        //获取连接
        BackendConnectionPool nioConnPool = null;
        BackendConnection conn = null;
        try {
            nioConnPool = getNioConnPool(url,invocation,null);
            conn = nioConnPool.borrowObject();
            if(conn != null && conn.isClosed()){
                String address = url.getHost()+":" + url.getPort();
                throw new RpcException(RpcException.NETWORK_EXCEPTION,String.format("get connection:%s failed,conn is closed.",address));
            }
            return new BackendConnectionWrapper(conn,nioConnPool);
        } catch (Exception e) {
            String address = new StringBuilder()
                    .append(url.getHost())
                    .append(":")
                    .append(url.getHost())
                    .toString();
            throw new RpcException(RpcException.NETWORK_EXCEPTION,String.format("get connection:%s failed,exception:%s",address,e.getMessage()));
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
        //若存在相应地址的连接池
        if(connectionPoolMap.get(address) != null){
            BackendConnectionPool connectionPool = connectionPoolMap.get(address);
            //若连接池有效，则使用
            if(connectionPool.isValid() && !connectionPool.isClosed()){
                return connectionPool;
            }else{
                //若连接池无效，则关闭
                if(!connectionPool.isClosed()){
                    try {
                        //fix 由于并发无法正常关闭问题
                        for(int i=0;i<20;i++){
                            if(connectionPool != null){
                                connectionPool.close();
                                connectionPoolMap.remove(address);
                            }
                            Thread.sleep(10);
                        }
                    } catch (Exception e) {}
                }
            }
        }

        //若不存在可用连接池，则新建
        synchronized (connectionPoolMap){
            //高并发场景，double check
            if(connectionPoolMap.get(address) != null){
                BackendConnectionPool connectionPool = connectionPoolMap.get(address);
                if(connectionPool.isValid() && !connectionPool.isClosed()){
                    return connectionPool;
                }else{
                    throw new RpcException(RpcException.NETWORK_EXCEPTION,"connection pool invalid or closed.");
                }
            }else{
                BackendConnectionPool connectionPool = createNioConnPool(url,invocation,new ClientRemoteConfig());
                connectionPoolMap.put(address,connectionPool);
                return connectionPool;
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

    @Override
    public void releaseConnection(String address) {
        releaseNioConnPool(address);
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
     * 判断连接池是否有效(因组件内部判断连接池有效性接口存在延时，不准确)
     * @return
     */
    /*
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
    */

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
