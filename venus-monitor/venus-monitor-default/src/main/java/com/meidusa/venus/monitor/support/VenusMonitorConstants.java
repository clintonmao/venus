package com.meidusa.venus.monitor.support;

/**
 * venus监控上报常量定义
 * Created by Zhangzhihua on 2017/11/30.
 */
public class VenusMonitorConstants {

    //consumer
    public static int ROLE_CONSUMER = 0;
    //provider
    public static int ROLE_PROVIDER = 2;
    //一次处理记录条数
    public static int perDetailProcessNum = 100;
    //一次上报记录条数
    public static int perDetailReportNum = 100;
    //慢操作耗时
    public static int SLOW_COST_TIME = 200;
    //支持最大队列长度
    public static int QUEU_MAX_SIZE = 50000;
}
