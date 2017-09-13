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
 * bus消息分发处理
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
     * 转发消息 TODO 监控上报
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

        //分发消息 TODO 版本号校验
        /*
        if (tuple.left.contains(serviceVersion)) {
        }
        */
        BusBackendConnection remoteConn = null;
        try {
            remoteConn = getBackendConnection(url);//TODO 更改连接获取方式 tuple.right.borrowObject();
            routerPacket.backendRequestID = remoteConn.getNextRequestID();
            remoteConn.addRequest(routerPacket.backendRequestID, routerPacket.frontendConnectionID, routerPacket.frontendRequestID);
            srcConn.addUnCompleted(routerPacket.frontendRequestID, routerPacket);
            //分发
            remoteConn.write(VenusRouterPacket.toByteBuffer(routerPacket));
            VenusTracerUtil.logRouter(traceId, apiName, srcConn.getInetAddress().getHostAddress(), remoteConn.getHost()+":"+remoteConn.getPort());

            return null;//TODO 返回值
        } catch (Exception e) {
            srcConn.addUnCompleted(routerPacket.frontendRequestID, routerPacket);
            srcConn.getRetryHandler().addRetry(srcConn, routerPacket);
            return null;//TODO 返回值
        }
        /* TODO 创建连接时异常情况
        catch(InvalidVirtualPoolException e){
            ServiceAPIPacket apiPacket = new ServiceAPIPacket();
            packetBuffer.reset();
            apiPacket.init(packetBuffer);
            ErrorPacket error = new ErrorPacket();
            AbstractServicePacket.copyHead(apiPacket, error);
            error.errorCode = VenusExceptionCodeConstant.SERVICE_UNAVAILABLE_EXCEPTION;
            error.message = e.getMessage();
            //错误返回
            srcConn.write(error.toByteBuffer());
            return null;//TODO 返回值
        }
        */
        finally {
            if (remoteConn != null) {
                //TODO 连接释放
                //tuple.right.returnObject(remoteConn);
            }
        }

        //错误返回
        // srcConn.write(error.toByteBuffer());
        //return null;//TODO 返回值
    }

    @Override
    public void destroy() throws RpcException {

    }


    /**
     * 获取连接
     * @param url
     * @return
     */
    BusBackendConnection getBackendConnection(URL url){
        try {
            //TODO 若不存在，则创建，要保证连接复用
            String address = String.format("%s:%s",url.getHost(),String.valueOf(url.getPort()));
            BackendConnectionPool connectionPool =  createRealPool(address,null);
            return (BusBackendConnection)connectionPool.borrowObject();
        } catch (Exception e) {
            throw new RpcException("get backend connection failed.",e);
        }
    }

    /**
     * 创建连接 TODO 地址失效及相关情况处理
     * @param address
     * @param authenticator
     * @return
     */
    BackendConnectionPool createRealPool(String address, @SuppressWarnings("rawtypes") Authenticator authenticator) {
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
