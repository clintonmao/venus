package com.meidusa.venus.client.cluster;

import com.meidusa.venus.*;
import com.meidusa.venus.client.invoker.venus.VenusClientInvoker;

import java.util.List;

/**
 * fastfail集群容错invoker
 * Created by Zhangzhihua on 2017/7/31.
 */
public class ClusterFastfailInvoker extends AbstractClusterInvoker implements ClusterInvoker {

    @Override
    public void init() throws RpcException {
    }

    @Override
    public Result invoke(Invocation invocation, List<URL> urlList) throws RpcException {
        Invoker invoker = getInvoker(invocation);
        return  invoker.invoke(invocation,urlList.get(0));
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
