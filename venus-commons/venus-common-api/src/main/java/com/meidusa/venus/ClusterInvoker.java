package com.meidusa.venus;


import java.util.List;

/**
 * 集群调用接口，封装lb/failover操作，对外部调用透明，由各remoteInvoker调用
 * Created by Zhangzhihua on 2017/7/31.
 */
public interface ClusterInvoker {

    /**
     * 初始化
     */
    void init() throws RpcException;

    /**
     * 服务调用
     * @param invocation
     * @param urlList
     * @return
     * @throws RpcException
     */
    Result invoke(Invocation invocation, List<URL> urlList) throws RpcException;

    /**
     * 销毁
     */
    void destroy() throws RpcException;


    Invoker getInvoker();

    /**
     * 设置invoker
     * @param invoker
     */
    void setInvoker(Invoker invoker);
}
