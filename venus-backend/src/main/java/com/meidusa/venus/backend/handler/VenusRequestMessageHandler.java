package com.meidusa.venus.backend.handler;

import com.meidusa.toolkit.common.bean.util.Initialisable;
import com.meidusa.toolkit.common.bean.util.InitialisationException;
import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.toolkit.net.Connection;
import com.meidusa.toolkit.net.MessageHandler;
import com.meidusa.toolkit.net.util.InetAddressUtil;
import com.meidusa.toolkit.util.StringUtil;
import com.meidusa.toolkit.util.TimeUtil;
import com.meidusa.venus.backend.services.ServiceManager;
import com.meidusa.venus.exception.VenusExceptionCodeConstant;
import com.meidusa.venus.exception.VenusExceptionFactory;
import com.meidusa.venus.io.ServiceFilter;
import com.meidusa.venus.io.network.VenusFrontendConnection;
import com.meidusa.venus.io.packet.*;
import com.meidusa.venus.io.support.VenusStatus;
import com.meidusa.venus.rpc.Invocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * venus远程调用请求消息接收事件处理
 * @author structchen
 *
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class VenusRequestMessageHandler implements MessageHandler<VenusFrontendConnection, Tuple<Long, byte[]>>, Initialisable {

    private static Logger logger = LoggerFactory.getLogger(VenusRequestMessageHandler.class);

    private int maxExecutionThread;

    private int threadLiveTime = 30;

    private boolean executorEnabled = false;

    private boolean executorProtected;

    private boolean useThreadLocalExecutor;

    private Executor executor;

    private VenusExceptionFactory venusExceptionFactory;

    @Autowired
    private ServiceManager serviceManager;

    @Override
    public void init() throws InitialisationException {
        if (executor == null && executorEnabled && !useThreadLocalExecutor && maxExecutionThread > 0) {
            executor = Executors.newFixedThreadPool(maxExecutionThread);
        }
    }

    @Override
    public void handle(final VenusFrontendConnection conn,final Tuple<Long, byte[]> data) {
        //TODO 处理消息请求
        Invocation invocation = parseInvocation(conn, data);
        invoke(conn,invocation);
    }

    /**
     * 调用服务
     * @param conn
     * @param invocation
     */
    void invoke(Connection conn,Invocation invocation){
        //TODO 解析
        //TODO 调用invoker 共通处理由invoker抽象基类实现
    }

    /**
     * 解析请求消息
     * @return
     */
    Invocation parseInvocation(VenusFrontendConnection conn,Tuple<Long, byte[]> data){
        return null;
    }

    public void postMessageBack(Connection conn, VenusRouterPacket routerPacket, AbstractServicePacket source, AbstractServicePacket result) {
        if (routerPacket == null) {
            conn.write(result.toByteBuffer());
        } else {
            routerPacket.data = result.toByteArray();
            conn.write(routerPacket.toByteBuffer());
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

    public int getMaxExecutionThread() {
        return maxExecutionThread;
    }

    public void setMaxExecutionThread(int maxExecutionThread) {
        this.maxExecutionThread = maxExecutionThread;
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
