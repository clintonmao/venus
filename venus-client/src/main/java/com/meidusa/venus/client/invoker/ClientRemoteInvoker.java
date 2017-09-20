package com.meidusa.venus.client.invoker;

import com.meidusa.venus.*;
import com.meidusa.venus.ClientInvocation;
import com.meidusa.venus.client.cluster.ClusterInvokerFactory;
import com.meidusa.venus.client.factory.xml.config.ClientRemoteConfig;
import com.meidusa.venus.client.invoker.venus.VenusClientInvoker;
import com.meidusa.venus.client.router.Router;
import com.meidusa.venus.client.router.condition.ConditionRouter;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.RegisterContext;
import com.meidusa.venus.registry.domain.VenusServiceDefinitionDO;
import com.meidusa.venus.util.NetUtil;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * client 远程（包含实例间）调用
 * Created by Zhangzhihua on 2017/8/24.
 */
public class ClientRemoteInvoker implements Invoker{

    private static Logger logger = LoggerFactory.getLogger(ClientRemoteInvoker.class);

    /**
     * 静态地址配置
     */
    private ClientRemoteConfig remoteConfig;

    /**
     * 注册中心
     */
    private Register register;

    /**
     * 路由服务
     */
    private Router router = new ConditionRouter();

    private ClusterInvoker clusterInvoker;

    @Override
    public void init() throws RpcException {
    }

    @Override
    public Result invoke(Invocation invocation, URL url) throws RpcException {
        ClientInvocation clientInvocation = (ClientInvocation)invocation;
        //寻址，静态或动态
        List<URL> urlList = lookup(clientInvocation);

        //路由规则过滤 TODO 版本号访问控制
        urlList = router.filte(clientInvocation, urlList);

        //集群容错调用
        Result result = getClusterInvoker().invoke(invocation, urlList);
        return result;
    }

    @Override
    public void destroy() throws RpcException {

    }

    /**
     * 查找服务提供者地址列表
     * @param invocation
     * @return
     */
    List<URL> lookup(ClientInvocation invocation){
        if(!isDynamicLookup()){//静态地址
            List<URL> urlList = lookupByStatic(invocation);
            if(CollectionUtils.isEmpty(urlList)){
                throw new RpcException("not found avalid providers.");
            }
            return urlList;
        }else{//动态注册中心查找
            List<URL> urlList = lookupByDynamic(invocation);
            if(CollectionUtils.isEmpty(urlList)){
                throw new RpcException("not found avalid providers.");
            }
            return urlList;
        }
    }

    /**
     * 判断是否动态寻址
     * @return
     */
    boolean isDynamicLookup(){
        if(remoteConfig != null){//静态地址
            return false;
        }else if(register != null){//动态注册中心查找
            return true;
        }else{
            throw new RpcException("remoteConfig and registerUrl not allow empty.");
        }
    }

    /**
     * 静态寻址，直接配置addressList或remote
     * @param invocation
     * @return
     */
    List<URL> lookupByStatic(ClientInvocation invocation){
        List<URL> urlList = new ArrayList<URL>();
        //TODO 确认及处理多个地址格式
        String ipAddressList = remoteConfig.getFactory().getIpAddressList();
        String[] arr = ipAddressList.split(":");
        URL url = new URL();
        url.setHost(arr[0]);
        url.setPort(Integer.parseInt(arr[1]));
        url.setRemoteConfig(remoteConfig);
        urlList.add(url);
        return urlList;
    }

    /**
     * 动态寻址，注册中心查找
     * @param invocation
     * @return
     */
    List<URL> lookupByDynamic(ClientInvocation invocation){
        List<URL> urlList = new ArrayList<URL>();

        //解析请求Url
        URL requestUrl = parseRequestUrl(invocation);

        //查找服务定义
        List<VenusServiceDefinitionDO> serviceDefinitionDOList = getRegister().lookup(requestUrl);
        if(CollectionUtils.isEmpty(serviceDefinitionDOList)){
            throw new RpcException(String.format("not found available service %s providers.",requestUrl.toString()));
        }
        logger.info("look up service:{} provider group:{}",requestUrl.toString(),serviceDefinitionDOList.size());

        //TODO group/urls关系
        for(VenusServiceDefinitionDO srvDef:serviceDefinitionDOList){
            logger.info("look up service:{} provider size:{}",requestUrl.toString(),srvDef.getIpAddress().size());
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
     * 解析请求url
     * @param invocation
     * @return
     */
    URL parseRequestUrl(ClientInvocation invocation){
        String serviceInterfaceName = "null";
        if(invocation.getServiceInterface() != null){
            serviceInterfaceName = invocation.getServiceInterface().getName();
        }
        String serviceName = "null";//invocation.getServiceName();
        String version = "0.0.0";//TODO
        String serviceUrl = String.format(
                "venus://%s/%s?version=%s",
                serviceInterfaceName,
                serviceName,
                version
                );
        URL url = URL.parse(serviceUrl);
        return url;
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
            clusterInvoker.setInvoker(new VenusClientInvoker());
        }
        return clusterInvoker;
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
}
