package com.meidusa.venus.io.network;

import com.meidusa.toolkit.net.BackendConnection;

import java.nio.channels.SocketChannel;

/**
 * venus4实现，返回Venus4BackendConnection
 * Created by Zhangzhihua on 2018/3/7.
 */
public class Venus4BackendConnectionFactory extends VenusBackendConnectionFactory {

    @Override
    protected BackendConnection create(SocketChannel channel) {
        Venus4BackendConnection c = new Venus4BackendConnection(channel);
        c.setAuthenticator(getAuthenticator());
        c.setResponseMessageHandler(getMessageHandler());
        return c;
    }

}
