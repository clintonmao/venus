package com.meidusa.venus.client.invoker;

import com.chexiang.venus.demo.provider.model.Hello;
import com.meidusa.venus.*;
import com.meidusa.venus.ClientInvocation;
import com.meidusa.venus.client.cluster.ClusterFailoverInvoker;
import com.meidusa.venus.client.cluster.ClusterFastfailInvoker;
import com.meidusa.venus.client.factory.xml.config.ClientRemoteConfig;
import com.meidusa.venus.client.invoker.venus.VenusClientInvoker;
import com.meidusa.venus.client.router.Router;
import com.meidusa.venus.client.router.condition.ConditionRouter;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.io.utils.RpcIdUtil;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.domain.VenusServiceDefinitionDO;
import com.meidusa.venus.support.VenusConstants;
import com.meidusa.venus.util.JSONUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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
     * 条件路由服务
     */
    private Router router = new ConditionRouter();

    /**
     * venus协议调用invoker
     */
    private VenusClientInvoker invoker = new VenusClientInvoker();

    private ClusterFailoverInvoker clusterFailoverInvoker = new ClusterFailoverInvoker(invoker);

    private ClusterFastfailInvoker clusterFastfailInvoker = new ClusterFastfailInvoker(invoker);

    @Override
    public void init() throws RpcException {
    }

    @Override
    public Result invoke(Invocation invocation, URL url) throws RpcException {
        long bTime = System.currentTimeMillis();
        ClientInvocation clientInvocation = (ClientInvocation)invocation;
        //寻址，静态或动态
        List<URL> urlList = lookup(clientInvocation);

        //自定义路由过滤 TODO 版本号访问控制
        //urlList = router.filte(clientInvocation, urlList);

        //集群容错调用
        ClusterInvoker clusterInvoker = getClusterInvoker(clientInvocation,url);
        Result result = clusterInvoker.invoke(invocation, urlList);
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
        List<URL> urlList = null;
        if(!isDynamicLookup()){//静态地址
            urlList = lookupByStatic(invocation);
            if(CollectionUtils.isEmpty(urlList)){
                throw new RpcException("not found avalid providers.");
            }
        }else{//动态注册中心查找
            urlList = lookupByDynamic(invocation);
            if(CollectionUtils.isEmpty(urlList)){
                throw new RpcException("not found avalid providers.");
            }
        }

        //输出寻址结果信息
        List<String> targets = new ArrayList<String>();
        if(CollectionUtils.isNotEmpty(urlList)){
            for(URL url:urlList){
                String target = new StringBuilder()
                        .append(url.getHost())
                        .append(":")
                        .append(url.getPort())
                        .toString();
                targets.add(target);
            }
        }
        if(logger.isInfoEnabled()){
            logger.info("lookup service providers num:{},providers:{}.",targets.size(), JSONUtil.toJSONString(targets));
        }
        return urlList;
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
        String ipAddressList = remoteConfig.getFactory().getIpAddressList();
        String[] addressArr = ipAddressList.split(";");
        for(String address:addressArr){
            String[] arr = address.split(":");
            URL url = new URL();
            url.setHost(arr[0]);
            url.setPort(Integer.parseInt(arr[1]));
            url.setRemoteConfig(remoteConfig);
            urlList.add(url);
        }
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
        //logger.info("lookup service:{} provider group:{}",requestUrl.toString(),serviceDefinitionDOList.size());

        //TODO group/urls关系
        for(VenusServiceDefinitionDO srvDef:serviceDefinitionDOList){
            //logger.info("lookup service:{} provider size:{}",requestUrl.toString(),srvDef.getIpAddress().size());
            for(String addresss:srvDef.getIpAddress()){
                String[] arr = addresss.split(":");
                URL url = new URL();
                url.setHost(arr[0]);
                url.setPort(Integer.parseInt(arr[1]));
                url.setServiceDefinition(srvDef);
                if(StringUtils.isNotEmpty(srvDef.getProvider())){
                    url.setApplication(srvDef.getProvider());
                }
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
        String serviceName = "null";
        if(invocation.getService() != null){
            serviceName = invocation.getService().getName();
        }
        //若不指定版本号，则使用默认版本号
        String versionx = VenusConstants.VERSION_DEFAULT;
        if(StringUtils.isNotEmpty(invocation.getVersionx())){
            versionx = invocation.getVersionx();
        }

        StringBuilder stringBuilder = new StringBuilder()
                .append("venus://")
                .append(serviceInterfaceName).append("/")
                .append(serviceName);
        if(StringUtils.isNotEmpty(versionx)){
            stringBuilder
                    .append("?version=").append(versionx);
        }
        String serviceUrl = stringBuilder.toString();
        URL url = URL.parse(serviceUrl);
        return url;
    }

    /**
     * 获取集群容错invoker
     * @return
     */
    ClusterInvoker getClusterInvoker(ClientInvocation invocation,URL url){
        String cluster = invocation.getCluster();
        if(VenusConstants.CLUSTER_FAILOVER.equals(cluster) || invocation.getRetries() > 0){
            return clusterFailoverInvoker;
        }else if(VenusConstants.CLUSTER_FASTFAIL.equals(cluster)){
            return clusterFastfailInvoker;
        }else{
            throw new RpcException(String.format("invalid cluster policy:%s.",cluster));
        }
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
