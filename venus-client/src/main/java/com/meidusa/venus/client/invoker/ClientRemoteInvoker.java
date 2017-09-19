package com.meidusa.venus.client.invoker;

import com.meidusa.venus.*;
import com.meidusa.venus.client.cluster.ClusterInvokerFactory;
import com.meidusa.venus.client.factory.xml.config.RemoteConfig;
import com.meidusa.venus.client.invoker.venus.VenusClientInvoker;
import com.meidusa.venus.client.router.Router;
import com.meidusa.venus.client.router.condition.ConditionRouter;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.RegisterContext;
import com.meidusa.venus.registry.domain.ServiceDefinitionDO;
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
    private RemoteConfig remoteConfig;

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
        //初始化注册中心
        if(register == null){
            initRegister();
        }
        //TODO 订阅时机
        subscrible();
    }

    /**
     * 获取注册中心
     * @return
     */
    void initRegister(){
        Register register = RegisterContext.getInstance().getRegister();//MysqlRegister.getInstance(true,null);
        if(register == null){
            throw new RpcException("init register failed.");
        }
        this.register = register;
    }

    @Override
    public Result invoke(Invocation invocation, URL url) throws RpcException {
        //寻址，静态或动态 TODO 地址变化对连接池的影响
        List<URL> urlList = lookup(invocation);

        //路由规则过滤
        urlList = router.filte(urlList, invocation);

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
    List<URL> lookup(Invocation invocation){
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

        URL serviceUrl = parseUrl(invocation);
        ServiceDefinitionDO serviceDefinition = getRegister().lookup(serviceUrl);
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
        return urlList;
    }

    /**
     * 解析url
     * @param invocation
     * @return
     */
    URL parseUrl(Invocation invocation){
        String path = "venus://com.chexiang.venus.demo.provider.HelloService/helloService?version=1.0.0";
        URL serviceUrl = URL.parse(path);
        return serviceUrl;
    }

    /**
     * 订阅服务 TODO 订阅服务时机
     */
    void subscrible(){
        String subscribleUrl = "subscrible://com.chexiang.venus.demo.provider.HelloService/helloService?version=1.0.0&host=" + NetUtil.getLocalIp();
        URL url = URL.parse(subscribleUrl);
        logger.info("url:{}",url);
        getRegister().subscrible(url);
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

    public RemoteConfig getRemoteConfig() {
        return remoteConfig;
    }

    public void setRemoteConfig(RemoteConfig remoteConfig) {
        this.remoteConfig = remoteConfig;
    }

    public Register getRegister() {
        return register;
    }

    public void setRegister(Register register) {
        this.register = register;
    }
}
