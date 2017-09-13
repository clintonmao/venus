package com.meidusa.venus.bus.dispatch;

import com.meidusa.venus.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 消息分发代理类，除进行分发，另处理校验、监控等处理
 * Created by Zhangzhihua on 2017/9/1.
 */
public class BusDispatcherProxy implements Dispatcher {

    private static Logger logger = LoggerFactory.getLogger(BusDispatcherProxy.class);

    BusRemoteDispatcher busRemoteDispatcher;

    @Override
    public Result dispatch(Invocation invocation, URL url) throws RpcException {
        try {
            //调用前切面处理，校验、流控、降级等
            for(Filter filter : getBeforeFilters()){
                Result result = filter.beforeInvoke(invocation,null);
                if(result != null){
                    //VenusThreadContext.set(VenusThreadContext.RESPONSE_RESULT,result);
                    return result;
                }
            }

            Result result = busRemoteDispatcher.dispatch(invocation,null);
            return result;
        } catch (RpcException e) {
            //调用异常切面处理
            for(Filter filter : getThrowFilters()){
                Result result = filter.throwInvoke(invocation,url,e );
                if(result != null){
                    //VenusThreadContext.set(VenusThreadContext.RESPONSE_RESULT,result);
                    return result;
                }
            }

            throw e;
        } finally {
            //调用结束切面处理
            for(Filter filter : getAfterFilters()){
                filter.afterInvoke(invocation,url);
            }
        }
    }

    /**
     * 获取前置filters
     * @return
     */
    Filter[] getBeforeFilters(){
        return new Filter[]{
        };
    }

    /**
     * 获取前置filters
     * @return
     */
    Filter[] getThrowFilters(){
        return new Filter[]{
        };
    }

    /**
     * 获取after filters
     * @return
     */
    Filter[] getAfterFilters(){
        return new Filter[]{
        };
    }
}
