package com.meidusa.venus.client.cluster;

import com.meidusa.venus.ClusterInvoker;

/**
 * cluster invoker factory
 * Created by Zhangzhihua on 2017/9/13.
 */
public class ClusterInvokerFactory {

    /**
     * 获取clusterInvoker
     * @return
     */
    public static ClusterInvoker getClusterInvoker(){
        //TODO 根据配置查找
        return new ClusterFailoverInvoker();
    }
}
