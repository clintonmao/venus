package com.meidusa.venus.io.utils;

import com.meidusa.venus.io.packet.AbstractServicePacket;

/**
 * rpcId生成util
 * Created by Zhangzhihua on 2017/9/8.
 */
public class RpcIdUtil {

    /**
     * 获取rpc请求标识
     * @param clientId
     * @param clientRequestId
     * @return
     */
    public static String getRpcId(int clientId,long clientRequestId){
        String rpcId = new StringBuilder().append(clientId).append("-").append(clientRequestId).toString();
        return rpcId;
    }

    /**
     * 获取rpc请求标识
     * @param servicePacket
     * @return
     */
    public static String getRpcId(AbstractServicePacket servicePacket){
        String rpcId = new StringBuilder().append(servicePacket.clientId).append("-").append(servicePacket.clientRequestId).toString();
        return rpcId;
    }
}
