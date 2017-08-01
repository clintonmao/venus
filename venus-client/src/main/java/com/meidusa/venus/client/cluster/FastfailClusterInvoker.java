package com.meidusa.venus.client.cluster;

import com.meidusa.venus.*;
import com.meidusa.venus.rpc.Invocation;
import com.meidusa.venus.rpc.Invoker;
import com.meidusa.venus.rpc.Result;
import com.meidusa.venus.rpc.RpcException;

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
    public Result invoke(Invocation invocation) throws RpcException {
        return null;
    }

    @Override
    public void init() throws RpcException {
    }

    @Override
    public void destroy() throws RpcException {
    }
}
