package com.meidusa.venus.bus.filter.exception;

import com.meidusa.venus.*;

/**
 * bus异常切面处理
 * Created by Zhangzhihua on 2017/9/1.
 */
public class BusExceptionFilter implements Filter {

    @Override
    public void init() throws RpcException {

    }

    @Override
    public Result beforeInvoke(Invocation invocation, URL url) throws RpcException {
        return null;
    }

    @Override
    public Result throwInvoke(Invocation invocation, URL url, Throwable e) throws RpcException {
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
