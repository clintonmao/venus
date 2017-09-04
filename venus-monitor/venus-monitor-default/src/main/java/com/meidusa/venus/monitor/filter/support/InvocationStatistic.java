package com.meidusa.venus.monitor.filter.support;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 调用汇总
 * Created by Zhangzhihua on 2017/9/4.
 */
public class InvocationStatistic {

    private String methodPath;

    private AtomicInteger count = new AtomicInteger(0);

    /**
     * 添加明细并累加统计
     * @param detail
     */
    public void append(InvocationDetail detail){
        //TODO 处理相关计数及时间统计
        count.incrementAndGet();
    }

    /**
     * 重置计数及相关信息
     */
    public void reset(){
        //TODO 重置计数及时间相关
        count = new AtomicInteger(0);
    }

    public String getMethodPath() {
        return methodPath;
    }

    public void setMethodPath(String methodPath) {
        this.methodPath = methodPath;
    }

    public AtomicInteger getCount() {
        return count;
    }

    public void setCount(AtomicInteger count) {
        this.count = count;
    }
}
