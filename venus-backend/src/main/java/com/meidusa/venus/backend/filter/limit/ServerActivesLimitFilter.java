package com.meidusa.venus.backend.filter.limit;

import com.meidusa.venus.*;
import com.meidusa.venus.backend.ServerInvocation;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.support.VenusUtil;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * server 并发数流控处理
 * Created by Zhangzhihua on 2017/8/1.
 */
public class ServerActivesLimitFilter implements Filter {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    /**
     * method->并发数映射表
     */
    private static Map<String,AtomicInteger> methodActivesMapping = new ConcurrentHashMap<String,AtomicInteger>();

    //流控类型-并发数
    private static final String LIMIT_TYPE_ACTIVE = "active_limit";
    //流控类型-TPS
    private static final String LIMIT_TYPE_TPS = "tps_limit";
    //默认并发流控阈值
    private static final int DEFAULT_ACTIVES_LIMIT = 5;

    @Override
    public void init() throws RpcException {
    }

    @Override
    public Result beforeInvoke(Invocation invocation, URL url) throws RpcException {
        try {
            ServerInvocation serverInvocation = (ServerInvocation)invocation;
            if(!isEnableActiveLimit(serverInvocation, url)){
                return null;
            }
            //获取方法路径及当前并发数
            String methodPath = VenusUtil.getMethodPath(serverInvocation);
            AtomicInteger activeLimit = methodActivesMapping.get(methodPath);
            if(activeLimit == null){
                activeLimit = new AtomicInteger(0);
                methodActivesMapping.put(methodPath,activeLimit);
            }
            //判断是否超过流控阈值
            boolean isExceedActiveLimit = isExceedActiveLimit(methodPath,activeLimit);
            if(isExceedActiveLimit){
                throw new RpcException("exceed actives limit.");
            }
            //+1
            activeLimit.incrementAndGet();
            methodActivesMapping.put(methodPath,activeLimit);
            return null;
        } catch (RpcException e) {
            throw e;
        }catch (Throwable e){
            //对于非rpc异常，也即filter内部执行异常，只记录异常，避免影响正常调用
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error("ServerActivesLimitFilter.beforeInvoke error.",e);
            }
            return null;
        }
    }

    @Override
    public Result throwInvoke(Invocation invocation, URL url, Throwable e) throws RpcException {
        return null;
    }

    @Override
    public Result afterInvoke(Invocation invocation, URL url) throws RpcException {
        ServerInvocation serverInvocation = (ServerInvocation)invocation;
        if(!isEnableActiveLimit(serverInvocation, url)){
            return null;
        }
        String methodPath = VenusUtil.getMethodPath(serverInvocation);
        AtomicInteger activeLimit = methodActivesMapping.get(methodPath);
        if(activeLimit != null){
            //-1
            activeLimit.decrementAndGet();
            methodActivesMapping.put(methodPath,activeLimit);
            logger.info("after invoke methodActivesMapping:{}.",methodActivesMapping);
        }
        return null;
    }

    /**
     * 判断是否开启并发流控
     * @param invocation
     * @param url
     * @return
     */
    boolean isEnableActiveLimit(ServerInvocation invocation, URL url){
        return false;
    }

    /**
     * 判断是否超过并发流控阈值
     * @param methodPath
     * @param activeLimit
     * @return
     */
    boolean isExceedActiveLimit(String methodPath,AtomicInteger activeLimit){
        int actives = activeLimit.get();
        return actives > DEFAULT_ACTIVES_LIMIT;
    }


    @Override
    public void destroy() throws RpcException {
    }

}
