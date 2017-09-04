package com.meidusa.venus.client.filter.limit;

import com.meidusa.venus.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * client tps流控处理
 * Created by Zhangzhihua on 2017/8/1.
 */
public class ClientTpsLimitFilter extends BaseLimitFilter implements Filter {

    private static Logger logger = LoggerFactory.getLogger(ClientTpsLimitFilter.class);

    /**
     * method->tps映射表
     */
    private static Map<String,AtomicInteger> methodTpsMapping = new ConcurrentHashMap<String,AtomicInteger>();

    /**
     * 默认tps流控阈值
     */
    private static final int DEFAULT_TPS_LIMIT = 5;

    /**
     * tps统计数据处理定时任务
     */
    private ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1);

    private static boolean isRunningTask = false;

    public ClientTpsLimitFilter(){
        if(!isRunningTask){
            init();
            isRunningTask = true;
        }
    }

    @Override
    public void init() throws RpcException {
        scheduledThreadPool.schedule(resetAndReportTask,1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public Result beforeInvoke(Invocation invocation, URL url) throws RpcException {
        if(!isEnableTpsLimit(invocation, url)){
            return null;
        }
        //获取方法路径及当前并发数
        String methodPath = getMethodPath(invocation, url);
        AtomicInteger activeLimit = methodTpsMapping.get(methodPath);
        if(activeLimit == null){
            activeLimit = new AtomicInteger(0);
            methodTpsMapping.put(methodPath,activeLimit);
        }
        boolean isExceedTpsLimit = isExceedTpsLimit(methodPath,activeLimit);
        if(isExceedTpsLimit){
            throw new RpcException("exceed tps limit.");
        }
        //+1
        activeLimit.incrementAndGet();
        methodTpsMapping.put(methodPath,activeLimit);
        logger.info("before invoke methodTpsMapping:{}.", methodTpsMapping);
        return null;
    }

    /**
     * 判断是否超过并发流控阈值
     * @param methodPath
     * @return
     */
    boolean isExceedTpsLimit(String methodPath,AtomicInteger tpsLimit){
        int tps = tpsLimit.get();
        //TODO 从本地及注册中心获取流控设置
        return tps > DEFAULT_TPS_LIMIT;
    }

    /**
     * 判断是否开启TPS流控
     * @param invocation
     * @param url
     * @return
     */
    boolean isEnableTpsLimit(Invocation invocation, URL url){
        String limitType = getLimitType(invocation, url);
        return LIMIT_TYPE_TPS.equalsIgnoreCase(limitType);
    }

    @Override
    public Result throwInvoke(Invocation invocation, URL url, Throwable e) throws RpcException {
        return null;
    }

    @Override
    public Result afterInvoke(Invocation invocation, URL url) throws RpcException {
        return null;
    }

    @Override
    public void destroy() throws RpcException {

    }

    /**
     * 定时任务处理
     */
    TimerTask resetAndReportTask = new TimerTask() {

        @Override
        public void run() {
            for(Map.Entry<String,AtomicInteger> entry:methodTpsMapping.entrySet()){
                String methodPath = entry.getKey();
                AtomicInteger activeLimit = entry.getValue();
                //上报
                //TODO reporte
                //清零
                methodTpsMapping.put(methodPath,new AtomicInteger(0));
            }
        }
    };
}
