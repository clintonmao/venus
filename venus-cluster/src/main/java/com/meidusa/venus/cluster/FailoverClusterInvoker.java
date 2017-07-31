package com.meidusa.venus.cluster;

import com.meidusa.venus.client.RpcException;
import com.meidusa.venus.Invocation;
import com.meidusa.venus.client.invoker.Invoker;
import com.meidusa.venus.Result;

/**
 * failover集群容错invoker
 * Created by Zhangzhihua on 2017/7/31.
 */
public class FailoverClusterInvoker implements Invoker {

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        return null;
    }
}
