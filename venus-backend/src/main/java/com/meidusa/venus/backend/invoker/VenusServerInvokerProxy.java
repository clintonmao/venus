package com.meidusa.venus.backend.invoker;

import com.meidusa.venus.*;
import com.meidusa.venus.backend.filter.valid.ServerValidFilter;
import com.meidusa.venus.monitor.athena.filter.ServerAthenaMonitorFilter;

/**
 * 服务端调用代理类，除调用外，负责认证、流控、降级相关切面操作
 * Created by Zhangzhihua on 2017/8/25.
 */
public class VenusServerInvokerProxy implements Invoker {

    //TODO 实例化
    private VenusServerInvoker venusServerInvoker = new VenusServerInvoker();

    @Override
    public void init() throws RpcException {

    }

    @Override
    public Result invoke(Invocation invocation, URL url) throws RpcException {
        try {
            //前置操作，校验、认证、流控、降级
            for(Filter filter : getBeforeFilters()){
                Result result = filter.beforeInvoke(invocation,null);
                if(result != null){
                    return result;
                }
            }

            //处理调用请求
            Result result = venusServerInvoker.invoke(invocation,url);
            return result;
        } catch (RpcException e) {
            //调用异常切面
            for(Filter filter : getBeforeFilters()){
                Result result = filter.beforeInvoke(invocation,null);
                if(result != null){
                    return result;
                }
            }
            throw e;
        } finally {
            //调用后切面
            for(Filter filter : getBeforeFilters()){
                filter.beforeInvoke(invocation,null);
            }
        }
    }

    /**
     * 获取调用前切面
     * @return
     */
    Filter[] getBeforeFilters(){
        return new Filter[]{
                //校验
                new ServerValidFilter()
                //new ServerAthenaMonitorFilter()
        };
    }

    /**
     * 获取调用异常切面
     * @return
     */
    Filter[] getThrowFilters(){
        return new Filter[]{
                //new ServerAthenaMonitorFilter()
        };
    }

    /**
     * 获取调用后切面
     * @return
     */
    Filter[] getAfterFilters(){
        return new Filter[]{
                //new ServerAthenaMonitorFilter()
        };
    }

    @Override
    public void destroy() throws RpcException {

    }
}
