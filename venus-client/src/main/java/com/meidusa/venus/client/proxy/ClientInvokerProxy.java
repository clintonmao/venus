package com.meidusa.venus.client.proxy;

import com.meidusa.venus.*;
import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.annotations.Service;
import com.meidusa.venus.client.authenticate.DummyAuthenticator;
import com.meidusa.venus.client.factory.xml.config.RemoteConfig;
import com.meidusa.venus.client.invoker.ClientClusterInvoker;
import com.meidusa.venus.monitor.athena.client.filter.ClientAthenaMonitorFilter;
import com.meidusa.venus.client.filter.limit.ClientActivesLimitFilter;
import com.meidusa.venus.client.filter.limit.ClientTpsLimitFilter;
import com.meidusa.venus.client.filter.mock.ClientMockFilterProxy;
import com.meidusa.venus.client.filter.valid.ClientValidFilter;
import com.meidusa.venus.client.invoker.injvm.InjvmInvoker;
import com.meidusa.venus.exception.VenusExceptionFactory;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private RemoteConfig remoteConfig;

    /**
     * 注册中心地址
     */
    private String registerUrl;

    //TODO local/cluster实例化
    private InjvmInvoker injvmInvoker = new InjvmInvoker();

    private ClientClusterInvoker clientClusterInvoker = new ClientClusterInvoker();

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
                    return result;
                }
            }

            //根据配置选择内部调用还是集群环境调用
            Result result = null;
            Service service = invocation.getService();
            Endpoint endpoint = invocation.getEndpoint();
            if (endpoint != null && service != null) {
                if (StringUtils.isEmpty(service.implement())) {
                    //集群调用
                    result = clientClusterInvoker.invoke(invocation,null);
                } else {
                    //本地调用
                    result = injvmInvoker.invoke(invocation,null);
                }
            }else{
                //TODO 确认endpoint为空情况
                result = injvmInvoker.invoke(invocation,null);
            }

            return result;
        } catch (Throwable e) {
            //调用异常切面处理
            for(Filter filter : getThrowFilters()){
                Result result = filter.throwInvoke(invocation,null);
                if(result != null){
                    return result;
                }
            }
            //TODO 本地异常情况
            throw  new RpcException(e);
        }finally {
            //调用结束切面处理
            for(Filter filter : getAfterFilters()){
                filter.afterInvoke(invocation,null);
            }
        }
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
                //降级
                new ClientMockFilterProxy(),
                //client端athena监控采集
                new ClientAthenaMonitorFilter()
        };
    }

    /**
     * 获取前置filters TODO 初始化处理
     * @return
     */
    Filter[] getThrowFilters(){
        return new Filter[]{
                //client端athena监控采集
                new ClientAthenaMonitorFilter()
        };
    }

    /**
     * 获取after filters TODO 初始化处理
     * @return
     */
    Filter[] getAfterFilters(){
        return new Filter[]{
                //client端athena监控采集
                new ClientAthenaMonitorFilter()
        };
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

    public RemoteConfig getRemoteConfig() {
        return remoteConfig;
    }

    public void setRemoteConfig(RemoteConfig remoteConfig) {
        this.remoteConfig = remoteConfig;
    }

    public String getRegisterUrl() {
        return registerUrl;
    }

    public void setRegisterUrl(String registerUrl) {
        this.registerUrl = registerUrl;
    }
}
