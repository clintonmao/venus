package com.meidusa.venus.backend.handler;

import com.meidusa.fastmark.feature.SerializerFeature;
import com.meidusa.toolkit.common.bean.util.Initialisable;
import com.meidusa.toolkit.common.bean.util.InitialisationException;
import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.toolkit.net.MessageHandler;
import com.meidusa.venus.backend.services.ServiceManager;
import com.meidusa.venus.exception.VenusExceptionFactory;
import com.meidusa.venus.io.handler.VenusServerMessageHandler;
import com.meidusa.venus.io.network.VenusFrontendConnection;
import com.meidusa.venus.io.packet.AbstractServicePacket;
import com.meidusa.venus.io.packet.PacketConstant;
import com.meidusa.venus.io.packet.VenusRouterPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * venus服务端服务调用消息处理
 * @author structchen
 *
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class VenusServerInvokerMessageHandler extends VenusServerMessageHandler implements MessageHandler<VenusFrontendConnection, Tuple<Long, byte[]>>, Initialisable {

    private static Logger logger = LoggerFactory.getLogger(VenusServerInvokerMessageHandler.class);

    private static Logger performanceLogger = LoggerFactory.getLogger("venus.backend.performance");

    private static Logger performancePrintResultLogger = LoggerFactory.getLogger("venus.backend.print.result");

    private static Logger performancePrintParamsLogger = LoggerFactory.getLogger("venus.backend.print.params");

    private static SerializerFeature[] JSON_FEATURE = new SerializerFeature[]{SerializerFeature.ShortString,SerializerFeature.IgnoreNonFieldGetter,SerializerFeature.SkipTransientField};



    /*
    private int threadLiveTime = 30;
    private boolean executorEnabled = false;
    private boolean executorProtected;
    private boolean useThreadLocalExecutor;
    private Executor executor;
    */

    private VenusExceptionFactory venusExceptionFactory;

    private ServiceManager serviceManager;

    private VenusServerInvokerHandler venusServerInvokerTask = null;

    @Override
    public void init() throws InitialisationException {
        //初始化业务处理线程池
        /*
        if (executor == null) {
            //executor = new ThreadPoolExecutor(coreThread,maxThread,0,TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(maxQueue),new ThreadPoolExecutor.CallerRunsPolicy());
            executor = new ThreadPoolExecutor(coreThread,maxThread,0,TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(maxQueue),new RejectedExecutionHandler(){
                @Override
                public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                    logger.error("exceed max process,maxThread:{},maxQueue:{}.",maxThread,maxQueue);
                }
            });
        }
        */
    }


    @Override
    public void handle(VenusFrontendConnection conn, Tuple<Long, byte[]> data) {
        if("A".equalsIgnoreCase("B")){
            return;
        }

        /*
        final long waitTime = TimeUtil.currentTimeMillis() - data.left;
        byte serializeType = conn.getSerializeType();
        String sourceIp = conn.getHost();
        */
        byte[] message = data.right;
        int type = AbstractServicePacket.getType(message);
        //TODO 提取分发路由信息，统一serviceRequest报文
        if (PacketConstant.PACKET_TYPE_ROUTER == type) {
            VenusRouterPacket routerPacket = new VenusRouterPacket();
            routerPacket.original = message;
            routerPacket.init(message);
            type = AbstractServicePacket.getType(routerPacket.data);
            /*
            message = routerPacket.data;
            serializeType = routerPacket.serializeType;
            sourceIp = InetAddressUtil.intToAddress(routerPacket.srcIP);
            */
        }

        switch (type) {
            case PacketConstant.PACKET_TYPE_PING:
                super.handle(conn, data);
                break;
            case PacketConstant.PACKET_TYPE_PONG:
                super.handle(conn, data);
                break;
            case PacketConstant.PACKET_TYPE_VENUS_STATUS_REQUEST:
                super.handle(conn, data);
                break;
            case PacketConstant.PACKET_TYPE_SERVICE_REQUEST:
                //远程调用消息处理
                getVenusServerInvokerTask().handle(conn, data);
                break;
            default:
                super.handle(conn, data);
        }

    }

    /**
     * 获取服务调用处理
     * @return
     */
    VenusServerInvokerHandler getVenusServerInvokerTask(){
        if(venusServerInvokerTask == null){
            venusServerInvokerTask = new VenusServerInvokerHandler();
            venusServerInvokerTask.setServiceManager(getServiceManager());
        }
        return venusServerInvokerTask;
    }

    public VenusExceptionFactory getVenusExceptionFactory() {
        return venusExceptionFactory;
    }

    public void setVenusExceptionFactory(VenusExceptionFactory venusExceptionFactory) {
        this.venusExceptionFactory = venusExceptionFactory;
    }

    public ServiceManager getServiceManager() {
        return serviceManager;
    }

    public void setServiceManager(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }
}
