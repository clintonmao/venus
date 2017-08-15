package com.meidusa.venus.client.cluster;

import com.meidusa.venus.Address;
import com.meidusa.venus.Invocation;
import com.meidusa.venus.Invoker;
import com.meidusa.venus.Result;
import com.meidusa.venus.RpcException;
import com.meidusa.venus.client.invoker.venus.VenusClientInvoker;

import java.util.List;

/**
 * fastfail集群容错invoker
 * Created by Zhangzhihua on 2017/7/31.
 */
public class FastfailClusterInvoker implements Invoker {

    /**
     * 地址列表
     */
    private List<Address> addressList;

    @Override
    public void init() throws RpcException {
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        Invoker invoker = getInvoker(invocation);
        return  invoker.invoke(invocation);
    }

    @Override
    public void destroy() throws RpcException {
    }

    /**
     * 获取invoker
     * @param invocation
     * @return
     */
    Invoker getInvoker(Invocation invocation){
        //TODO
        return new VenusClientInvoker();
    }
}
