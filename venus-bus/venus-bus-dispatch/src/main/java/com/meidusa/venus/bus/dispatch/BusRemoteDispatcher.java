package com.meidusa.venus.bus.dispatch;

import com.meidusa.venus.*;
import com.meidusa.venus.bus.BusInvocation;
import com.meidusa.venus.bus.registry.xml.config.RemoteConfig;
import com.meidusa.venus.bus.registry.ServiceManager;
import com.meidusa.venus.client.cluster.ClusterInvokerFactory;
import com.meidusa.venus.client.router.Router;
import com.meidusa.venus.client.router.condition.ConditionRouter;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.domain.VenusServiceDefinitionDO;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息远程分发处理，负责寻址、过滤、集群容错分发调用等
 * Created by Zhangzhihua on 2017/9/1.
 */
public class BusRemoteDispatcher implements Dispatcher{

    private static Logger logger = LoggerFactory.getLogger(BusRemoteDispatcher.class);

    /**
     * XML注册管理
     */
    private ServiceManager serviceManager;

    /**
     * 远程注册中心注册管理
     */
    private Register register;

    private ClusterInvoker clusterInvoker;

    //TODO 构造条件
    private Router router = new ConditionRouter();

    @Override
    public void init() throws RpcException {

    }

    @Override
    public Result invoke(Invocation invocation, URL url) throws RpcException {
        BusInvocation busInvocation = (BusInvocation)invocation;
        //寻址
        List<URL> urlList = lookup(busInvocation);

        //TODO 路由规则过滤/版本号校验 router.filte
        //路由规则过滤
        urlList = router.filte(invocation, urlList);

        //集群容错分发调用
        Result result = getClusterInvoker().invoke(invocation,urlList);
        return result;
    }

    /**
     * 寻址
     * @param invocation
     * @return
     */
    List<URL> lookup(BusInvocation invocation){
        if(!isDynamicLookup()){
            return this.lookupByStatic(invocation);
        }else{
            return this.lookupByDynamic(invocation);
        }
    }

    /**
     * 判断是本地静态还是注册中心动态寻址
     * @return
     */
    boolean isDynamicLookup(){
        return register != null;
    }

    /**
     *
     * @param invocation
     * @return
     */
    List<URL> lookupByStatic(BusInvocation invocation){
        List<URL> urlList = new ArrayList<URL>();
        List<RemoteConfig> remoteConfigList = serviceManager.lookup(invocation.getServiceName());
        if(CollectionUtils.isEmpty(remoteConfigList)){
            return urlList;
        }
        for(RemoteConfig remoteConfig:remoteConfigList){
            String ipAddressList = remoteConfig.getFactory().getIpAddressList();
            String[] arr = ipAddressList.split(":");
            URL url = new URL();
            url.setHost(arr[0]);
            url.setPort(Integer.parseInt(arr[1]));
            urlList.add(url);
        }
        return urlList;
    }

    /**
     * 查找服务地址
     * @return
     */
    List<URL> lookupByDynamic(BusInvocation invocation){
        List<URL> urlList = new ArrayList<URL>();

        URL serviceUrl = parseUrl(invocation);
        VenusServiceDefinitionDO serviceDefinition = register.lookup(serviceUrl);

        if(serviceDefinition == null || CollectionUtils.isEmpty(serviceDefinition.getIpAddress())){
            throw new RpcException("not found available service providers.");
        }
        logger.info("serviceDefinition:{}",serviceDefinition);

        for(String item:serviceDefinition.getIpAddress()){
            String[] arr = item.split(":");
            URL url = new URL();
            url.setHost(arr[0]);
            url.setPort(Integer.parseInt(arr[1]));
            urlList.add(url);
        }
        return null;
    }

    /**
     * 解析url
     * @param invocation
     * @return
     */
    URL parseUrl(BusInvocation invocation){
        String path = "venus://com.chexiang.venus.demo.provider.HelloService/helloService?version=1.0.0";
        URL serviceUrl = URL.parse(path);
        return serviceUrl;
    }

    /**
     * 获取集群容错invoker
     * @return
     */
    ClusterInvoker getClusterInvoker(){
        //TODO 根据配置获取clusterInvoker
        if(clusterInvoker == null){
            clusterInvoker =  ClusterInvokerFactory.getClusterInvoker();
            //TODO 根据配置加载invoker
            clusterInvoker.setInvoker(new BusDispatcher());
        }
        return clusterInvoker;
    }

    @Override
    public void destroy() throws RpcException {

    }

    public ServiceManager getServiceManager() {
        return serviceManager;
    }

    public void setServiceManager(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    public Register getRegister() {
        return register;
    }

    public void setRegister(Register register) {
        this.register = register;
    }
}
