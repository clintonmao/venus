package com.meidusa.venus.backend.exporter;

import com.meidusa.venus.URL;
import com.meidusa.venus.rpc.Exporter;
import com.meidusa.venus.rpc.RpcException;

/**
 * 服务发布抽象类
 * Created by Zhangzhihua on 2017/8/2.
 */
public abstract class AbstractExporter implements Exporter{

    @Override
    public void exporte(URL url) throws RpcException {
        try {
            init();

            doExporte(url);
        } catch (RpcException e) {
            throw new RpcException(e);
        }
    }

    /**
     * 各协议服务发布实现
     * @param url
     * @throws RpcException
     */
    public abstract  void doExporte(URL url) throws RpcException;

}
