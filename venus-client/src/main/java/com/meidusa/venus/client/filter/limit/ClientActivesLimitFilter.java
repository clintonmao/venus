package com.meidusa.venus.client.filter.limit;

import com.meidusa.venus.*;

/**
 * 并发数流控处理
 * Created by Zhangzhihua on 2017/8/1.
 */
public class ClientActivesLimitFilter implements Filter {

    @Override
    public void init() throws RpcException {

    }

    @Override
    public Result beforeInvoke(Invocation invocation, URL url) throws RpcException {
        //TODO 判断开启情况
        //FIXME
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
