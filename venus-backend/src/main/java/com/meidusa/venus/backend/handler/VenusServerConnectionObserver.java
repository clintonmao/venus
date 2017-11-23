package com.meidusa.venus.backend.handler;

import com.meidusa.toolkit.net.Connection;
import com.meidusa.toolkit.net.ConnectionObserver;
import com.meidusa.toolkit.net.FrontendConnection;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.slf4j.Logger;

/**
 * backend连接监听处理
 * Created by Zhangzhihua on 2017/10/29.
 */
public class VenusServerConnectionObserver implements ConnectionObserver {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    @Override
    public void connectionEstablished(Connection conn) {
        if(logger.isDebugEnabled()){
            if(conn != null && conn instanceof FrontendConnection){
                logger.info("connection established,target:[{}].",getTargetAddress((FrontendConnection)conn));
            }else{
                logger.info("connection established,conn:{}.",conn);
            }
        }
    }

    @Override
    public void connectionFailed(Connection conn, Exception fault) {
        if(exceptionLogger.isErrorEnabled()){
            if(conn != null && conn instanceof FrontendConnection){
                exceptionLogger.error("connection failed,target:[{}],fault:{}.",getTargetAddress((FrontendConnection)conn),fault);
            }else{
                exceptionLogger.error("connection failed,conn:{},fault:{}.",conn,fault);
            }
        }
    }

    @Override
    public void connectionClosed(Connection conn) {
        if(logger.isDebugEnabled()){
            if(conn != null && conn instanceof FrontendConnection){
                logger.warn("connection closed,target:[{}].",getTargetAddress((FrontendConnection)conn));
            }else{
                logger.warn("connection closed,conn:{}.",conn);
            }
        }

    }

    /**
     * 获取连接地址
     * @param frontendConnection
     * @return
     */
    String getTargetAddress(FrontendConnection frontendConnection){
        StringBuilder builder = new StringBuilder();
        builder.append(frontendConnection.getHost());
        builder.append(":");
        builder.append(frontendConnection.getPort());
        return builder.toString();
    }

}
