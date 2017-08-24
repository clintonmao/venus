package com.meidusa.venus.client.invoker;

import com.meidusa.venus.*;
import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.annotations.Service;
import com.meidusa.venus.client.cluster.FailoverClusterInvoker;
import com.meidusa.venus.client.factory.simple.SimpleServiceFactory;
import com.meidusa.venus.client.factory.xml.config.RemoteConfig;
import com.meidusa.venus.client.filter.athenamonitor.ClientAthenaMonitorFilter;
import com.meidusa.venus.client.filter.limit.ClientActivesLimitFilter;
import com.meidusa.venus.client.filter.limit.ClientTpsLimitFilter;
import com.meidusa.venus.client.filter.mock.ClientMockFilterProxy;
import com.meidusa.venus.client.filter.valid.ClientValidFilter;
import com.meidusa.venus.client.invoker.injvm.InjvmInvoker;
import com.meidusa.venus.client.proxy.InvokerInvocationHandler;
import com.meidusa.venus.client.router.Router;
import com.meidusa.venus.client.router.condition.ConditionRouter;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.RegisterService;
import com.meidusa.venus.registry.mysql.MysqlRegister;
import com.meidusa.venus.service.registry.HostPort;
import com.meidusa.venus.service.registry.ServiceDefinition;
import com.meidusa.venus.util.NetUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * client invoker调用代理类，附加处理校验、流控、降级相关切面操作
 * Created by Zhangzhihua on 2017/8/24.
 */
public class ClientInvokerProxy implements Invoker {

    private static Logger logger = LoggerFactory.getLogger(ClientInvokerProxy.class);

    /**
     * 路由服务
     */
    private Router router = new ConditionRouter();

    /**
     * jvm内部调用
     */
    private InjvmInvoker injvmInvoker;

    /**
     * 注册中心地址
     */
    private String registerUrl = "192.168.1.1:9000";

    /**
     * 注册中心
     */
    private Register register;

    /**
     * 静态连接配置
     */
    private RemoteConfig remoteConfig;

    @Override
    public void init() throws RpcException {
        subscrible();
    }

    /**
     * 订阅服务
     */
    void subscrible(){
        String subscribleUrl = "subscrible://com.chexiang.venus.demo.provider.HelloService/helloService?version=1.0.0&host=" + NetUtil.getLocalIp();
        URL url = URL.parse(subscribleUrl);
        logger.info("url:{}",url);
        getRegister().subscrible(url);
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

            //根据配置选择jvm内部调用还是集群环境容错调用
            Result result = null;
            Service service = invocation.getService();
            Endpoint endpoint = invocation.getEndpoint();
            if (endpoint != null && service != null) {
                if (StringUtils.isEmpty(service.implement())) {
                    //集群容错调用
                    result = doInvokeInCluster(invocation);
                } else {
                    //jvm内部调用
                    result = doInvokeInJvm(invocation);
                }
            }else{
                //TODO 确认endpoint为空情况
                result = doInvokeInJvm(invocation);
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
     * jvm内部调用
     * @param invocation
     * @return
     */
    Result doInvokeInJvm(Invocation invocation){
        return injvmInvoker.invoke(invocation, null);
    }

    /**
     * 集群容错调用
     * @param invocation
     * @return
     */
    Result doInvokeInCluster(Invocation invocation){
        //寻址，TODO 地址变化对连接池的影响
        List<URL> urlList = lookup(invocation);

        //路由规则过滤
        urlList = router.filte(urlList, invocation);

        //集群调用
        Result result = getClusterInvoker().invoke(invocation, urlList);
        return result;
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

    /**
     * 查找服务提供者地址列表
     * @param invocation
     * @return
     */
    List<URL> lookup(Invocation invocation){
        if(remoteConfig != null){
            return lookupByDynamic(invocation);
        }else if(StringUtils.isNotEmpty(registerUrl)){
            return lookupByDynamic(invocation);
        }else{
            throw new RpcException("remoteConfig and registerUrl not allow empty.");
        }
    }

    /**
     * 静态寻址，直接配置addressList或remote
     * @param invocation
     * @return
     */
    List<URL> lookupByStatic(Invocation invocation){
        List<URL> urlList = new ArrayList<URL>();
        //TODO 确认及处理多个地址格式
        String ipAddressList = remoteConfig.getFactory().getIpAddressList();
        String[] arr = ipAddressList.split(":");
        URL url = new URL();
        url.setHost(arr[0]);
        url.setPort(Integer.parseInt(arr[1]));
        urlList.add(url);
        return urlList;
    }

    /**
     * 动态寻址，注册中心查找
     * @param invocation
     * @return
     */
    List<URL> lookupByDynamic(Invocation invocation){
        List<URL> urlList = new ArrayList<URL>();

        String path = "venus://com.chexiang.venus.demo.provider.HelloService/helloService?version=1.0.0";
        URL serviceUrl = URL.parse(path);
        ServiceDefinition serviceDefinition = getRegister().lookup(serviceUrl);
        if(serviceDefinition == null || CollectionUtils.isEmpty(serviceDefinition.getIpAddress())){
            throw new RpcException("service not found available providers.");
        }
        logger.info("serviceDefinition:{}",serviceDefinition);

        for(String item:serviceDefinition.getIpAddress()){
            String[] arr = item.split(":");
            URL url = new URL();
            url.setHost(arr[0]);
            url.setPort(Integer.parseInt(arr[1]));
            urlList.add(url);
        }
        return urlList;
    }

    /**
     * 获取注册中心
     * @return
     */
    Register getRegister(){
        if(register != null){
            return register;
        }
        //TODO 对于远程，使用registerUrl初始化remoteRegisterService
        register = MysqlRegister.getInstance(true,null);
        return register;
    }

    /**
     * 获取注册中心远程服务
     * @param registerUrl
     * @return
     */
    RegisterService getRegisterService(String registerUrl){
        String[] split = registerUrl.split(";");
        List<HostPort> hosts = new ArrayList<HostPort>();
        for (int i = 0; i < split.length; i++) {
            String str = split[i];
            String[] split2 = str.split(":");
            if (split2.length > 1) {
                String host = split2[0];
                String port = split2[1];
                HostPort hp = new HostPort(host, Integer.parseInt(port));
                hosts.add(hp);
            }
        }

        HostPort hp = hosts.get(new Random().nextInt(hosts.size()));
        SimpleServiceFactory ssf = new SimpleServiceFactory(hp.getHost(), hp.getPort());
        ssf.setCoTimeout(60000);
        ssf.setSoTimeout(60000);
        RegisterService registerService = ssf.getService(RegisterService.class);
        return registerService;
    }

    /**
     * 获取cluster invoker
     * @return
     */
    ClusterInvoker getClusterInvoker(){
        return new FailoverClusterInvoker();
    }

    @Override
    public void destroy() throws RpcException {

    }
}
