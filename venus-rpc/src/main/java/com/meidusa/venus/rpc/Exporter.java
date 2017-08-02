package com.meidusa.venus.rpc;

import com.meidusa.venus.URL;

/**
 * 服务发布接口
 * Created by Zhangzhihua on 2017/8/2.
 */
public interface Exporter {

    /**
     * 初始化
     * @throws RpcException
     */
    void init() throws RpcException;

    /**
     * 服务发布
     * @param url
     * @throws RpcException
     */
    void exporte(URL url) throws RpcException;

    /**
     * 销毁操作
     * @throws RpcException
     */
    void destroy() throws RpcException;
}
