package com.meidusa.venus.client.invoker;

import com.meidusa.venus.*;
import com.meidusa.venus.client.cluster.ClusterFailoverInvoker;
import com.meidusa.venus.client.cluster.ClusterFastfailInvoker;
import com.meidusa.venus.client.factory.xml.config.ClientRemoteConfig;
import com.meidusa.venus.client.invoker.venus.VenusClientInvoker;
import com.meidusa.venus.client.router.Router;
import com.meidusa.venus.client.router.condition.ConditionRuleRouter;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.exception.VenusConfigException;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.domain.VenusServiceDefinitionDO;
import com.meidusa.venus.support.VenusConstants;
import com.meidusa.venus.util.JSONUtil;
import com.meidusa.venus.util.Range;
import com.meidusa.venus.util.RangeUtil;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * client 远程（包含实例间）调用
 * Created by Zhangzhihua on 2017/8/24.
 */
public class ClientRemoteInvoker implements Invoker{

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    /**
     * 静态地址配置
     */
    private ClientRemoteConfig remoteConfig;

    /**
     * 静态地址列表，第一次调用初始化
     */
    private List<URL> cacheUrlList;

    /**
     * 注册中心
     */
    private Register register;

    /**
     * 条件路由服务
     */
    private Router router = new ConditionRuleRouter();

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
        if(isRegisterLookup()){
            //走注册中心寻址调用
            return this.invokeByRegisterLookup(invocation, url);
        }else{
            //静态地址调用
            return this.invokeByStaticLookup(invocation, url);
        }
    }

    /**
     * 走静态寻址调用
     * @param invocation
     * @param url
     * @return
     * @throws RpcException
     */
    Result invokeByStaticLookup(Invocation invocation, URL url) throws RpcException {
        ClientInvocation clientInvocation = (ClientInvocation)invocation;
        //静态寻址
        List<URL> urlList = lookupByStatic(clientInvocation);

        //集群容错调用
        Result result = getClusterInvoker(clientInvocation,url).invoke(invocation, urlList);
        return result;
    }

    /**
     * 走注册中心寻址调用
     * @param invocation
     * @param url
     * @return
     * @throws RpcException
     */
    Result invokeByRegisterLookup(Invocation invocation, URL url) throws RpcException {
        ClientInvocation clientInvocation = (ClientInvocation)invocation;
        //注册中心寻址及版本校验
        List<URL> urlList = lookupByRegister(clientInvocation);

        //自定义路由过滤
        //urlList = router.filte(clientInvocation, urlList);

        //集群容错调用
        Result result = getClusterInvoker(clientInvocation,url).invoke(invocation, urlList);
        return result;
    }

    /**
     * 判断是否注册中心寻址
     * @return
     */
    boolean isRegisterLookup(){
        if(remoteConfig != null){//静态地址
            return false;
        }else if(register != null){//走注册中心寻址
            return true;
        }else{
            throw new VenusConfigException("remoteConfig and registerUrl not allow empty.");
        }
    }

    /**
     * 静态寻址，直接配置addressList或remote
     * @param invocation
     * @return
     */
    List<URL> lookupByStatic(ClientInvocation invocation){
        List<URL> urlList = new ArrayList<URL>();
        if(cacheUrlList == null){
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
            cacheUrlList = urlList;
        }else {
            urlList = cacheUrlList;
        }

        if(CollectionUtils.isEmpty(urlList)){
            throw new RpcException("not found avalid providers.");
        }


        //输出寻址结果信息
        if(logger.isDebugEnabled()){
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
            logger.debug("static lookup service providers num:{},providers:{}.",targets.size(), JSONUtil.toJSONString(targets));
        }
        return urlList;
    }

    /**
     * 动态寻址，注册中心查找
     * @param invocation
     * @return
     */
    List<URL> lookupByRegister(ClientInvocation invocation){
        List<URL> urlList = new ArrayList<URL>();
        //解析请求Url
        URL requestUrl = parseRequestUrl(invocation);

        //查找服务定义
        List<VenusServiceDefinitionDO> srvDefList = getRegister().lookup(requestUrl);
        if(CollectionUtils.isEmpty(srvDefList)){
            throw new RpcException(String.format("not found available service %s providers.",requestUrl.toString()));
        }

        //当前接口定义版本号
        int currentVersion = Integer.parseInt(invocation.getVersion());
        //判断是否允许访问版本
        for(VenusServiceDefinitionDO srvDef:srvDefList){
            if(isAllowVersion(srvDef,currentVersion)){
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
                //若找到，则跳出
                break;
            }
        }

        if(CollectionUtils.isEmpty(urlList)){
            throw new RpcException("with version valid,not found allowed service providers.");
        }

        //输出寻址结果信息
        if(logger.isDebugEnabled()){
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
            logger.debug("lookup service providers num:{},providers:{}.",targets.size(), JSONUtil.toJSONString(targets));
        }
        return urlList;
    }

    /**
     * 判断是否允许访问版本
     * @param srvDef
     * @return
     */
    boolean isAllowVersion(VenusServiceDefinitionDO srvDef,int currentVersion){
        //若版本号相同，则允许
        if(Integer.parseInt(srvDef.getVersion()) == currentVersion){
            return true;
        }

        //否则，根据版本兼容定义判断是否许可
        String versionRange = srvDef.getVersionRange();
        if(StringUtils.isEmpty(versionRange)){
            return false;
        }
        Range supportVersioRange = RangeUtil.getVersionRange(versionRange);
        return supportVersioRange.contains(currentVersion);
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

        StringBuilder buf = new StringBuilder();
        buf.append("/").append(serviceInterfaceName);
        buf.append("/").append(serviceName);
        buf.append("?");
        String serviceUrl = buf.toString();
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

    @Override
    public void destroy() throws RpcException {

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
