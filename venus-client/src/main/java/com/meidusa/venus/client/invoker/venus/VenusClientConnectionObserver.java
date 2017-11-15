package com.meidusa.venus.client.invoker.venus;

import com.meidusa.toolkit.net.BackendConnection;
import com.meidusa.toolkit.net.Connection;
import com.meidusa.toolkit.net.ConnectionObserver;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * client连接监听处理
 * Created by Zhangzhihua on 2017/10/29.
 */
public class VenusClientConnectionObserver implements ConnectionObserver {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    /**
     * rpcId-请求&响应映射表
     */
    private Map<String, VenusReqRespWrapper> serviceReqRespMap;

    @Override
    public void connectionEstablished(Connection conn) {
        if(logger.isInfoEnabled()){
            if(conn != null && conn instanceof BackendConnection){
                logger.info("connection established,target:[{}].",getTargetAddress((BackendConnection)conn));
            }else{
                logger.info("connection established,conn:{}.",conn);
            }
        }
    }

    @Override
    public void connectionFailed(Connection conn, Exception fault) {
        if(exceptionLogger.isErrorEnabled()){
            if(conn != null && conn instanceof BackendConnection){
                exceptionLogger.error("connection failed,target:[{}],fault:{}.",getTargetAddress((BackendConnection)conn),fault);
            }else{
                exceptionLogger.error("connection failed,conn:{},fault:{}.",conn,fault);
            }
        }
    }

    @Override
    public void connectionClosed(Connection conn) {
        if(logger.isWarnEnabled()){
            if(conn != null && conn instanceof BackendConnection){
                logger.warn("connection closed,target:[{}].",getTargetAddress((BackendConnection)conn));
            }else{
                logger.warn("connection closed,conn:{}.",conn);
            }
        }

        //释放latch wait
        if(conn != null){
            releaseCountDownLatch(conn,null);
        }
    }

    /**
     * 获取连接地址
     * @param backendConnection
     * @return
     */
    String getTargetAddress(BackendConnection backendConnection){
        StringBuilder builder = new StringBuilder();
        builder.append(backendConnection.getHost());
        builder.append(":");
        builder.append(backendConnection.getPort());
        return builder.toString();
    }

    /**
     * 释放latch wait
     * @param conn
     * @param fault
     */
    void releaseCountDownLatch(Connection conn, Exception fault){
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

    public Map<String, VenusReqRespWrapper> getServiceReqRespMap() {
        return serviceReqRespMap;
    }

    public void setServiceReqRespMap(Map<String, VenusReqRespWrapper> serviceReqRespMap) {
        this.serviceReqRespMap = serviceReqRespMap;
    }
}
