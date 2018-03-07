package com.meidusa.venus.io.network;

import com.meidusa.toolkit.util.TimeUtil;
import com.meidusa.venus.io.packet.PingPacket;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.SocketChannel;

/**
 * venus4实现，扩展心跳检查
 * Created by Zhangzhihua on 2018/3/7.
 */
public class Venus4BackendConnection extends VenusBackendConnection {
    private static Logger logger = LoggerFactory.getLogger(Venus4BackendConnection.class);
    private long lastPing;
    private long lastPong;
    private long PING_INTERVAL = 15000;

    public Venus4BackendConnection(SocketChannel channel) {
        super(channel);
    }

    protected void idleCheck() {
        //避免在认证期间发送ping数据包
        if(this.isAuthenticated()){
            if (isIdleTimeout()) {
                if(logger.isWarnEnabled()){
                    logger.warn("conn="+this.host+":"+ this.port+ " ping/pong timeout="+(lastPing - lastPong)+"!");
                }
                close();
            }else{
                PingPacket ping = new PingPacket();
                this.setLastPing(TimeUtil.currentTimeMillis());
                this.write(ping.toByteBuffer());
            }
        }
    }

    public boolean isIdleTimeout() {
        return lastPing - lastPong > PING_INTERVAL;
    }

    public long getLastPing() {
        return lastPing;
    }

    public void setLastPing(long lastPing) {
        this.lastPing = lastPing;
    }

    public long getLastPong() {
        return lastPong;
    }

    public void setLastPong(long lastPong) {
        this.lastPong = lastPong;
    }

}
