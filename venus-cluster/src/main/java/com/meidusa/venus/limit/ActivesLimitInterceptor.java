package com.meidusa.venus.limit;

import com.meidusa.venus.Invocation;
import com.meidusa.venus.client.RpcException;
import com.meidusa.venus.client.interceptor.Interceptor;

/**
 * 并发数流控处理
 * Created by Zhangzhihua on 2017/8/1.
 */
public class ActivesLimitInterceptor implements Interceptor {

    @Override
    public void intercept(Invocation invocation) throws RpcException {
        //TODO 判断开启情况
        //TODO
    }
}
