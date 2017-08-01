package com.meidusa.venus.cluster;

import com.meidusa.venus.Address;
import com.meidusa.venus.client.RpcException;
import com.meidusa.venus.Invocation;
import com.meidusa.venus.client.invoker.Invoker;
import com.meidusa.venus.Result;
import com.meidusa.venus.client.invoker.venus.VenusInvoker;
import com.meidusa.venus.loadbanlance.Loadbanlance;
import com.meidusa.venus.loadbanlance.random.RandomLoadbanlance;

import java.util.List;

/**
 * failover集群容错invoker
 * Created by Zhangzhihua on 2017/7/31.
 */
public class FailoverClusterInvoker implements Invoker {

    /**
     * 地址列表
     */
    private List<Address> addressList;

    /**
     * retry次数 TODO 读取配置
     */
    private int retry = 3;

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        for(int i=0;i<retry;i++){
            try {
                //查找地址
                Address address = getLoadbanlance().select(addressList);
                //获取对应协议的invoker
                Invoker invoker = getInvoker(invocation);
                // 调用
                return  invoker.invoke(invocation);
            } catch (RpcException e) {
                //TODO 异常日志处理
            }
        }
        throw new RpcException(String.format("serivce %,method % invoke failed with % tries.","","",""));
    }

    /**
     * 获取invoker
     * @param invocation
     * @return
     */
    Invoker getInvoker(Invocation invocation){
        //TODO
        return new VenusInvoker();
    }

    /**
     * 获取loadbanlance
     * @return
     */
    Loadbanlance getLoadbanlance(){
        return new RandomLoadbanlance();
    }



}
