package com.meidusa.venus.bus.network;

import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import com.meidusa.toolkit.net.FrontendConnection;
import com.meidusa.venus.bus.handler.RetryMessageHandler;
import com.meidusa.venus.io.network.VenusFrontendConnectionFactory;

/**
 * Bus前端连接工厂
 * 
 * @author structchen
 * 
 */
public class BusFrontendConnectionFactory extends VenusFrontendConnectionFactory implements InitializingBean {
    private static Logger logger = LoggerFactory.getLogger(BusFrontendConnectionFactory.class);

    private RetryMessageHandler retryMessageHandler;

    protected FrontendConnection getConnection(SocketChannel channel) {
        BusFrontendConnection conn = new BusFrontendConnection(channel);
        conn.setRequestHandler(getMessageHandler());
        conn.setAuthenticateProvider(getAuthenticateProvider());
        conn.setRetryHandler(retryMessageHandler);
        return conn;
    }

    public RetryMessageHandler getRetryMessageHandler() {
        return retryMessageHandler;
    }

    public void setRetryMessageHandler(RetryMessageHandler retryMessageHandler) {
        this.retryMessageHandler = retryMessageHandler;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("frontend socket receiveBuffer=" + this.getReceiveBufferSize() + "K, sentBuffer=" + this.getSendBufferSize() + "K");
    }
}
