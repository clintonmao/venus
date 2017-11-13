package com.meidusa.venus.bus.dispatch;

import com.meidusa.toolkit.net.*;
import com.meidusa.toolkit.util.TimeUtil;
import com.meidusa.venus.Invocation;
import com.meidusa.venus.ServerInvocation;
import com.meidusa.venus.URL;
import com.meidusa.venus.bus.common.Dispatcher;
import com.meidusa.venus.client.cluster.loadbalance.Loadbalance;
import com.meidusa.venus.client.cluster.loadbalance.RandomLoadbalance;
import com.meidusa.venus.client.cluster.loadbalance.RoundLoadbalance;
import com.meidusa.venus.client.factory.xml.config.ClientRemoteConfig;
import com.meidusa.venus.client.factory.xml.config.FactoryConfig;
import com.meidusa.venus.client.factory.xml.config.PoolConfig;
import com.meidusa.venus.client.router.Router;
import com.meidusa.venus.client.router.condition.ConditionRuleRouter;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.io.network.VenusBackendConnectionFactory;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.domain.VenusServiceDefinitionDO;
import com.meidusa.venus.support.VenusConstants;
import com.meidusa.venus.support.VenusThreadContext;
import com.meidusa.venus.util.JSONUtil;
import com.meidusa.venus.util.Range;
import com.meidusa.venus.util.RangeUtil;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * bus消息分发调用
 * Created by Zhangzhihua on 2017/8/24.
 */
public class BusDispatcher implements Dispatcher {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger tracerLogger = VenusLoggerFactory.getTracerLogger();

    /**
     * 注册中心
     */
    private Register register;

    /**
     * 条件路由服务
     */
    private Router router = new ConditionRuleRouter();

    //服务订阅映射表
    private static Map<String,String> serviceSubscribledMap = new ConcurrentHashMap<String,String>();

    private static ConnectionConnector connector;

    private static ConnectionManager[] connectionManagers;

    //nio连接映射表
    private Map<String, BackendConnectionPool> nioPoolMap = new ConcurrentHashMap<String, BackendConnectionPool>();

    //NIO消息响应处理
    private BusDispatcherMessageHandler messageHandler = new BusDispatcherMessageHandler();

    private RandomLoadbalance randomLoadbanlance = new RandomLoadbalance();
    private RoundLoadbalance roundLoadbanlance = new RoundLoadbalance();
    //服务路径-randomlb映射表
    private static Map<String,RandomLoadbalance> randomLbMap = new ConcurrentHashMap<String,RandomLoadbalance>();
    //服务路径-roundlb映射表
    private static Map<String,RoundLoadbalance> roundLbMap = new ConcurrentHashMap<String,RoundLoadbalance>();

    public BusDispatcher(){
        synchronized (this){
            //构造连接
            if(connector == null && connectionManagers == null){
                try {
                    if(logger.isInfoEnabled()){
                        logger.info("#########init connector#############");
                    }
                    connector = new ConnectionConnector("connection connector-0");
                    int ioThreads = Runtime.getRuntime().availableProcessors();
                    connectionManagers = new ConnectionManager[ioThreads];
                    for(int i=0;i<ioThreads;i++){
                        ConnectionManager connManager = new ConnectionManager("connection manager-" + i, -1);
                        //添加连接监听
                        connectionManagers[i] = connManager;
                        connManager.start();
                    }
                    connector.setProcessors(connectionManagers);
                    connector.start();
                } catch (IOException e) {
                    throw new RpcException(e);
                }
            }

        }
    }

    @Override
    public void dispatch(Invocation invocation) throws RpcException {
        ServerInvocation serverInvocation = (ServerInvocation)invocation;
        //解析请求服务url
        String serviceUrl = parseServiceUrl(serverInvocation);

        URL url = null;
        if(!serviceUrl.contains("?")){
            url = URL.parse(serviceUrl + "?");
        }else{
            url = URL.parse(serviceUrl);
        }

        //若未订阅，则先订阅服务
        if(serviceSubscribledMap.get(serviceUrl) == null){
            register.subscrible(url);
            serviceSubscribledMap.put(serviceUrl,serviceUrl);
        }

        //注册中心寻址及版本校验
        List<URL> urlList = lookupByRegister(serverInvocation,url);

        //自定义路由过滤
        //urlList = router.filte(clientInvocation, urlList);

        //转发调用
        doDispatch(serverInvocation,urlList);
    }

    /**
     * 转发调用
     * @param invocation
     */
    void doDispatch(ServerInvocation invocation,List<URL> urlList){
        //select url
        URL url = getLoadbanlance(null,invocation).select(urlList);

        //转发请求
        sendRequest(invocation, url);
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
    String parseServiceUrl(ServerInvocation invocation){
        String serviceInterfaceName = "null";
        if(invocation.getServiceInterfaceName() != null){
            serviceInterfaceName = invocation.getServiceInterfaceName();
        }
        String serviceName = "null";
        if(invocation.getServiceName() != null){
            serviceName = invocation.getServiceName();
        }

        StringBuilder buf = new StringBuilder();
        buf.append("/").append(serviceInterfaceName);
        buf.append("/").append(serviceName);
        if(StringUtils.isNotEmpty(invocation.getVersion())){
            buf.append("?version=").append(invocation.getVersion());
        }
        String serviceUrl = buf.toString();
        return serviceUrl;
    }

    /**
     * 获取loadbanlance
     * @return
     */
    Loadbalance getLoadbanlance(String lb, ServerInvocation busInvocation){
        //目前，选择lb到服务级别
        String servicePath = busInvocation.getServicePath();
        if(VenusConstants.LOADBALANCE_RANDOM.equals(lb)){
            if(randomLbMap.get(servicePath) == null){
                randomLbMap.put(servicePath,randomLoadbanlance);
            }
            return randomLbMap.get(servicePath);
        }else if(VenusConstants.LOADBALANCE_ROUND.equals(lb)){
            if(roundLbMap.get(servicePath) == null){
                roundLbMap.put(servicePath,roundLoadbanlance);
            }
            return roundLbMap.get(servicePath);
        }else{
            return randomLoadbanlance;
        }
    }

    /**
     * 发送远程调用消息
     * @param invocation
     * @param url 目标地址
     * @return
     * @throws Exception
     */
    void sendRequest(ServerInvocation invocation, URL url) throws RpcException{
        long start = TimeUtil.currentTimeMillis();
        long borrowed = start;
        BackendConnectionPool nioConnPool = null;
        BackendConnection conn = null;
        String rpcId = invocation.getRpcId();
        Throwable exception = null;
        try {
            //获取连接
            nioConnPool = getNioConnPool(url,invocation,null);
            conn = nioConnPool.borrowObject();
            if(!conn.isActive()){
                throw new RpcException(RpcException.NETWORK_EXCEPTION,"connetion not active.");
            }
            borrowed = System.currentTimeMillis();

            //发送请求消息，响应由handler类处理
            ByteBuffer buffer = ByteBuffer.wrap(invocation.getMessage());
            VenusThreadContext.set(VenusThreadContext.CLIENT_OUTPUT_SIZE,Integer.valueOf(buffer.limit()));

            conn.write(buffer);
        } catch (RpcException e){
            exception = e;
            throw e;
        }catch (Throwable e){
            exception = e;
            throw new RpcException(e);
        }finally {
            //返连接
            if (conn != null && nioConnPool != null) {
                nioConnPool.returnObject(conn);
            }

            //打印trace logger
            long connTime = borrowed - start;
            long totalTime = System.currentTimeMillis() - start;
            if(exception != null){
                if (tracerLogger.isErrorEnabled()) {
                    String tpl = "send request,[failed],rpcId:{},methodPath:{},target:{},used time:{},exception:{}.";
                    Object[] arguments = new Object[]{
                            rpcId,
                            invocation.getMethodPath(),
                            url.getHost(),
                            "[" + totalTime + "," + connTime + "]",
                            exception
                    };
                    tracerLogger.error(tpl,arguments);
                }
            }else{
                if(tracerLogger.isInfoEnabled()){
                    String tpl = "send request,[success],rpcId:{},methodPath:{},target:{},used time:{}ms.";
                    Object[] arguments = new Object[]{
                            rpcId,
                            invocation.getMethodPath(),
                            url.getHost(),
                            "[" + totalTime + "," + connTime + "]"
                    };
                    tracerLogger.info(tpl,arguments);
                }
            }
        }
    }


    /**
     * 根据远程配置获取nio连接池
     * @return
     * @throws Exception
     * @param url
     */
    public BackendConnectionPool getNioConnPool(URL url,ServerInvocation invocation,ClientRemoteConfig remoteConfig){
        String address = new StringBuilder()
                .append(url.getHost())
                .append(":")
                .append(url.getPort())
                .toString();
        //若存在，则直接使用连接池
        if(nioPoolMap.get(address) != null){
            return nioPoolMap.get(address);
        }else{
            //若不存在，则创建连接池
            synchronized (this){
                BackendConnectionPool backendConnectionPool = null;
                if(nioPoolMap.get(address) != null){
                    backendConnectionPool = nioPoolMap.get(address);
                }else{
                    backendConnectionPool = createNioPool(url,invocation,new ClientRemoteConfig());
                    nioPoolMap.put(address,backendConnectionPool);
                }
                return backendConnectionPool;
            }
        }
    }

    /**
     * 创建连接池
     * @param url
     * @param remoteConfig
     * @return
     * @throws Exception
     */
    private BackendConnectionPool createNioPool(URL url,ServerInvocation invocation,ClientRemoteConfig remoteConfig){
        if(logger.isInfoEnabled()){
            logger.info("#########create nio pool#############:{}.",url);
        }
        //初始化连接工厂
        VenusBackendConnectionFactory nioFactory = new VenusBackendConnectionFactory();
        nioFactory.setHost(url.getHost());
        nioFactory.setPort(Integer.valueOf(url.getPort()));
        if (remoteConfig.getAuthenticator() != null) {
            nioFactory.setAuthenticator(remoteConfig.getAuthenticator());
        }
        FactoryConfig factoryConfig = remoteConfig.getFactory();
        if (factoryConfig != null) {
            //BeanUtils.copyProperties(nioFactory, factoryConfig);
        }
        nioFactory.setConnector(connector);
        nioFactory.setMessageHandler(messageHandler);
        //nioFactory.setSendBufferSize(2);
        //nioFactory.setReceiveBufferSize(4);
        //nioFactory.setWriteQueueCapcity(16);

        //初始化连接池
        int connectionCount = 4;
        BackendConnectionPool nioPool = new PollingBackendConnectionPool("N-" + url.getHost(), nioFactory, connectionCount);
        PoolConfig poolConfig = remoteConfig.getPool();
        if (poolConfig != null) {
            //BeanUtils.copyProperties(nioPool, poolConfig);
        }
        nioPool.init();
        //若连接池初始化失败，则释放连接池（fix 此时心跳检测已启动）
        boolean isValid = nioPool.isValid();
        if(!isValid){
            boolean isClosed = nioPool.isClosed();
            if(!isClosed){
                if(logger.isWarnEnabled()){
                    logger.warn("connection pool is invalid,close connection pool.");
                }
                try {
                    nioPool.close();
                } catch (Exception e) {
                    //捕获关闭异常，避免影响处理流程
                    if(logger.isErrorEnabled()){
                        logger.error("close invalid connection pool error.");
                    }
                }
            }
            throw new RpcException(RpcException.NETWORK_EXCEPTION,"init connection pool failed.");
        }
        return nioPool;
    }

    /**
     * 动态寻址，注册中心查找
     * @param invocation
     * @return
     */
    List<URL> lookupByRegister(ServerInvocation invocation,URL url){
        List<URL> urlList = new ArrayList<URL>();
        //解析请求Url
        URL requestUrl = url;

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
                    URL tmpUrl = new URL();
                    tmpUrl.setHost(arr[0]);
                    tmpUrl.setPort(Integer.parseInt(arr[1]));
                    tmpUrl.setServiceDefinition(srvDef);
                    if(StringUtils.isNotEmpty(srvDef.getProvider())){
                        tmpUrl.setApplication(srvDef.getProvider());
                    }
                    urlList.add(tmpUrl);
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
                for(URL tmpUrl:urlList){
                    String target = new StringBuilder()
                            .append(tmpUrl.getHost())
                            .append(":")
                            .append(tmpUrl.getPort())
                            .toString();
                    targets.add(target);
                }
            }
            logger.info("lookup service providers num:{},providers:{}.",targets.size(), JSONUtil.toJSONString(targets));
        }
        return urlList;
    }

    public Register getRegister() {
        return register;
    }

    public void setRegister(Register register) {
        this.register = register;
    }

    public BusDispatcherMessageHandler getMessageHandler() {
        return messageHandler;
    }

    public void setMessageHandler(BusDispatcherMessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

}
