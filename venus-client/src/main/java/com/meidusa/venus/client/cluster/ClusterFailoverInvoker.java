package com.meidusa.venus.client.cluster;

import com.meidusa.venus.*;
import com.meidusa.venus.client.cluster.loadbanlance.Loadbanlance;
import com.meidusa.venus.client.cluster.loadbanlance.RandomLoadbanlance;
import com.meidusa.venus.client.invoker.venus.VenusClientInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * failover集群容错invoker
 * Created by Zhangzhihua on 2017/7/31.
 */
public class ClusterFailoverInvoker implements ClusterInvoker {

    private static Logger logger = LoggerFactory.getLogger(ClusterFailoverInvoker.class);

    /**
     * retry次数 TODO 读取配置
     */
    private int retry = 1;

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
        throw new RpcException(String.format("invoke serivce %s,method %s failed with %d tries.",invocation.getService().name(),invocation.getMethod().getName(),retry));
    }

    /**
     * 获取invoker
     * @return
     */
    Invoker getInvoker(){
        return new VenusClientInvoker();
    }

    /**
     * 获取loadbanlance
     * @return
     */
    Loadbanlance getLoadbanlance(){
        return new RandomLoadbanlance();
    }

    @Override
    public void destroy() throws RpcException {

    }
}
