package com.meidusa.venus.client.cluster;

import com.meidusa.venus.ClientInvocation;
import com.meidusa.venus.ClusterInvoker;
import com.meidusa.venus.Invoker;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.client.cluster.loadbalance.Loadbalance;
import com.meidusa.venus.client.cluster.loadbalance.RandomLoadbalance;
import com.meidusa.venus.client.cluster.loadbalance.RoundLoadbalance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 集群容错调用抽象类
 * Created by Zhangzhihua on 2017/9/13.
 */
public abstract class AbstractClusterInvoker implements ClusterInvoker {

    //负载策略-随机
    private static String LB_RANDOM = "random";
    //负载策略-轮询
    private static String LB_ROUND = "round";

    private RandomLoadbalance randomLoadbanlance = new RandomLoadbalance();

    private RoundLoadbalance roundLoadbanlance = new RoundLoadbalance();

    //服务路径-randomlb映射表
    private static Map<String,RandomLoadbalance> randomLbMap = new ConcurrentHashMap<String,RandomLoadbalance>();

    //服务路径-roundlb映射表
    private static Map<String,RoundLoadbalance> roundLbMap = new ConcurrentHashMap<String,RoundLoadbalance>();

    protected Invoker invoker = null;

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
    Loadbalance getLoadbanlance(String lb, ClientInvocation clientInvocation){
        String servicePath = clientInvocation.getServicePath();
        if(LB_RANDOM.equals(lb)){
            if(randomLbMap.get(servicePath) == null){
                randomLbMap.put(servicePath,randomLoadbanlance);
            }
            return randomLbMap.get(servicePath);
        }else if(LB_ROUND.equals(lb)){
            if(roundLbMap.get(servicePath) == null){
                roundLbMap.put(servicePath,roundLoadbanlance);
            }
            return roundLbMap.get(servicePath);
        }else{
            throw new RpcException("unspport loadbanlance policy.");
        }
    }
}
