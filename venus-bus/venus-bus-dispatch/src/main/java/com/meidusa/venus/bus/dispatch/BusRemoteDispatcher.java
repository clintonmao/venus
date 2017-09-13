package com.meidusa.venus.bus.dispatch;

import com.meidusa.venus.*;
import com.meidusa.venus.backend.ErrorPacketWrapperException;
import com.meidusa.venus.bus.BusInvocation;
import com.meidusa.venus.bus.network.BusFrontendConnection;
import com.meidusa.venus.bus.registry.ServiceRegisterManager;
import com.meidusa.venus.client.cluster.ClusterInvokerFactory;
import com.meidusa.venus.client.invoker.venus.VenusClientInvoker;
import com.meidusa.venus.client.router.Router;
import com.meidusa.venus.client.router.condition.ConditionRouter;
import com.meidusa.venus.exception.VenusExceptionCodeConstant;
import com.meidusa.venus.io.packet.AbstractServicePacket;
import com.meidusa.venus.io.packet.ErrorPacket;
import com.meidusa.venus.io.packet.ServiceAPIPacket;
import com.meidusa.venus.io.packet.ServicePacketBuffer;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * 消息远程分发处理
 * Created by Zhangzhihua on 2017/9/1.
 */
public class BusRemoteDispatcher implements Dispatcher{

    private static Logger logger = LoggerFactory.getLogger(BusRemoteDispatcher.class);

    private ServiceRegisterManager serviceRegisterManager;

    private ClusterInvoker clusterInvoker;

    //TODO 构造条件
    private Router router = new ConditionRouter();

    @Override
    public void init() throws RpcException {

    }

    @Override
    public Result invoke(Invocation invocation, URL url) throws RpcException {
        //寻址
        List<URL> urlList = lookup(invocation);

        //TODO 路由规则过滤/版本号校验 router.filte
        //路由规则过滤
        urlList = router.filte(urlList, invocation);

        //集群容错分发调用
        Result result = getClusterInvoker().invoke(invocation,urlList);
        return result;
    }

    @Override
    public void destroy() throws RpcException {

    }

    /**
     * 寻址
     * @param invocation
     * @return
     */
    List<URL> lookup(Invocation invocation){
        if(!isDynamicLookup()){
            return this.lookupByStatic(invocation);
        }else{
            return this.lookupByDynamic(invocation);
        }
    }

    /**
     * 判断是本地静态还是注册中心动态寻址
     * @return
     */
    boolean isDynamicLookup(){
        //TODO
        return true;
    }

    /**
     *
     * @param invocation
     * @return
     */
    List<URL> lookupByStatic(Invocation invocation){
        return Collections.emptyList();
    }

    /**
     * 查找服务地址 TODO 静态查找、动态查找
     * @return
     */
    List<URL> lookupByDynamic(Invocation invocation){
        BusInvocation busInvocation = (BusInvocation)invocation;
        BusFrontendConnection srcConn = busInvocation.getSrcConn();
        ServicePacketBuffer packetBuffer = busInvocation.getPacketBuffer();
        String serviceName = busInvocation.getServiceName();
        try {
            List<URL> list = serviceRegisterManager.lookup(serviceName);

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
            //错误返回，统一返回，fixed
            //srcConn.write(error.toByteBuffer());
            logger.error("error when invoke", e);
            throw new ErrorPacketWrapperException(error);
        }
    }

    /**
     * 获取集群容错invoker
     * @return
     */
    ClusterInvoker getClusterInvoker(){
        //TODO 根据配置获取clusterInvoker
        if(clusterInvoker == null){
            clusterInvoker =  ClusterInvokerFactory.getClusterInvoker();
            //TODO 根据配置加载invoker
            clusterInvoker.setInvoker(new VenusClientInvoker());
        }
        return clusterInvoker;
    }

}
