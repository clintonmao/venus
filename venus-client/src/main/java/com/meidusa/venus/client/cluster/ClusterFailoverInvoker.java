package com.meidusa.venus.client.cluster;

import com.meidusa.venus.*;
import com.meidusa.venus.client.ClientInvocation;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.slf4j.Logger;

import java.util.List;

/**
 * failover集群容错invoker，封装invoker调用，将集群容错透明化
 * Created by Zhangzhihua on 2017/7/31.
 */
public class ClusterFailoverInvoker extends AbstractClusterInvoker implements ClusterInvoker {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    public ClusterFailoverInvoker(Invoker invoker){
        this.invoker = invoker;
    }

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

        //服务调用，根据配置进行重试
        for(int i=0;i<retries;i++){
            try {
                return doInvokeForNetworkFailover(invocation, urlList);
            } catch (RpcException e) {
                //对于timeout超时异常根据客户端配置进行重试
                if(e.isTimeout()){
                    if(i < retries){
                    }else{
                        throw e;
                    }
                }else{
                    throw e;
                }
            } catch (Throwable t){
                throw t;
            }
        }

        throw new RpcException(String.format("invoke serivce %s,method %s failed with %d tries.",invocation.getServiceName(),invocation.getMethodName(),retries));
    }


    @Override
    public void destroy() throws RpcException {

    }


}
