package com.meidusa.venus.backend.interceptors.valid;

import com.meidusa.venus.backend.invoker.support.RpcInvocation;
import com.meidusa.venus.rpc.Interceptor;
import com.meidusa.venus.rpc.Invocation;
import com.meidusa.venus.rpc.Result;
import com.meidusa.venus.rpc.RpcException;

/**
 * 服务端校验处理
 * Created by Zhangzhihua on 2017/8/7.
 */
public class ValidInterceptor implements Interceptor{

    @Override
    public Result intercept(Invocation invocation) throws RpcException {
        RpcInvocation rpcInvocation = (RpcInvocation)invocation;
        //TODO
        return null;
    }
}
