package com.meidusa.venus.io.network;

import java.nio.channels.SocketChannel;

import com.meidusa.toolkit.net.FrontendConnection;

public class VenusBackendFrontendConnectionFactory extends VenusFrontendConnectionFactory {

    protected FrontendConnection getConnection(SocketChannel channel) {
        VenusBackendFrontendConnection conn = new VenusBackendFrontendConnection(channel);
        conn.setRequestHandler(getMessageHandler());
        conn.setAuthenticateProvider(getAuthenticateProvider());
        return conn;
    }
}
