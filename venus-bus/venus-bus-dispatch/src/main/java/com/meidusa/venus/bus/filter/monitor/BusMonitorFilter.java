package com.meidusa.venus.bus.filter.monitor;

import com.meidusa.venus.*;
import com.meidusa.venus.exception.RpcException;

/**
 * bus监控切面处理
 * Created by Zhangzhihua on 2017/9/1.
 */
public class BusMonitorFilter implements Filter {

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
