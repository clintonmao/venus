package com.meidusa.venus.bus.dispatch;

import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.toolkit.net.*;
import com.meidusa.toolkit.util.StringUtil;
import com.meidusa.venus.Invocation;
import com.meidusa.venus.Result;
import com.meidusa.venus.RpcException;
import com.meidusa.venus.URL;
import com.meidusa.venus.bus.BusInvocation;
import com.meidusa.venus.bus.config.BusRemoteConfig;
import com.meidusa.venus.bus.handler.BusDispatchMessageHandler;
import com.meidusa.venus.bus.network.BusBackendConnection;
import com.meidusa.venus.bus.network.BusBackendConnectionFactory;
import com.meidusa.venus.bus.network.BusFrontendConnection;
import com.meidusa.venus.io.authenticate.Authenticator;
import com.meidusa.venus.io.packet.PacketConstant;
import com.meidusa.venus.io.packet.ServicePacketBuffer;
import com.meidusa.venus.io.packet.VenusRouterPacket;
import com.meidusa.venus.util.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息分发处理
 * Created by Zhangzhihua on 2017/9/1.
 */
public class BusDispatcher implements Dispatcher{

    private static Logger logger = LoggerFactory.getLogger(BusDispatcher.class);

    protected int DEFAULT_POOL_SIZE = BusRemoteConfig.DEFAULT_POOL_SIZE;

    private static ConnectionConnector connector;

    private static ConnectionManager connManager;

    private static boolean isInitConnector = false;

    private boolean enableAsync = true;

    private int asyncExecutorSize = 10;

    /**
     * nio连接映射表
     */
    private Map<String, BackendConnectionPool> nioPoolMap = new HashMap<String, BackendConnectionPool>();

    //TODO set observer
    private BusDispatchMessageHandler messageHandler = new BusDispatchMessageHandler();

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

    }

    /**
     * 转发消息
     * @param invocation
     */
    @Override
    public Result invoke(Invocation invocation,URL url) throws RpcException{
        try {
            //构造请求
            ByteBuffer byteBuffer = buildRequest(invocation);

            //发送请求
            sendRequest(invocation,byteBuffer,url);

            //若未出现异常，则直接返回Null,响应结果由response线程处理
            return null;
        } catch (Exception e) {
            throw new RpcException(e);
        }
    }

    /**
     * 构造请求报文
     * @param invocation
     * @return
     */
    ByteBuffer buildRequest(Invocation invocation){
        BusInvocation busInvocation = (BusInvocation)invocation;
        List<Tuple<Range, BackendConnectionPool>> list = busInvocation.getList();
        BusFrontendConnection srcConn = busInvocation.getSrcConn();
        VenusRouterPacket routerPacket = busInvocation.getRouterPacket();
        ServicePacketBuffer packetBuffer = busInvocation.getPacketBuffer();
        String apiName = busInvocation.getApiName();
        int serviceVersion = busInvocation.getServiceVersion();
        byte[] traceId = busInvocation.getTraceId();

        ByteBuffer byteBuffer = VenusRouterPacket.toByteBuffer(routerPacket);
        return byteBuffer;
    }

    /**
     * 发送请求
     * @param invocation
     * @param byteBuffer
     * @param url
     */
    void sendRequest(Invocation invocation,ByteBuffer byteBuffer,URL url){
        BusInvocation busInvocation = (BusInvocation)invocation;
        BusFrontendConnection srcConn = busInvocation.getSrcConn();
        VenusRouterPacket routerPacket = busInvocation.getRouterPacket();

        //转发
        BackendConnectionPool connectionPool = null;
        BusBackendConnection remoteConn = null;
        try {
            connectionPool = getConnectionPool(url);
            //TODO 确认BusBackendConnection与backendConnection差异性
            remoteConn = (BusBackendConnection)connectionPool.borrowObject();
            routerPacket.backendRequestID = remoteConn.getNextRequestID();
            remoteConn.addRequest(routerPacket.backendRequestID, routerPacket.frontendConnectionID, routerPacket.frontendRequestID);
            srcConn.addUnCompleted(routerPacket.frontendRequestID, routerPacket);
            //转发消息
            remoteConn.write(VenusRouterPacket.toByteBuffer(routerPacket));

            //TODO 确认日志输出功能
            //VenusTracerUtil.logRouter(traceId, apiName, srcConn.getInetAddress().getHostAddress(), remoteConn.getHost()+":"+remoteConn.getPort());
        } catch (Exception e) {
            /* TODO 此段代码功能确认？
            srcConn.addUnCompleted(routerPacket.frontendRequestID, routerPacket);
            srcConn.getRetryHandler().addRetry(srcConn, routerPacket);
            */
            throw new RpcException(e);
        } finally {
            if (connectionPool != null && remoteConn != null) {
                //TODO 连接释放，要释放？
                connectionPool.returnObject(remoteConn);
            }
        }
    }

    /**
     * 获取连接池
     * @param url
     * @return
     */
    BackendConnectionPool getConnectionPool(URL url){
        try {
            //若存在，则直接使用，否则新建
            String address = String.format("%s:%s",url.getHost(),String.valueOf(url.getPort()));
            if(nioPoolMap.get(address) != null){
                return nioPoolMap.get(address);
            }
            BackendConnectionPool connectionPool =  createConnPool(address,null);
            nioPoolMap.put(address,connectionPool);
            return connectionPool;
        } catch (Exception e) {
            throw new RpcException("get backend connection failed.",e);
        }
    }

    /**
     * 创建连接池 TODO 地址失效及相关情况处理
     * @param address
     * @param authenticator
     * @return
     */
    BackendConnectionPool createConnPool(String address, @SuppressWarnings("rawtypes") Authenticator authenticator) {
        BusBackendConnectionFactory nioFactory = new BusBackendConnectionFactory();
        if (authenticator != null) {
            nioFactory.setAuthenticator(authenticator);
        }
        BackendConnectionPool pool = null;
        String[] arr = StringUtil.split(address, ":");
        if (arr.length > 1) {
            nioFactory.setHost(arr[0]);
            nioFactory.setPort(Integer.valueOf(arr[1]));
        } else {
            nioFactory.setHost(arr[0]);
            nioFactory.setPort(PacketConstant.VENUS_DEFAULT_PORT);
        }

        //设置connector
        nioFactory.setConnector(getConnector());
        //设置messageHandler
        nioFactory.setMessageHandler(getMessageHandler());

        pool = new PollingBackendConnectionPool(address, nioFactory, DEFAULT_POOL_SIZE);
        pool.init();
        return pool;
    }

    @Override
    public void destroy() throws RpcException {

    }


    public static ConnectionConnector getConnector() {
        return connector;
    }

    public static ConnectionManager getConnManager() {
        return connManager;
    }

    public boolean isEnableAsync() {
        return enableAsync;
    }

    public int getAsyncExecutorSize() {
        return asyncExecutorSize;
    }

    public BusDispatchMessageHandler getMessageHandler() {
        return messageHandler;
    }
}
