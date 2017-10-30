package com.meidusa.venus.client.invoker.venus;

import com.meidusa.toolkit.net.Connection;
import com.meidusa.toolkit.net.ConnectionObserver;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

/**
 * client连接监听处理
 * Created by Zhangzhihua on 2017/10/29.
 */
public class VenusClientConnectionObserver implements ConnectionObserver {

    private static Logger logger = LoggerFactory.getLogger(VenusClientConnectionObserver.class);

    //是否开启connect监听处理
    public static boolean isEnableConnectObserver = true;

    /**
     * rpcId-请求&响应映射表
     */
    private Map<String, VenusReqRespWrapper> serviceReqRespMap;

    public Map<String, VenusReqRespWrapper> getServiceReqRespMap() {
        return serviceReqRespMap;
    }

    public void setServiceReqRespMap(Map<String, VenusReqRespWrapper> serviceReqRespMap) {
        this.serviceReqRespMap = serviceReqRespMap;
    }

    @Override
    public void connectionEstablished(Connection conn) {

    }

    @Override
    public void connectionFailed(Connection conn, Exception fault) {
        if(logger.isErrorEnabled()){
            logger.error("connection failed,conn:{},fault:{}.",conn,fault);
        }
    }

    @Override
    public void connectionClosed(Connection conn) {
        if(logger.isErrorEnabled()){
            logger.error("connection close,conn:{}.",conn);
        }
        if(!isEnableConnectObserver){
            return;
        }

        //释放latch wait
        if(conn != null){
            releaseCountDownLatch(conn,null);
        }
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
            logger.error("release countDown latch error.",e);
        }
    }
}
