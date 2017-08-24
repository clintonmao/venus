package com.meidusa.venus.client.filter.mock;

import com.meidusa.venus.*;

/**
 * 异常放通处理
 * Created by Zhangzhihua on 2017/8/1.
 */
public class ClientThrowMockFilter implements Filter {

    @Override
    public void init() throws RpcException {

    }

    @Override
    public Result beforeInvoke(Invocation invocation, URL url) throws RpcException {
        return null;
    }

    @Override
    public Result throwInvoke(Invocation invocation, URL url) throws RpcException {
        return null;
    }

    @Override
    public Result afterInvoke(Invocation invocation, URL url) throws RpcException {
        return null;
    }

    @Override
    public void destroy() throws RpcException {

    }
}
