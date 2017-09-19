package com.meidusa.venus.client.filter.limit;

import com.meidusa.venus.*;
import com.meidusa.venus.ClientInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 流控基类
 * Created by Zhangzhihua on 2017/8/30.
 */
public class BaseLimitFilter {

    private static Logger logger = LoggerFactory.getLogger(BaseLimitFilter.class);

    //流控类型-并发数
    static final String LIMIT_TYPE_ACTIVE = "active_limit";
    //流控类型-TPS
    static final String LIMIT_TYPE_TPS = "tps_limit";

    /**
     * 获取方法标识路径
     * @param invocation
     * @param url
     * @return
     */
    String getMethodPath(ClientInvocation invocation, URL url){
        //TODO client/server分别处理
        String methodPath = String.format(
                "%s/%s?version=%s&method=%s",
                invocation.getMethod().getDeclaringClass().getName(),
                invocation.getServiceName(),
                "0.0.0",
                invocation.getMethod().getName()
        );
        logger.info("methodPath:{}.", methodPath);
        return methodPath;
    }

    /**
     * 获取流控类型
     * @param invocation
     * @param url
     * @return
     */
    String getLimitType(ClientInvocation invocation, URL url){
        //TODO 获取流控类型
        return LIMIT_TYPE_ACTIVE;
    }

}
