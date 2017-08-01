package com.meidusa.venus.limit;

import com.meidusa.venus.Invocation;
import com.meidusa.venus.client.RpcException;
import com.meidusa.venus.client.interceptor.Interceptor;

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
