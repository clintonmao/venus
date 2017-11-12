package com.meidusa.venus.support;

/**
 * venus常量定义
 * Created by Zhangzhihua on 2017/10/27.
 */
public class VenusConstants {

    //超时时间，默认3000ms
    public static final int TIMEOUT_DEFAULT = 3000;

    //重试次数，若retries不为空，则cluster默认开启failover
    public static final int RETRIES_DEFAULT = 0;

    //负载策略-随机
    public static final String LOADBALANCE_RANDOM = "random";

    //负载策略-轮询
    public static final String LOADBALANCE_ROUND = "round";

    //负载均衡策略,默认轮询
    public static final String LOADBALANCE_DEFAULT = "round";

    //集群策略-failover
    public static final String CLUSTER_FAILOVER = "failover";

    //集群策略-fastfail
    public static final String CLUSTER_FASTFAIL = "fastfail";

    //集群容错策略，默认fastfail
    public static final String CLUSTER_DEFAULT = "fastfail";

    //默认连接数目，默认为8
    public static final int CONNECTION_DEFAULT_COUNT = 8;

    //venus协议默认线程数
    public static final int VENUS_PROTOCOL_DEFAULT_CORE_THREADS = 100;

    //服务默认版本号
    public static final int VERSION_DEFAULT = 0;
    
    //注册中心-逻辑删除无效的注册订阅服务间隔时间 （秒）
    public static final int LOGIC_DEL_INVALID_SERVICE_TIME = 30;
    
    //注册中心-逻辑删除无效的注册订阅服务间隔时间 （小时）
    public static final int DELELE_INVALID_SERVICE_HOUR = 8;
    
    //注册中心-心跳间隔时间 （秒）
    public static final int HEARTBEAT_INTERVAL = 3;
    
    //注册中心-服务定义加载间隔时间 （秒）
    public static final int SERVER_DEFINE_LOAD_INTERVAL = 10;
    
    //注册中心-失败重试间隔时间 （秒）
    public static final int FAIL_RETRY_INTERVAL = 30;
    
    


}
