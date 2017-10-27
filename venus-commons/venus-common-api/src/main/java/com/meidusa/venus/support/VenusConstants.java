package com.meidusa.venus.support;

/**
 * venus常量定义
 * Created by Zhangzhihua on 2017/10/27.
 */
public class VenusConstants {

    //集群容错策略，默认fastfail
    public static String CLUSTER_DEFAULT = "fastfail";

    //重试次数，若retries不为空，则cluster默认开启failover
    public static int RETRIES_DEFAULT = 0;

    //负载均衡策略,默认random
    public static String LOADBANLANCE_DEFAULT = "random";

    //超时时间，默认3000ms
    public static int TIMEOUT_DEFAULT = 3000;
}
