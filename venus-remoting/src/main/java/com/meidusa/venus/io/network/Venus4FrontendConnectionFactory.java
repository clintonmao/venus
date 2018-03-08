package com.meidusa.venus.io.network;

import java.nio.channels.SocketChannel;

import com.meidusa.toolkit.net.FrontendConnection;

public class Venus4FrontendConnectionFactory extends VenusFrontendConnectionFactory {

    protected FrontendConnection getConnection(SocketChannel channel) {
        Venus4FrontendConnection conn = new Venus4FrontendConnection(channel);
        conn.setRequestHandler(getMessageHandler());
        conn.setAuthenticateProvider(getAuthenticateProvider());
        return conn;
    }
}
