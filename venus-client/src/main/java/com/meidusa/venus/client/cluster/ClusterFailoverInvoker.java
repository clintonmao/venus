package com.meidusa.venus.client.cluster;

import com.meidusa.venus.*;
import com.meidusa.venus.exception.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * failover集群容错invoker
 * Created by Zhangzhihua on 2017/7/31.
 */
public class ClusterFailoverInvoker extends AbstractClusterInvoker implements ClusterInvoker {

    private static Logger logger = LoggerFactory.getLogger(ClusterFailoverInvoker.class);

    @Override
    public void init() throws RpcException {
    }

    @Override
    public Result invoke(Invocation invocation, List<URL> urlList) throws RpcException {
        ClientInvocation clientInvocation = (ClientInvocation)invocation;
        int retries = clientInvocation.getRetries();
        if(retries < 1){
            retries = 1;
        }
        String lb = clientInvocation.getLoadbanlance();
        //对于系统异常，进行重试
        for(int i=0;i<retries;i++){
            try {
                //选择地址
                URL url = getLoadbanlance(lb,clientInvocation).select(urlList);
                if(logger.isInfoEnabled()){
                    logger.info("select service provider:【{}】.",new StringBuilder().append(url.getHost()).append(":").append(url.getPort()));
                }

                // 调用
                return  getInvoker().invoke(invocation, url);
            } catch (RpcException e) {
                logger.warn("invoke failed,to retry.",e);
            }
        }

        throw new RpcException(String.format("invoke serivce %s,method %s failed with %d tries.",invocation.getServiceName(),invocation.getMethodName(),retries));
    }

    @Override
    public void destroy() throws RpcException {

    }
}
