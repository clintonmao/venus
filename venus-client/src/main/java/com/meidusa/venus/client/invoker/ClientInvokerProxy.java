package com.meidusa.venus.client.invoker;

import com.athena.service.api.AthenaDataService;
import com.meidusa.venus.*;
import com.meidusa.venus.ClientInvocation;
import com.meidusa.venus.client.invoker.injvm.InjvmClientInvoker;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.io.utils.RpcIdUtil;
import com.meidusa.venus.monitor.AthenaContext;
import com.meidusa.venus.client.authenticate.DummyAuthenticator;
import com.meidusa.venus.client.factory.xml.config.ClientRemoteConfig;
import com.meidusa.venus.client.filter.limit.ClientActivesLimitFilter;
import com.meidusa.venus.client.filter.limit.ClientTpsLimitFilter;
import com.meidusa.venus.client.filter.mock.ClientCallbackMockFilter;
import com.meidusa.venus.client.filter.mock.ClientReturnMockFilter;
import com.meidusa.venus.client.filter.mock.ClientThrowMockFilter;
import com.meidusa.venus.client.filter.valid.ClientValidFilter;
import com.meidusa.venus.exception.VenusExceptionFactory;
import com.meidusa.venus.monitor.athena.filter.ClientAthenaMonitorFilter;
import com.meidusa.venus.monitor.filter.ClientMonitorFilter;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.support.EndpointWrapper;
import com.meidusa.venus.support.ServiceWrapper;
import com.meidusa.venus.support.VenusThreadContext;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * client invoker调用代理类，附加处理校验、流控、降级相关切面操作
 * Created by Zhangzhihua on 2017/8/24.
 */
public class ClientInvokerProxy implements Invoker {

    private static Logger logger = LoggerFactory.getLogger(ClientInvokerProxy.class);

    /**
     * 异常处理
     */
    private VenusExceptionFactory venusExceptionFactory;

    /**
     * 认证配置
     */
    private DummyAuthenticator authenticator;

    /**
     * 静态配置地址
     */
    private ClientRemoteConfig remoteConfig;

    /**
     * 注册中心
     */
    private Register register;

    /**
     * injvm调用 TODO 初始化
     */
    private InjvmClientInvoker injvmInvoker = new InjvmClientInvoker();

    /**
     * 远程(包含同ip实例间)调用
     */
    private ClientRemoteInvoker clientRemoteInvoker = new ClientRemoteInvoker();

    private ClientMonitorFilter clientMonitorFilter;

    private static boolean isEnableFilter = false;

    @Override
    public void init() throws RpcException {
    }

    @Override
    public Result invoke(Invocation invocation, URL url) throws RpcException {
        /*
        if(random.nextInt(50000) > 49980){
            logger.error("current thread:{},instance:{}",Thread.currentThread(),this);
        }
        */
        long bTime = System.currentTimeMillis();
        ClientInvocation clientInvocation = (ClientInvocation)invocation;
        try {
            if(isEnableFilter){
                //调用前切面处理，校验、流控、降级等
                for(Filter filter : getBeforeFilters()){
                    Result result = filter.beforeInvoke(invocation,null);
                    if(result != null){
                        VenusThreadContext.set(VenusThreadContext.RESPONSE_RESULT,result);
                        return result;
                    }
                }
            }

            //根据配置选择内部调用还是跨实例/远程调用
            if(isInjvmInvoke(clientInvocation)){
                Result result = getInjvmInvoker().invoke(invocation, url);
                VenusThreadContext.set(VenusThreadContext.RESPONSE_RESULT,result);
                return result;
            }else{
                ClientRemoteInvoker clientRemoteInvoker = getRemoteInvoker();
                Result result = clientRemoteInvoker.invoke(invocation, url);
                //logger.error("clientRemoteInvoker:{},thread:{},this.",clientRemoteInvoker,Thread.currentThread(),this);
                VenusThreadContext.set(VenusThreadContext.RESPONSE_RESULT,result);
                return result;
            }
        } catch (Throwable e) {
            VenusThreadContext.set(VenusThreadContext.RESPONSE_EXCEPTION,e);
            if(isEnableFilter){
                //调用异常切面处理
                for(Filter filter : getThrowFilters()){
                    Result result = filter.throwInvoke(invocation,url,e );
                    if(result != null){
                        VenusThreadContext.set(VenusThreadContext.RESPONSE_RESULT,result);
                        return result;
                    }
                }
            }
            //TODO 本地异常情况
            throw  new RpcException(e);
        }finally {
            if(isEnableFilter){
                //调用结束切面处理
                for(Filter filter : getAfterFilters()){
                    filter.afterInvoke(invocation,url);
                }
            }
            if(logger.isWarnEnabled()){
                logger.warn("request rpcId:{} cost time:{}.", RpcIdUtil.getRpcId(clientInvocation.getClientId(),clientInvocation.getClientRequestId()),System.currentTimeMillis()-bTime);
            }
        }
    }

    /**
     * 判断是否invjm内部调用
     * @param invocation
     * @return
     */
    boolean isInjvmInvoke(ClientInvocation invocation){
        ServiceWrapper service = invocation.getService();
        EndpointWrapper endpoint = invocation.getEndpoint();
        if (endpoint != null && service != null) {
            return StringUtils.isNotEmpty(service.getImplement());
        }else{
            //本地调用
            //TODO 确认endpoint为空情况
            return true;
        }
    }

    public InjvmClientInvoker getInjvmInvoker() {
        return injvmInvoker;
    }

    /**
     * 获取remote Invoker
     * @return
     */
    public ClientRemoteInvoker getRemoteInvoker() {
        if(remoteConfig != null){
            clientRemoteInvoker.setRemoteConfig(remoteConfig);
        }
        if(register != null){
            clientRemoteInvoker.setRegister(register);
        }
        return clientRemoteInvoker;
    }

    /**
     * 获取前置filters TODO 初始化处理
     * @return
     */
    Filter[] getBeforeFilters(){
        return new Filter[]{
                //校验
                new ClientValidFilter(),
                //并发数流控
                new ClientActivesLimitFilter(),
                //TPS流控
                new ClientTpsLimitFilter(),
                //return降级
                new ClientReturnMockFilter(),
                //throw降级
                new ClientThrowMockFilter(),
                //mock降级
                new ClientCallbackMockFilter(),
                //athena监控
                new ClientAthenaMonitorFilter(),
                //venus监控
                getClientMonitorFilter()
        };
    }

    /**
     * 获取前置filters TODO 初始化处理
     * @return
     */
    Filter[] getThrowFilters(){
        return new Filter[]{
                //athena监控
                new ClientAthenaMonitorFilter(),
                //venus监控
                getClientMonitorFilter()
        };
    }

    /**
     * 获取after filters TODO 初始化处理
     * @return
     */
    Filter[] getAfterFilters(){
        return new Filter[]{
                //并发数流控
                new ClientActivesLimitFilter(),
                //athena监控
                new ClientAthenaMonitorFilter(),
                //venus监控
                getClientMonitorFilter()
        };
    }

    /**
     * getClientMonitorFilter
     * @return
     */
    ClientMonitorFilter getClientMonitorFilter(){
         if(clientMonitorFilter != null){
             return clientMonitorFilter;
         }
        clientMonitorFilter = new ClientMonitorFilter(getAthenaDataService());
        return clientMonitorFilter;
    }

    /**
     * 获取athena监控filter
     * @return
     */
    Filter getAthenaMonitorFilter(){
        try {
            //TODO 实例化
            Filter filter = (Filter) Class.forName("com.meidusa.venus.monitor.athena.filter.ClientAthenaMonitorFilter").newInstance();
            return filter;
        } catch (Exception e) {
            logger.error("new ClientAthenaMonitorFilter error.",e);
            return null;
        }
    }


    @Override
    public void destroy() throws RpcException {

    }

    public VenusExceptionFactory getVenusExceptionFactory() {
        return venusExceptionFactory;
    }

    public void setVenusExceptionFactory(VenusExceptionFactory venusExceptionFactory) {
        this.venusExceptionFactory = venusExceptionFactory;
    }

    public DummyAuthenticator getAuthenticator() {
        return authenticator;
    }

    public void setAuthenticator(DummyAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    public ClientRemoteConfig getRemoteConfig() {
        return remoteConfig;
    }

    public void setRemoteConfig(ClientRemoteConfig remoteConfig) {
        this.remoteConfig = remoteConfig;
    }

    public Register getRegister() {
        return register;
    }

    public void setRegister(Register register) {
        this.register = register;
    }

    public AthenaDataService getAthenaDataService() {
        return AthenaContext.getInstance().getAthenaDataService();
    }

}
