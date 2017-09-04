package com.meidusa.venus.monitor.filter.support;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 调用汇总
 * Created by Zhangzhihua on 2017/9/4.
 */
public class InvocationStatistic {

    private String servicePath;

    private AtomicInteger count;

    public String getServicePath() {
        return servicePath;
    }

    public void setServicePath(String servicePath) {
        this.servicePath = servicePath;
    }

    public AtomicInteger getCount() {
        return count;
    }

    public void setCount(AtomicInteger count) {
        this.count = count;
    }

    public void reset(){
        //TODO 重置计数
    }
}
