package com.meidusa.venus.bus.handler;

import com.meidusa.venus.bus.network.BusFrontendConnection;
import com.meidusa.venus.io.packet.VenusRouterPacket;

/**
 * 消息重试处理,诸如:后端服务不可用的 时候,将有默认3次尝试请求.每次间隔1秒的机制,重新对虚拟连接池发起请求,如果都失败将返回异常数据包给客户端.
 * 
 * @author structchen
 * 
 */
public interface RetryMessageHandler {

    /**
     * 增加一个重试路由数据包
     *
     * @param conn
     * @param data
     */
    void addRetry(BusFrontendConnection conn, VenusRouterPacket data);

}
