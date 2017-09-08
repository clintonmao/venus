package com.meidusa.venus.monitor.athena.filter;

import com.meidusa.venus.*;
import com.meidusa.venus.monitor.athena.reporter.AthenaExtensionResolver;

/**
 * server athena监控filter
 * Created by Zhangzhihua on 2017/8/24.
 */
public class ServerAthenaMonitorFilter implements Filter {

    static boolean isRunning = false;

    public ServerAthenaMonitorFilter(){
        if(!isRunning){
            init();
            isRunning = true;
        }
    }

    @Override
    public void init() throws RpcException {
        AthenaExtensionResolver.getInstance().resolver();
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
