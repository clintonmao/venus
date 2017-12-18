package com.meidusa.venus.backend.invoker;

import com.meidusa.venus.*;
import com.meidusa.venus.backend.filter.valid.ServerValidFilter;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.monitor.VenusMonitorFactory;
import com.meidusa.venus.monitor.athena.filter.ServerAthenaMonitorFilter;
import com.meidusa.venus.monitor.filter.ServerVenusMonitorFilter;
import com.meidusa.venus.support.VenusThreadContext;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端调用代理类，除调用外，负责认证、流控、降级相关切面操作
 * Created by Zhangzhihua on 2017/8/25.
 */
public class VenusServerInvokerProxy implements Invoker {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private VenusServerInvoker venusServerInvoker = new VenusServerInvoker();

    //前置filters
    private List<Filter> beforeFilters = new ArrayList<Filter>();
    //异常filters
    private List<Filter> throwFilters = new ArrayList<Filter>();
    //后置filters
    private List<Filter> afterFilters = new ArrayList<Filter>();

    //校验filter
    private ServerValidFilter serverValidFilter = new ServerValidFilter();
    //venus上报filter
    private static ServerVenusMonitorFilter serverVenusMonitorFilter = null;
    //athena上报filter
    private ServerAthenaMonitorFilter serverAthenaMonitorFilter = null;

    public VenusServerInvokerProxy(){
        init();
    }

    @Override
    public void init() throws RpcException {
        synchronized (this){
            boolean isEnableFilter = VenusApplication.getInstance().isEnableFilter();
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
            throw new RpcException(e);
        } finally {
            //调用后切面
            try {
                for(Filter filter : getAfterFilters()){
                    filter.afterInvoke(invocation, url);
                }
            } catch (Throwable e) {
                throw new RpcException(e);
            }
        }
    }

    /**
     * 初始化filters
     */
    void initFilters(){
        //初始化监控filters
        initMonitorFilters();

        //添加filters
        addBeforeFilters();
        addThrowFilters();
        addAfterFilters();
    }

    /**
     * 初始化监控filter
     */
    void initMonitorFilters(){
        VenusMonitorFactory venusMonitorFactory = VenusMonitorFactory.getInstance();
        if(venusMonitorFactory != null){
            if(venusMonitorFactory.isEnableVenusReport()){
                if(serverVenusMonitorFilter == null){
                    serverVenusMonitorFilter = new ServerVenusMonitorFilter();
                    serverVenusMonitorFilter.init();
                }
            }else{
                if(logger.isWarnEnabled()){
                    logger.warn("############not enable venus report,venus monitor filter diabled##############");
                }
            }
            if(venusMonitorFactory.isEnableAthenaReport()){
                serverAthenaMonitorFilter = new ServerAthenaMonitorFilter();
                serverAthenaMonitorFilter.init();
            }else{
                if(logger.isWarnEnabled()){
                    logger.warn("############not enable athena report,athena monitor filter diabled##############");
                }
            }
        }else{
            if(logger.isWarnEnabled()){
                logger.warn("############not enable monitor report,vensu and athena monitor filter diabled##############");
            }
        }
    }

    /**
     * 添加前置切面
     */
    void addBeforeFilters(){
        beforeFilters.add(serverValidFilter);
        //监控filters
        if(serverVenusMonitorFilter != null){
            beforeFilters.add(serverVenusMonitorFilter);
        }
        if(serverAthenaMonitorFilter != null){
            beforeFilters.add(serverAthenaMonitorFilter);
        }
    }

    /**
     * 添加异常切面
     */
    void addThrowFilters(){
        //监控filters
        if(serverVenusMonitorFilter != null){
            throwFilters.add(serverVenusMonitorFilter);
        }
        if(serverAthenaMonitorFilter != null){
            throwFilters.add(serverAthenaMonitorFilter);
        }
    }

    /**
     * 添加后置切面
     */
    void addAfterFilters(){
        //监控filters
        if(serverVenusMonitorFilter != null){
            afterFilters.add(serverVenusMonitorFilter);
        }
        if(serverAthenaMonitorFilter != null){
            afterFilters.add(serverAthenaMonitorFilter);
        }
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
