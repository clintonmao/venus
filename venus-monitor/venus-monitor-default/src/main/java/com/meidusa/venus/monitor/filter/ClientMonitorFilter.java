package com.meidusa.venus.monitor.filter;

import com.meidusa.venus.*;

/**
 * client监控filter
 * Created by Zhangzhihua on 2017/8/28.
 */
public class ClientMonitorFilter implements Filter {

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
