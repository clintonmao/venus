package com.meidusa.venus.backend.invoker;

import com.meidusa.venus.*;
import com.meidusa.venus.backend.filter.valid.ServerValidFilter;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.monitor.VenusMonitorFactory;
import com.meidusa.venus.monitor.athena.filter.ServerAthenaMonitorFilter;
import com.meidusa.venus.monitor.filter.ServerMonitorFilter;
import com.meidusa.venus.support.VenusThreadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端调用代理类，除调用外，负责认证、流控、降级相关切面操作
 * Created by Zhangzhihua on 2017/8/25.
 */
public class VenusServerInvokerProxy implements Invoker {

    private VenusServerInvoker venusServerInvoker = new VenusServerInvoker();

    private static boolean isEnableFilter = false;

    //前置filters
    private List<Filter> beforeFilters = new ArrayList<Filter>();
    //异常filters
    private List<Filter> throwFilters = new ArrayList<Filter>();
    //后置filters
    private List<Filter> afterFilters = new ArrayList<Filter>();

    //校验filter
    private ServerValidFilter serverValidFilter = new ServerValidFilter();
    //athena上报filter
    private ServerAthenaMonitorFilter serverAthenaMonitorFilter = new ServerAthenaMonitorFilter();
    //venus上报filter
    private ServerMonitorFilter serverMonitorFilter = new ServerMonitorFilter();

    public VenusServerInvokerProxy(){
        init();
    }

    @Override
    public void init() throws RpcException {
        synchronized (this){
            if(isEnableFilter){
                initFilters();
            }
        }
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
     * 初始化filters
     */
    void initFilters(){
        initBeforeFilters();
        initThrowFilters();
        initAfterFilters();
    }

    /**
     * 初始化前置切面
     */
    void initBeforeFilters(){
        beforeFilters.add(serverValidFilter);
        //监控filters
        initMonitorFilters();
    }

    /**
     * 初始化异常切面
     */
    void initThrowFilters(){
        //监控filters
        initMonitorFilters();
    }

    /**
     * 初始化后置切面
     */
    void initAfterFilters(){
        //监控filters
        initMonitorFilters();
    }

    void initMonitorFilters(){
        VenusMonitorFactory venusMonitorFactory = getVenusMonitorFactory();
        if(venusMonitorFactory != null){
            if(venusMonitorFactory.isEnableAthenaReport()){
                beforeFilters.add(serverAthenaMonitorFilter);
            }
            if(venusMonitorFactory.isEnableVenusReport()){
                beforeFilters.add(getServerMonitorFilter());
            }
        }
    }

    public ServerMonitorFilter getServerMonitorFilter() {
        synchronized (serverMonitorFilter){
            if(!serverMonitorFilter.isRunning()){
                serverMonitorFilter.setAthenaDataService(getVenusMonitorFactory().getAthenaDataService());
                serverMonitorFilter.startProcessAndReporterTread();
            }
        }
        return serverMonitorFilter;
    }

    /**
     * 获取VenusMonitorFactory
     * @return
     */
    VenusMonitorFactory getVenusMonitorFactory(){
        return VenusMonitorFactory.getInstance();
    }

    public List<Filter> getBeforeFilters() {
        return beforeFilters;
    }

    public List<Filter> getThrowFilters() {
        return throwFilters;
    }

    public List<Filter> getAfterFilters() {
        return afterFilters;
    }

    @Override
    public void destroy() throws RpcException {

    }

}
