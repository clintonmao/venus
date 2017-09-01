package com.meidusa.venus.bus.dispatch;

import com.meidusa.venus.Invocation;
import com.meidusa.venus.Result;
import com.meidusa.venus.RpcException;
import com.meidusa.venus.URL;

/**
 * bus消息分发接口
 * Created by Zhangzhihua on 2017/9/1.
 */
public interface Dispatcher {

    /**
     * 消息分发
     * @param invocation
     * @param url
     * @return
     * @throws RpcException
     */
    Result dispatch(Invocation invocation, URL url) throws RpcException;
}
