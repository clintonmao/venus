package com.meidusa.venus.rpc.mock;

import com.meidusa.venus.rpc.*;

/**
 * mock调用代理
 * Created by Zhangzhihua on 2017/8/1.
 */
public class MockFilterProxy implements Filter {

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        //TODO
        return null;
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
        return new ReturnMockFilter();
    }

}