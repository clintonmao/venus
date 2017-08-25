package com.meidusa.venus;

import java.util.HashMap;
import java.util.Map;

/**
 * venus线程上下文信息
 * Created by Zhangzhihua on 2017/8/24.
 */
public class VenusContext {

    final static ThreadLocal<Map<String,Object>> mapThreadLocal = new ThreadLocal<Map<String, Object>>();

    //athena事务id
    public final static String ATHENA_TRANSACTION_ID = "athenaTransactionId";
    //client请求报文长度
    public final static String CLIENT_OUTPUT_SIZE = "clientOutputSize";
    //client接收报文长度
    public final static String CLIENT_INPUT_SIZE = "clientInputSize";


    /**
     * 设置上下文属性
     * @param key
     * @param value
     */
    public static void set(String key,Object value){
        if(mapThreadLocal.get() == null){
            mapThreadLocal.set(new HashMap<String,Object>());
        }
        mapThreadLocal.get().put(key,value);
    }

    /**
     * 获取属性值
     * @param key
     * @return
     */
    public static Object get(String key){
        if(mapThreadLocal.get() == null){
            return null;
        }
        return mapThreadLocal.get().get(key);
    }

}
