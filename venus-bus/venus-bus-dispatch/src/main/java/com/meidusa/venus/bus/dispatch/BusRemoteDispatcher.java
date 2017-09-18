package com.meidusa.venus.bus.dispatch;

import com.meidusa.venus.*;
import com.meidusa.venus.bus.BusInvocation;
import com.meidusa.venus.bus.registry.xml.config.RemoteConfig;
import com.meidusa.venus.bus.registry.ServiceManager;
import com.meidusa.venus.client.cluster.ClusterInvokerFactory;
import com.meidusa.venus.client.router.Router;
import com.meidusa.venus.client.router.condition.ConditionRouter;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.service.registry.ServiceDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
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
        //寻址
        List<URL> urlList = lookup(invocation);

        //TODO 路由规则过滤/版本号校验 router.filte
        //路由规则过滤
        urlList = router.filte(urlList, invocation);

        //集群容错分发调用
        Result result = getClusterInvoker().invoke(invocation,urlList);
        return result;
    }

    /**
     * 寻址
     * @param invocation
     * @return
     */
    List<URL> lookup(Invocation invocation){
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
    List<URL> lookupByStatic(Invocation invocation){
        //TODO
        List<RemoteConfig> remoteConfigList = serviceManager.lookup(invocation.getServiceName());
        //TODO toURL,若空，则抛异常
        return Collections.emptyList();
    }

    /**
     * 查找服务地址
     * @return
     */
    List<URL> lookupByDynamic(Invocation invocation){
        BusInvocation busInvocation = (BusInvocation)invocation;
        //TODO invocation->toUrl
        ServiceDefinition serviceDefinition = register.lookup(null);
        //TODO 若空，则抛异常
        //TODO toUrl，统一寻址输入、输出；统一本地配置输入、输出
        return null;
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


}
