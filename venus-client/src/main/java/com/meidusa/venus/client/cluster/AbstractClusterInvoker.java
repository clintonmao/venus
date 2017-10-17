package com.meidusa.venus.client.cluster;

import com.meidusa.venus.ClusterInvoker;
import com.meidusa.venus.Invoker;
import com.meidusa.venus.RpcException;
import com.meidusa.venus.client.cluster.loadbanlance.Loadbanlance;
import com.meidusa.venus.client.cluster.loadbanlance.RandomLoadbanlance;
import com.meidusa.venus.client.cluster.loadbanlance.RoundLoadbanlance;

/**
 * 抽象集群容错调用类
 * Created by Zhangzhihua on 2017/9/13.
 */
public abstract class AbstractClusterInvoker implements ClusterInvoker {

    //负载策略-随机
    private static String LB_RANDOM = "random";
    //负载策略-轮询
    private static String LB_ROUND = "round";

    private RandomLoadbanlance randomLoadbanlance = new RandomLoadbanlance();

    private RoundLoadbanlance roundLoadbanlance = new RoundLoadbanlance();

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

    /**
     * 获取loadbanlance
     * @return
     */
    Loadbanlance getLoadbanlance(String lb){
        if(LB_RANDOM.equals(lb)){
            return randomLoadbanlance;
        }else if(LB_ROUND.equals(lb)){
            return roundLoadbanlance;
        }else{
            throw new RpcException("unspport loadbanlance policy.");
        }
    }
}
