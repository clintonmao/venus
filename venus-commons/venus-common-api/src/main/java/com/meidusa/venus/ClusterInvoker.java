package com.meidusa.venus;


import java.util.List;

/**
 * invoker接口
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
