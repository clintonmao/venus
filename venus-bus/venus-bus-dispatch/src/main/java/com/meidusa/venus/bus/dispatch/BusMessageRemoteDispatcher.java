package com.meidusa.venus.bus.dispatch;

import com.meidusa.venus.Invocation;
import com.meidusa.venus.Result;
import com.meidusa.venus.RpcException;
import com.meidusa.venus.URL;
import com.meidusa.venus.bus.BusInvocation;
import com.meidusa.venus.bus.network.BusFrontendConnection;
import com.meidusa.venus.bus.service.ServiceRemoteManager;
import com.meidusa.venus.exception.VenusExceptionCodeConstant;
import com.meidusa.venus.io.packet.AbstractServicePacket;
import com.meidusa.venus.io.packet.ErrorPacket;
import com.meidusa.venus.io.packet.ServiceAPIPacket;
import com.meidusa.venus.io.packet.ServicePacketBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 消息远程分发处理
 * Created by Zhangzhihua on 2017/9/1.
 */
public class BusMessageRemoteDispatcher implements Dispatcher{

    private static Logger logger = LoggerFactory.getLogger(BusMessageRemoteDispatcher.class);

    private BusMessageClusterFailoverDispatcher busMessageClusterFailoverDispatcher;

    private ServiceRemoteManager remoteManager;

    @Override
    public Result dispatch(Invocation invocation, URL url) throws RpcException {
        //寻址
        List<URL> urlList = lookup(invocation);

        //TODO 路由规则过滤 router.filte

        //分发
        Result result = busMessageClusterFailoverDispatcher.dispatch(invocation,urlList);
        return result;
    }

    /**
     * 查找服务地址 TODO 静态查找、动态查找
     * @return
     */
    List<URL> lookup(Invocation invocation){
        BusInvocation busInvocation = (BusInvocation)invocation;
        BusFrontendConnection srcConn = busInvocation.getSrcConn();
        ServicePacketBuffer packetBuffer = busInvocation.getPacketBuffer();
        String serviceName = busInvocation.getServiceName();
        try {
            List<URL> list = remoteManager.lookup(serviceName);

            // service not found
            if (list == null || list.size() == 0) {
                ServiceAPIPacket apiPacket = new ServiceAPIPacket();
                packetBuffer.reset();
                apiPacket.init(packetBuffer);
                ErrorPacket error = new ErrorPacket();
                AbstractServicePacket.copyHead(apiPacket, error);
                error.errorCode = VenusExceptionCodeConstant.SERVICE_NOT_FOUND;
                error.message = "service not found :" + serviceName;
                //错误返回
                srcConn.write(error.toByteBuffer());

                throw new RpcException("service not found");
            }
            return list;
        } catch (Exception e) {
            ServiceAPIPacket apiPacket = new ServiceAPIPacket();
            packetBuffer.reset();
            apiPacket.init(packetBuffer);

            ErrorPacket error = new ErrorPacket();
            AbstractServicePacket.copyHead(apiPacket, error);
            error.errorCode = VenusExceptionCodeConstant.SERVICE_UNAVAILABLE_EXCEPTION;
            error.message = e.getMessage();
            //错误返回
            srcConn.write(error.toByteBuffer());
            logger.error("error when invoke", e);

            throw new RpcException("error when invoke.",e);
        }
    }
}
