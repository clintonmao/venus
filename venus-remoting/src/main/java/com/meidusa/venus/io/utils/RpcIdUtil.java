package com.meidusa.venus.io.utils;

import com.meidusa.venus.io.packet.AbstractServicePacket;

/**
 * rpcId生成util
 * Created by Zhangzhihua on 2017/9/8.
 */
public class RpcIdUtil {

    /**
     * 获取rpc请求标识
     * @param servicePacket
     * @return
     */
    public static String getRpcId(AbstractServicePacket servicePacket){
        return String.format("%s-%s",String.valueOf(servicePacket.clientId),String.valueOf(servicePacket.clientRequestId));
    }
}
