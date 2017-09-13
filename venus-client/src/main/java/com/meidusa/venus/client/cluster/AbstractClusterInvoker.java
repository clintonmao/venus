package com.meidusa.venus.client.cluster;

import com.meidusa.venus.ClusterInvoker;
import com.meidusa.venus.Invoker;

/**
 * 抽象集群容错调用类
 * Created by Zhangzhihua on 2017/9/13.
 */
public abstract class AbstractClusterInvoker implements ClusterInvoker {

    /**
     * 注入invoker
     */
    private Invoker invoker;

    public Invoker getInvoker() {
        return invoker;
    }

    public void setInvoker(Invoker invoker) {
        this.invoker = invoker;
    }
}
