package com.meidusa.venus.bus.common;

import com.meidusa.venus.Invocation;
import com.meidusa.venus.Result;
import com.meidusa.venus.URL;
import com.meidusa.venus.exception.RpcException;

/**
 * 分发调用接口
 * Created by Zhangzhihua on 2017/11/12.
 */
public interface Dispatcher {

    /**
     * 转发请求
     * @param invocation
     * @param url
     * @throws RpcException
     */
    void dispatch(Invocation invocation, URL url) throws RpcException;
}
