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
        String lb = clientInvocation.getLoadbalance();

        //调用相应协议服务
        for(int i=0;i<retries;i++){
            try {
                //选择地址
                URL url = getLoadbanlance(lb,clientInvocation).select(urlList);
                if(logger.isDebugEnabled()){
                    logger.debug("select service provider:【{}】.",new StringBuilder().append(url.getHost()).append(":").append(url.getPort()));
                }

                // 调用
                return  getInvoker().invoke(invocation, url);
            } catch (RpcException e) {
                //对于网络异常、超时异常根据配置进行重试
                if(e.isNetwork() || e.isTimeout()){
                    if(i < retries){
                    }else{
                        throw e;
                    }
                }else{
                    throw e;
                }
            }catch (Throwable t){
                throw t;
            }
        }

        throw new RpcException(String.format("invoke serivce %s,method %s failed with %d tries.",invocation.getServiceName(),invocation.getMethodName(),retries));
    }

    @Override
    public void destroy() throws RpcException {

    }


}
