package com.meidusa.venus.bus.dispatch;

import com.meidusa.venus.*;
import com.meidusa.venus.bus.network.BusFrontendConnection;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.registry.VenusRegistryFactory;
import com.meidusa.venus.registry.Register;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 消息分发代理类，除进行分发，另处理校验、监控等切面操作
 * Created by Zhangzhihua on 2017/9/1.
 */
public class BusDispatcherProxy implements Dispatcher {

    private static Logger logger = LoggerFactory.getLogger(BusDispatcherProxy.class);

    private VenusRegistryFactory venusRegistryFactory;

    private BusRemoteDispatcher busRemoteDispatcher;

    private Map<String,BusFrontendConnection> requestConnectionMap;

    @Override
    public void init() throws RpcException {

    }

    @Override
    public Result invoke(Invocation invocation, URL url) throws RpcException {
        try {
            //调用前切面处理，校验、流控、降级等
            for(Filter filter : getBeforeFilters()){
                Result result = filter.beforeInvoke(invocation,null);
                if(result != null){
                    //VenusThreadContext.set(VenusThreadContext.RESPONSE_RESULT,result);
                    return result;
                }
            }

            //远程调用
            Result result = getBusRemoteDispatcher().invoke(invocation,null);
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

    public BusRemoteDispatcher getBusRemoteDispatcher() {
        if(busRemoteDispatcher != null){
            return busRemoteDispatcher;
        }
        busRemoteDispatcher = new BusRemoteDispatcher();
        if(venusRegistryFactory != null){
            Register register = venusRegistryFactory.getRegister();
            busRemoteDispatcher.setRegister(register);
        }
        if(requestConnectionMap != null){
            busRemoteDispatcher.setRequestConnectionMap(requestConnectionMap);
        }
        return busRemoteDispatcher;
    }

    @Override
    public void destroy() throws RpcException {

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

    public VenusRegistryFactory getVenusRegistryFactory() {
        return venusRegistryFactory;
    }

    public void setVenusRegistryFactory(VenusRegistryFactory venusRegistryFactory) {
        this.venusRegistryFactory = venusRegistryFactory;
    }

    public Map<String, BusFrontendConnection> getRequestConnectionMap() {
        return requestConnectionMap;
    }

    public void setRequestConnectionMap(Map<String, BusFrontendConnection> requestConnectionMap) {
        this.requestConnectionMap = requestConnectionMap;
    }
}
