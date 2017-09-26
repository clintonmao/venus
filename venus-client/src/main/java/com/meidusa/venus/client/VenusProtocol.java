package com.meidusa.venus.client;

import com.meidusa.venus.io.serializer.SerializerFactory;

/**
 * venus协议，初始化protocol、remoting相关定义
 * Created by Zhangzhihua on 2017/9/26.
 */
public class VenusProtocol {

    private static boolean isInited = false;

    public static void init(){
        if(!isInited){
            //初始化序列化配置
            SerializerFactory.init();
        }
    }
}
