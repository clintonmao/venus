package com.meidusa.venus.monitor.support;

import com.meidusa.venus.Result;

/**
 * venus监控工具类
 * Created by Zhangzhihua on 2018/3/12.
 */
public class VenusMonitorUtil {

    /**
     * 判断是否操作异常
     * @param detail
     * @return
     */
    public static boolean isExceptionOperation(InvocationDetail detail){
        if(detail.getException() != null){
            return true;
        }
        Result result = detail.getResult();
        return result != null && result.getErrorCode() != 0;
    }

    /**
     * 判断是否为慢操作
     * @param detail
     * @return
     */
    public static boolean isSlowOperation(InvocationDetail detail){
        if(detail.getResponseTime() == null){
            return true;
        }
        long costTime = detail.getResponseTime().getTime() - detail.getInvocation().getRequestTime().getTime();
        return costTime > VenusMonitorConstants.SLOW_COST_TIME;
    }
}
