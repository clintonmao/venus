package com.meidusa.venus.bus.dispatch;

import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.toolkit.net.BackendConnectionPool;
import com.meidusa.toolkit.net.MessageHandler;
import com.meidusa.toolkit.net.PollingBackendConnectionPool;
import com.meidusa.toolkit.util.StringUtil;
import com.meidusa.venus.Invocation;
import com.meidusa.venus.Result;
import com.meidusa.venus.RpcException;
import com.meidusa.venus.URL;
import com.meidusa.venus.bus.BusInvocation;
import com.meidusa.venus.bus.network.BusBackendConnection;
import com.meidusa.venus.bus.network.BusBackendConnectionFactory;
import com.meidusa.venus.bus.network.BusFrontendConnection;
import com.meidusa.venus.bus.registry.xml.bean.Remote;
import com.meidusa.venus.io.authenticate.Authenticator;
import com.meidusa.venus.io.packet.PacketConstant;
import com.meidusa.venus.io.packet.ServicePacketBuffer;
import com.meidusa.venus.io.packet.VenusRouterPacket;
import com.meidusa.venus.util.Range;
import com.meidusa.venus.util.VenusTracerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 消息分发处理
 * Created by Zhangzhihua on 2017/9/1.
 */
public class BusDispatcher implements Dispatcher{

    private static Logger logger = LoggerFactory.getLogger(BusDispatcher.class);

    protected int DEFAULT_POOL_SIZE = Remote.DEFAULT_POOL_SIZE;

    protected MessageHandler messageHandler;

    @Override
    public void init() throws RpcException {

    }


    /**
     * 转发消息
     * @param invocation
     */
    @Override
    public Result invoke(Invocation invocation,URL url) throws RpcException{
        BusInvocation busInvocation = (BusInvocation)invocation;
        List<Tuple<Range, BackendConnectionPool>> list = busInvocation.getList();
        BusFrontendConnection srcConn = busInvocation.getSrcConn();
        VenusRouterPacket routerPacket = busInvocation.getRouterPacket();
        ServicePacketBuffer packetBuffer = busInvocation.getPacketBuffer();
        String apiName = busInvocation.getApiName();
        int serviceVersion = busInvocation.getServiceVersion();
        byte[] traceId = busInvocation.getTraceId();

        //转发
        BackendConnectionPool connectionPool = null;
        BusBackendConnection remoteConn = null;
        try {
            connectionPool = getConnectionPool(url);
            //TODO 确认BusBackendConnection与backendConnection差异性
            remoteConn = (BusBackendConnection)connectionPool.borrowObject();
            //remoteConn = getConnectionPool(url);//TODO 更改连接获取方式 tuple.right.borrowObject();
            routerPacket.backendRequestID = remoteConn.getNextRequestID();
            remoteConn.addRequest(routerPacket.backendRequestID, routerPacket.frontendConnectionID, routerPacket.frontendRequestID);
            srcConn.addUnCompleted(routerPacket.frontendRequestID, routerPacket);
            //转发消息
            remoteConn.write(VenusRouterPacket.toByteBuffer(routerPacket));
            VenusTracerUtil.logRouter(traceId, apiName, srcConn.getInetAddress().getHostAddress(), remoteConn.getHost()+":"+remoteConn.getPort());

            //若未出现异常，则直接返回Null,响应结果由response线程处理
            return null;
        } catch (Exception e) {
            /* TODO 此段代码功能确认？
            srcConn.addUnCompleted(routerPacket.frontendRequestID, routerPacket);
            srcConn.getRetryHandler().addRetry(srcConn, routerPacket);
            */
            throw new RpcException(e);
        } finally {
            if (connectionPool != null && remoteConn != null) {
                //TODO 连接释放，要释放？
                //tuple.right.returnObject(remoteConn);
                connectionPool.returnObject(remoteConn);
            }
        }
    }

    @Override
    public void destroy() throws RpcException {

    }


    /**
     * 获取连接池
     * @param url
     * @return
     */
    BackendConnectionPool getConnectionPool(URL url){
        try {
            //TODO 若不存在，则创建，要保证连接复用
            //查找 URL->pool对应关系
            String address = String.format("%s:%s",url.getHost(),String.valueOf(url.getPort()));
            BackendConnectionPool connectionPool =  createConnPool(address,null);
            // TODO 释放连接 connectionPool.returnObject();
            return connectionPool;
            //return (BusBackendConnection)connectionPool.borrowObject();
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
        String[] temp = StringUtil.split(address, ":");
        if (temp.length > 1) {
            nioFactory.setHost(temp[0]);
            nioFactory.setPort(Integer.valueOf(temp[1]));
        } else {
            nioFactory.setHost(temp[0]);
            nioFactory.setPort(PacketConstant.VENUS_DEFAULT_PORT);
        }

        //TODO 设置connector
        //nioFactory.setConnector(this.getConnector());
        //TODO 设置messageHandler
        //nioFactory.setMessageHandler(getMessageHandler());

        pool = new PollingBackendConnectionPool(address, nioFactory, DEFAULT_POOL_SIZE);
        pool.init();
        return pool;
    }

}
