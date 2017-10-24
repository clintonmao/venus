package com.meidusa.venus.backend.handler;

import com.meidusa.fastmark.feature.SerializerFeature;
import com.meidusa.toolkit.common.bean.util.Initialisable;
import com.meidusa.toolkit.common.bean.util.InitialisationException;
import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.toolkit.net.MessageHandler;
import com.meidusa.toolkit.net.util.InetAddressUtil;
import com.meidusa.toolkit.util.TimeUtil;
import com.meidusa.venus.backend.invoker.VenusServerInvokerTask;
import com.meidusa.venus.backend.services.ServiceManager;
import com.meidusa.venus.exception.VenusExceptionFactory;
import com.meidusa.venus.io.handler.VenusServerMessageHandler;
import com.meidusa.venus.io.network.VenusFrontendConnection;
import com.meidusa.venus.io.packet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.*;

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

    private int coreThread = 20;

    private int maxThread = 20;

    private int maxQueue = 10000;

    private int threadLiveTime = 30;

    private boolean executorEnabled = false;

    private boolean executorProtected;

    private boolean useThreadLocalExecutor;

    private Executor executor;

    private VenusExceptionFactory venusExceptionFactory;

    private ServiceManager serviceManager;

    @Override
    public void init() throws InitialisationException {
        /*
        if (executor == null && executorEnabled && !useThreadLocalExecutor && maxExecutionThread > 0) {
            executor = Executors.newFixedThreadPool(maxExecutionThread);
        }
        */
        //初始化业务处理线程池
        if (executor == null) {
            //executor = Executors.newFixedThreadPool(maxExecutionThread);
            //executor = new ThreadPoolExecutor(coreThread,maxThread,0,TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(maxQueue),new ThreadPoolExecutor.CallerRunsPolicy());
            executor = new ThreadPoolExecutor(coreThread,maxThread,0,TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(maxQueue),new RejectedExecutionHandler(){
                @Override
                public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                    logger.error("exceed max process,maxThread:{},maxQueue:{}.",maxThread,maxQueue);
                }
            });
        }
    }


    @Override
    public void handle(VenusFrontendConnection conn, Tuple<Long, byte[]> data) {
        final long waitTime = TimeUtil.currentTimeMillis() - data.left;
        byte[] message = data.right;

        int type = AbstractServicePacket.getType(message);
        byte serializeType = conn.getSerializeType();
        String sourceIp = conn.getHost();
        //TODO 提取分发路由信息，统一serviceRequest报文
        if (PacketConstant.PACKET_TYPE_ROUTER == type) {
            VenusRouterPacket routerPacket = new VenusRouterPacket();
            routerPacket.original = message;
            routerPacket.init(message);
            type = AbstractServicePacket.getType(routerPacket.data);
            message = routerPacket.data;
            serializeType = routerPacket.serializeType;
            sourceIp = InetAddressUtil.intToAddress(routerPacket.srcIP);
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
                //TODO 多线程调用handler，并处理异常问题，如队列满等
                VenusServerInvokerTask venusServerInvokerTask = new VenusServerInvokerTask(conn, data);
                venusServerInvokerTask.setServiceManager(getServiceManager());
                executor.execute(venusServerInvokerTask);
                break;
            default:
                super.handle(conn, data);
        }

    }

    public boolean isExecutorEnabled() {
        return executorEnabled;
    }

    public void setExecutorEnabled(boolean executorEnabled) {
        this.executorEnabled = executorEnabled;
    }

    public boolean isExecutorProtected() {
        return executorProtected;
    }

    public boolean isUseThreadLocalExecutor() {
        return useThreadLocalExecutor;
    }

    public void setUseThreadLocalExecutor(boolean useThreadLocalExecutor) {
        this.useThreadLocalExecutor = useThreadLocalExecutor;
    }

    public void setExecutorProtected(boolean executorProtected) {
        this.executorProtected = executorProtected;
    }

    public int getThreadLiveTime() {
        return threadLiveTime;
    }

    public void setThreadLiveTime(int threadLiveTime) {
        this.threadLiveTime = threadLiveTime;
    }

    public int getCoreThread() {
        return coreThread;
    }

    public void setCoreThread(int coreThread) {
        this.coreThread = coreThread;
    }

    public int getMaxThread() {
        return maxThread;
    }

    public void setMaxThread(int maxThread) {
        this.maxThread = maxThread;
    }

    public int getMaxQueue() {
        return maxQueue;
    }

    public void setMaxQueue(int maxQueue) {
        this.maxQueue = maxQueue;
    }

    public VenusExceptionFactory getVenusExceptionFactory() {
        return venusExceptionFactory;
    }

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
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
        //TODO 具体参数功能
