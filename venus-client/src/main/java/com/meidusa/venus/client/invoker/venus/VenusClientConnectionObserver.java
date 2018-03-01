package com.meidusa.venus.client.invoker.venus;

import com.meidusa.toolkit.net.BackendConnection;
import com.meidusa.toolkit.net.Connection;
import com.meidusa.toolkit.net.ConnectionObserver;
import com.meidusa.venus.ConnectionFactory;
import com.meidusa.venus.Invoker;
import com.meidusa.venus.support.VenusContext;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.slf4j.Logger;

/**
 * client连接监听处理
 * Created by Zhangzhihua on 2017/10/29.
 */
public class VenusClientConnectionObserver implements ConnectionObserver {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

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
                String ipAddress = getTargetAddress((BackendConnection)conn);
                logger.warn("connection closed,target:[{}].",ipAddress);
            }else{
                logger.warn("connection closed,conn:{}.",conn);
            }
        }

        try {
            if(conn != null){
                VenusContext context = VenusContext.getInstance();
                //释放连接相关资源
                ConnectionFactory connectionFactory = context.getConnectionFactory();
                if(connectionFactory != null){
                    connectionFactory.releaseConnection(conn);
                }
            }
        } catch (Exception e) {
            exceptionLogger.error("#########release conn rela resource failed.",e);
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

}
