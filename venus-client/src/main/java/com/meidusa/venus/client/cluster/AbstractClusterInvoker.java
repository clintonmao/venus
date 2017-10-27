package com.meidusa.venus.client.cluster;

import com.meidusa.venus.ClientInvocation;
import com.meidusa.venus.ClusterInvoker;
import com.meidusa.venus.Invoker;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.client.cluster.loadbanlance.Loadbanlance;
import com.meidusa.venus.client.cluster.loadbanlance.RandomLoadbanlance;
import com.meidusa.venus.client.cluster.loadbanlance.RoundLoadbanlance;

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

    private RandomLoadbanlance randomLoadbanlance = new RandomLoadbanlance();

    private RoundLoadbanlance roundLoadbanlance = new RoundLoadbanlance();

    //服务路径-randomlb映射表
    private static Map<String,RandomLoadbanlance> randomLbMap = new ConcurrentHashMap<String,RandomLoadbanlance>();

    //服务路径-roundlb映射表
    private static Map<String,RoundLoadbanlance> roundLbMap = new ConcurrentHashMap<String,RoundLoadbanlance>();


    /**
     * 相应协议的invoker调用实现
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
    Loadbanlance getLoadbanlance(String lb, ClientInvocation clientInvocation){
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
