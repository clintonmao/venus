package com.meidusa.venus.rpc.mock;

import com.meidusa.venus.rpc.Invocation;
import com.meidusa.venus.rpc.Invoker;
import com.meidusa.venus.rpc.Result;
import com.meidusa.venus.rpc.RpcException;

/**
 * 异常放通处理
 * Created by Zhangzhihua on 2017/8/1.
 */
public class ThrowMockInvoker implements Invoker {

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        //TODO
        return null;
    }

    @Override
    public void init() throws RpcException {
    }

    @Override
    public void destroy() throws RpcException {
    }
}
