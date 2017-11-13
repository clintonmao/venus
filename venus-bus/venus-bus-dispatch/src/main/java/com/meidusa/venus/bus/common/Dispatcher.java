package com.meidusa.venus.bus.common;

import com.meidusa.venus.Invocation;
import com.meidusa.venus.exception.RpcException;

/**
 * 分发调用接口
 * Created by Zhangzhihua on 2017/11/12.
 */
public interface Dispatcher {

    /**
     * 转发请求
     * @param invocation
     * @throws RpcException
     */
    void dispatch(Invocation invocation) throws RpcException;
}
