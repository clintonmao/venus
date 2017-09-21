package com.meidusa.venus.client.cluster;

import com.meidusa.venus.*;
import com.meidusa.venus.ClientInvocation;
import com.meidusa.venus.client.cluster.loadbanlance.Loadbanlance;
import com.meidusa.venus.client.cluster.loadbanlance.RandomLoadbanlance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * failover集群容错invoker
 * Created by Zhangzhihua on 2017/7/31.
 */
public class ClusterFailoverInvoker extends AbstractClusterInvoker implements ClusterInvoker {

    private static Logger logger = LoggerFactory.getLogger(ClusterFailoverInvoker.class);

    /**
     * retry次数 TODO 读取配置
     */
    private int retry = 1;

    //TODO 根据配置加载
    private RandomLoadbanlance randomLoadbanlance = new RandomLoadbanlance();

    @Override
    public void init() throws RpcException {
    }

    @Override
    public Result invoke(Invocation invocation, List<URL> urlList) throws RpcException {
        //TODO 只针对系统异常进行重试
        for(int i=0;i<retry;i++){
            try {
                //选择地址
                URL url = getLoadbanlance().select(urlList);

                // 调用
                return  getInvoker().invoke(invocation, url);
            } catch (RpcException e) {
                logger.warn("invoke failed.",e);
            }
        }

        throw new RpcException(String.format("invoke serivce %s,method %s failed with %d tries.",invocation.getServiceName(),invocation.getMethodName(),retry));
    }

    /**
     * 获取loadbanlance
     * @return
     */
    Loadbanlance getLoadbanlance(){
        return randomLoadbanlance;
    }

    @Override
    public void destroy() throws RpcException {

    }
}
