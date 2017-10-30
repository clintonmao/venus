package com.meidusa.venus.bus.dispatch;

import com.meidusa.venus.*;
import com.meidusa.venus.bus.BusInvocation;
import com.meidusa.venus.bus.network.BusFrontendConnection;
import com.meidusa.venus.client.cluster.ClusterFastfailInvoker;
import com.meidusa.venus.client.router.Router;
import com.meidusa.venus.client.router.condition.ConditionRouter;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.domain.VenusServiceDefinitionDO;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 消息远程分发处理，负责寻址、过滤、集群容错分发调用等
 * Created by Zhangzhihua on 2017/9/1.
 */
public class BusRemoteDispatcher implements Dispatcher{

    private static Logger logger = LoggerFactory.getLogger(BusRemoteDispatcher.class);

    /**
     * 注册中心
     */
    private Register register;

    private Map<String,BusFrontendConnection> requestConnectionMap;

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
     * 查找服务地址
     * @return
     */
    List<URL> lookup(BusInvocation invocation){
        List<URL> urlList = new ArrayList<URL>();

        //解析请求Url
        URL requestUrl = parseRequestUrl(invocation);

        //查找服务定义
        Register register = getRegister();
        List<VenusServiceDefinitionDO> serviceDefinitionDOList = null;
        //缓存查找
        serviceDefinitionDOList = register.lookup(requestUrl);
        //若缓存为空，则从注册中心查找
        if(CollectionUtils.isEmpty(serviceDefinitionDOList)){
            serviceDefinitionDOList = register.lookup(requestUrl,true);
            //若注册中心不为空，则订阅服务，否则报服务没有提供节点错误
            if(CollectionUtils.isNotEmpty(serviceDefinitionDOList)){
                //TODO 重复订阅、重复注册、lookup时未订阅
                register.subscrible(requestUrl);
            }else{
                throw new RpcException(String.format("not found available service %s providers.",requestUrl.toString()));
            }
        }
        logger.info("look up service:{} provider group:{}",requestUrl.toString(),serviceDefinitionDOList.size());

        //TODO group/urls关系
        for(VenusServiceDefinitionDO srvDef:serviceDefinitionDOList){
            for(String addresss:srvDef.getIpAddress()){
                String[] arr = addresss.split(":");
                URL url = new URL();
                url.setHost(arr[0]);
                url.setPort(Integer.parseInt(arr[1]));
                url.setServiceDefinition(srvDef);
                urlList.add(url);
            }
        }
        return urlList;
    }

    /**
     * 解析url
     * @param invocation
     * @return
     */
    URL parseRequestUrl(BusInvocation invocation){
        //String path = "venus://com.chexiang.venus.demo.provider.HelloService/helloService?version=0.0.0";
        String protocol = "venus";
        String serviceInterfaceName = invocation.getServiceInterfaceName();
        String serviceName = invocation.getServiceName();
        String version = invocation.getVersion();
        String requestUrl = String.format(
                "%s://%s/%s?version=%s",
                protocol,
                serviceInterfaceName,
                serviceName,
                version
                );
        URL url = URL.parse(requestUrl);
        return url;
    }

    /**
     * 获取集群容错invoker
     * @return
     */
    ClusterInvoker getClusterInvoker(){
        //TODO 根据配置获取clusterInvoker
        if(clusterInvoker == null){
            clusterInvoker =  new ClusterFastfailInvoker(null);
            //TODO 根据配置加载invoker
            //clusterInvoker.setInvoker(new BusDispatcher(this.requestConnectionMap));
        }
        return clusterInvoker;
    }

    @Override
    public void destroy() throws RpcException {

    }

    public Register getRegister() {
        return register;
    }

    public void setRegister(Register register) {
        this.register = register;
    }

    public Map<String, BusFrontendConnection> getRequestConnectionMap() {
        return requestConnectionMap;
    }

    public void setRequestConnectionMap(Map<String, BusFrontendConnection> requestConnectionMap) {
        this.requestConnectionMap = requestConnectionMap;
    }
}
