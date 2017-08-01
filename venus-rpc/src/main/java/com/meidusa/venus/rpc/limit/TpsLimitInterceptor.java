package com.meidusa.venus.rpc.limit;

import com.meidusa.venus.rpc.Interceptor;
import com.meidusa.venus.rpc.Invocation;
import com.meidusa.venus.rpc.RpcException;

/**
 * tps流控处理
 * Created by Zhangzhihua on 2017/8/1.
 */
public class TpsLimitInterceptor implements Interceptor {

    @Override
    public void intercept(Invocation invocation) throws RpcException {
        //TODO 判断开启状态
        //TODO
    }
}
