package com.meidusa.venus.client.invoker;

import com.meidusa.venus.*;
import com.meidusa.venus.client.cluster.ClusterFailoverInvoker;
import com.meidusa.venus.client.factory.simple.SimpleServiceFactory;
import com.meidusa.venus.client.factory.xml.config.RemoteConfig;
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
 * client 远程（包含实例间）调用
 * Created by Zhangzhihua on 2017/8/24.
 */
public class ClientRemoteInvoker implements Invoker{

    private static Logger logger = LoggerFactory.getLogger(ClientRemoteInvoker.class);

    private RemoteConfig remoteConfig;

    /**
     * 注册中心地址
     */
    private String registerUrl;

    /**
     * 注册中心
     */
    private Register register;

    /**
     * 路由服务
     */
    private Router router = new ConditionRouter();

    private ClusterFailoverInvoker clusterFailoverInvoker = new ClusterFailoverInvoker();

    @Override
    public void init() throws RpcException {
        //TODO 订阅时机
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
        //寻址，静态或动态 TODO 地址变化对连接池的影响
        List<URL> urlList = lookup(invocation);

        //路由规则过滤
        urlList = router.filte(urlList, invocation);

        //集群容错调用
        Result result = getClusterFailoverInvoker().invoke(invocation, urlList);
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
        if(remoteConfig != null){//静态地址
            List<URL> urlList = lookupByStatic(invocation);
            if(CollectionUtils.isEmpty(urlList)){
                throw new RpcException("not found avalid providers.");
            }
            return urlList;
        }else if(StringUtils.isNotEmpty(registerUrl)){//动态注册中心查找
            List<URL> urlList = lookupByDynamic(invocation);
            if(CollectionUtils.isEmpty(urlList)){
                throw new RpcException("not found avalid providers.");
            }
            return urlList;
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

        //TODO url动态拼装
        String path = "venus://com.chexiang.venus.demo.provider.HelloService/helloService?version=1.0.0";
        URL serviceUrl = URL.parse(path);

        //TODO 本地或远程接口调用
        ServiceDefinition serviceDefinition = getRegister().lookup(serviceUrl);
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
     * 获取集群容错invoker
     * @return
     */
    ClusterInvoker getClusterFailoverInvoker(){
        //TODO 根据配置获取
        return clusterFailoverInvoker;
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
