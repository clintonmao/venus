package com.meidusa.venus.rpc;


/**
 * invoker接口
 * Created by Zhangzhihua on 2017/7/31.
 */
public interface Invoker {

    /**
     * 初始化
     */
    void init() throws RpcException;

    /**
     * 服务调用
     * @param invocation
     * @return
     * @throws RpcException
     */
    Result invoke(Invocation invocation) throws RpcException;

    /**
     * 销毁
     */
    void destroy() throws RpcException;
}
