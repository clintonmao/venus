package com.meidusa.venus.client.filter.mock;

import com.meidusa.venus.*;

/**
 * mock调用代理
 * Created by Zhangzhihua on 2017/8/1.
 */
public class ClientMockFilterProxy implements Filter {

    @Override
    public void init() throws RpcException {

    }

    @Override
    public Result beforeInvoke(Invocation invocation, URL url) throws RpcException {
        //TODO
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

    /**
     * 判断是否需要放通处理
     * @param invocation
     * @return
     */
    boolean isNeedMock(Invocation invocation){
        //TODO
        return false;
    }

    /**
     * 获取mock invoker
     * @param invocation
     * @return
     */
    Filter getMockInvoker(Invocation invocation){
        //TODO 处理mock wrapper
        return new ClientReturnMockFilter();
    }

}
