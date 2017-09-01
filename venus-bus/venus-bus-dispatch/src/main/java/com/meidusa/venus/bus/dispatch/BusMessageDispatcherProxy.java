package com.meidusa.venus.bus.dispatch;

import com.meidusa.venus.Invocation;
import com.meidusa.venus.Result;
import com.meidusa.venus.RpcException;
import com.meidusa.venus.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 消息分发代理类，除进行分发，另处理校验、监控等处理
 * Created by Zhangzhihua on 2017/9/1.
 */
public class BusMessageDispatcherProxy implements Dispatcher {

    private static Logger logger = LoggerFactory.getLogger(BusMessageDispatcherProxy.class);

    BusMessageRemoteDispatcher busMessageRemoteDispatcher;

    @Override
    public Result dispatch(Invocation invocation, URL url) throws RpcException {
        try {
            //beforeFilter
            return busMessageRemoteDispatcher.dispatch(invocation,null);
        } catch (RpcException e) {
            //throwFilter
            throw e;
        } finally {
            //afterFilter
        }
    }
}
