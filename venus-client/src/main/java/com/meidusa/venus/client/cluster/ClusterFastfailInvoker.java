package com.meidusa.venus.client.cluster;

import com.chexiang.venus.demo.provider.model.Hello;
import com.meidusa.venus.*;
import com.meidusa.venus.ClientInvocation;
import com.meidusa.venus.exception.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * fastfail集群容错invoker，也即正常调用封装
 * Created by Zhangzhihua on 2017/7/31.
 */
public class ClusterFastfailInvoker extends AbstractClusterInvoker implements ClusterInvoker {

    private static Logger logger = LoggerFactory.getLogger(ClusterFastfailInvoker.class);

    @Override
    public void init() throws RpcException {
    }

    @Override
    public Result invoke(Invocation invocation, List<URL> urlList) throws RpcException {
        ClientInvocation clientInvocation = (ClientInvocation)invocation;
        String lb = clientInvocation.getLoadbanlance();

        URL url = getLoadbanlance(lb,clientInvocation).select(urlList);
        if(logger.isInfoEnabled()){
            logger.info("select service provider:【{}】.",new StringBuilder().append(url.getHost()).append(":").append(url.getPort()));
        }

        if("A".equalsIgnoreCase("B")){
            return new Result(new Hello("hi@","ok{cluster-invoke-1}"));
        }

        return  getInvoker().invoke(invocation,url);
    }

    @Override
    public void destroy() throws RpcException {
    }

}
