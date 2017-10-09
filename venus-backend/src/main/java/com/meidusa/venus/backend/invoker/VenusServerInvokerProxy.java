package com.meidusa.venus.backend.invoker;

import com.athena.service.api.AthenaDataService;
import com.meidusa.venus.*;
import com.meidusa.venus.backend.filter.valid.ServerValidFilter;
import com.meidusa.venus.monitor.AthenaContext;
import com.meidusa.venus.monitor.athena.filter.ServerAthenaMonitorFilter;
import com.meidusa.venus.monitor.filter.ServerMonitorFilter;

/**
 * 服务端调用代理类，除调用外，负责认证、流控、降级相关切面操作
 * Created by Zhangzhihua on 2017/8/25.
 */
public class VenusServerInvokerProxy implements Invoker {

    //TODO 实例化
    private VenusServerInvoker venusServerInvoker = new VenusServerInvoker();

    private AthenaDataService athenaDataService;

    private ServerAthenaMonitorFilter serverAthenaMonitorFilter;

    private ServerMonitorFilter serverMonitorFilter;

    @Override
    public void init() throws RpcException {

    }

    @Override
    public Result invoke(Invocation invocation, URL url) throws RpcException {
        Result result = null;
        try {
            //前置操作，校验、认证、流控、降级
            for(Filter filter : getBeforeFilters()){
                 result = filter.beforeInvoke(invocation,null);
                if(result != null){
                    VenusThreadContext.set(VenusThreadContext.RESPONSE_RESULT,result);
                    return result;
                }
            }

            //处理调用请求
            result = venusServerInvoker.invoke(invocation,url);
            VenusThreadContext.set(VenusThreadContext.RESPONSE_RESULT,result);
            return result;
        } catch (Throwable e) {
            VenusThreadContext.set(VenusThreadContext.RESPONSE_EXCEPTION,e);
            //调用异常切面
            for(Filter filter : getThrowFilters()){
                result = filter.throwInvoke(invocation,null,e);
                if(result != null){
                    VenusThreadContext.set(VenusThreadContext.RESPONSE_RESULT,result);
                    return result;
                }
            }
            //TODO catch中异常处理
            throw new RpcException(e);
        } finally {
            //调用后切面
            try {
                for(Filter filter : getAfterFilters()){
                    filter.afterInvoke(invocation, url);
                }
            } catch (Throwable e) {
                //TODO finally异常处理
                throw new RpcException(e);
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
                new ServerValidFilter(),
                getServerAthenaMonitorFilter(),
                getServerMonitorFilter()
        };
    }

    /**
     * 获取调用异常切面
     * @return
     */
    Filter[] getThrowFilters(){
        return new Filter[]{
                getServerAthenaMonitorFilter(),
                getServerMonitorFilter()
        };
    }

    /**
     * 获取调用后切面
     * @return
     */
    Filter[] getAfterFilters(){
        return new Filter[]{
                getServerAthenaMonitorFilter(),
                getServerMonitorFilter()
        };
    }

    public ServerAthenaMonitorFilter getServerAthenaMonitorFilter() {
        if(serverAthenaMonitorFilter == null){
            serverAthenaMonitorFilter = new ServerAthenaMonitorFilter();
        }
        return serverAthenaMonitorFilter;
    }

    public ServerMonitorFilter getServerMonitorFilter() {
        if(serverMonitorFilter == null){
            serverMonitorFilter = new ServerMonitorFilter(getAthenaDataService());
        }
        return serverMonitorFilter;
    }

    @Override
    public void destroy() throws RpcException {

    }

    public AthenaDataService getAthenaDataService() {
        return AthenaContext.getInstance().getAthenaDataService();
    }

}
