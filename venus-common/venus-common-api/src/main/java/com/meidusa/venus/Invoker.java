package com.meidusa.venus;


import com.meidusa.toolkit.net.Connection;
import com.meidusa.venus.exception.RpcException;

/**
 * 服务调用接口，分别由client/server/bus proxy代理类/remote包装类/协议类实现
 * Created by Zhangzhihua on 2017/7/31.
 */
public interface Invoker {

    /**
     * 初始化
     */
    void init() throws RpcException;

    /**
     * 服务调用
     * @param invocation
     * @param url
     * @return
     * @throws RpcException
     */
    Result invoke(Invocation invocation, URL url) throws RpcException;

    /**
     * 释放连接资源
     * @param conn
     */
    void releaseConnection(Connection conn);

    /**
     * 销毁
     */
    void destroy() throws RpcException;
}
