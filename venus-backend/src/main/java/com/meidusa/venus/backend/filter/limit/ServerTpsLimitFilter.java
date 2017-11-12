package com.meidusa.venus.backend.filter.limit;

import com.meidusa.venus.*;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.support.VenusUtil;
import com.meidusa.venus.util.VenusLoggerFactory;
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
 * server tps流控处理filter
 * Created by Zhangzhihua on 2017/8/29.
 */
public class ServerTpsLimitFilter implements Filter {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    /**
     * method->tps映射表
     */
    private static Map<String,AtomicInteger> methodTpsMapping = new ConcurrentHashMap<String,AtomicInteger>();

    //流控类型-并发数
    private static final String LIMIT_TYPE_ACTIVE = "active_limit";
    //流控类型-TPS
    private static final String LIMIT_TYPE_TPS = "tps_limit";
    //默认tps流控阈值
    private static final int DEFAULT_TPS_LIMIT = 5;

    /**
     * tps统计数据处理定时任务
     */
    private ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1);

    private static boolean isRunningTask = false;

    public ServerTpsLimitFilter(){
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
        try {
            ServerInvocation serverInvocation = (ServerInvocation)invocation;
            if(!isEnableTpsLimit(serverInvocation, url)){
                return null;
            }
            //获取方法路径及当前并发数
            String methodPath = VenusUtil.getMethodPath(serverInvocation);
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
            return null;
        } catch (RpcException e) {
            throw e;
        }catch (Throwable e){
            //对于非rpc异常，也即filter内部执行异常，只记录异常，避免影响正常调用
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error("ServerTpsLimitFilter.beforeInvoke error.",e);
            }
            return null;
        }
    }

    /**
     * 判断是否开启TPS流控
     * @param invocation
     * @param url
     * @return
     */
    boolean isEnableTpsLimit(ServerInvocation invocation, URL url){
        return false;
    }

    /**
     * 判断是否超过并发流控阈值
     * @param methodPath
     * @return
     */
    boolean isExceedTpsLimit(String methodPath,AtomicInteger tpsLimit){
        int tps = tpsLimit.get();
        return tps > DEFAULT_TPS_LIMIT;
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
