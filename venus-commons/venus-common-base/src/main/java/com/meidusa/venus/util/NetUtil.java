package com.meidusa.venus.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Net util
 * Created by Zhangzhihua on 2017/8/17.
 */
public class NetUtil {

    /**
     * 获取本机ip
     * @return
     */
    public static String getLocalIp(){
        try {
            InetAddress addr = InetAddress.getLocalHost();
            String localIp =  addr.getHostAddress();
            return localIp;
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
