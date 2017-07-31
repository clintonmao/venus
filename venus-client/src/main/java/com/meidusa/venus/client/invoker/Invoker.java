package com.meidusa.venus.client.invoker;

import com.meidusa.venus.client.RpcException;

/**
 * invoker接口
 * Created by Zhangzhihua on 2017/7/31.
 */
public interface Invoker {

    /**
     * invoke统一接口
     * @param invocation
     * @return
     * @throws RpcException
     */
    Result invoke(Invocation invocation) throws RpcException;
}
