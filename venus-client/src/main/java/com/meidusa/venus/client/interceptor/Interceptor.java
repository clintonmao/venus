package com.meidusa.venus.client.interceptor;

import com.meidusa.venus.Invocation;
import com.meidusa.venus.client.RpcException;

/**
 * interceptor
 * Created by Zhangzhihua on 2017/8/1.
 */
public interface Interceptor {

    /**
     * 横切面接口
     * @param invocation
     * @throws RpcException
     */
    void intercept(Invocation invocation) throws RpcException;
}
