package com.meidusa.venus;


import com.meidusa.venus.exception.RpcException;

/**
 * filte接口定义
 * Created by Zhangzhihua on 2017/8/1.
 */
public interface Filter{

    /**
     * 初始化
     */
    void init() throws RpcException;

    /**
     * 调用前处理
     * @param invocation
     * @param url
     * @return
     * @throws RpcException
     */
    Result beforeInvoke(Invocation invocation, URL url) throws RpcException;

    /**
     * 调用异常
     * @param invocation
     * @param url
     * @param e
     * @return
     * @throws RpcException
     */
    Result throwInvoke(Invocation invocation, URL url, Throwable e) throws RpcException;

    /**
     * 调用后处理
     * @param invocation
     * @param url
     * @return
     * @throws RpcException
     */
    Result afterInvoke(Invocation invocation, URL url) throws RpcException;

    /**
     * 销毁
     */
    void destroy() throws RpcException;
}
