package com.meidusa.venus.client.cluster;

import com.meidusa.venus.*;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * fastfail集群容错invoker，也即正常调用封装
 * Created by Zhangzhihua on 2017/7/31.
 */
public class ClusterFastfailInvoker extends AbstractClusterInvoker implements ClusterInvoker {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    public ClusterFastfailInvoker(Invoker invoker){
        this.invoker = invoker;
    }

    @Override
    public void init() throws RpcException {
    }

    @Override
    public Result invoke(Invocation invocation, List<URL> urlList) throws RpcException {
        ClientInvocation clientInvocation = (ClientInvocation)invocation;

        URL url = getLoadbanlance(clientInvocation.getLoadbalance(),clientInvocation).select(urlList);
        if(logger.isDebugEnabled()){
            logger.debug("select service provider:【{}】.",new StringBuilder().append(url.getHost()).append(":").append(url.getPort()));
        }

        return  getInvoker().invoke(invocation,url);
    }

    @Override
    public void destroy() throws RpcException {
    }


}
