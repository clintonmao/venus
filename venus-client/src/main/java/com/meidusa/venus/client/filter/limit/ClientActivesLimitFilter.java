package com.meidusa.venus.client.filter.limit;

import com.meidusa.venus.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * client 并发数流控处理
 * Created by Zhangzhihua on 2017/8/1.
 */
public class ClientActivesLimitFilter implements Filter {

    private static Logger logger = LoggerFactory.getLogger(ClientActivesLimitFilter.class);

    /**
     * method->并发数映射表
     */
    private static Map<String,AtomicInteger> methodActivesMapping = new ConcurrentHashMap<String,AtomicInteger>();

    /**
     * 默认并发流控阈值
     */
    private static final int DEFAULT_ACTIVES_LIMIT = 5;

    @Override
    public void init() throws RpcException {
    }

    @Override
    public Result beforeInvoke(Invocation invocation, URL url) throws RpcException {
        if(!isEnableLimit(invocation, url)){
            return null;
        }
        //获取方法路径及当前并发数
        String methodPath = getMethodPath(invocation, url);
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
        logger.info("before invoke methodActivesMapping:{}.",methodActivesMapping);
        return null;
    }

    @Override
    public Result throwInvoke(Invocation invocation, URL url) throws RpcException {
        return null;
    }

    @Override
    public Result afterInvoke(Invocation invocation, URL url) throws RpcException {
        if(!isEnableLimit(invocation, url)){
            return null;
        }
        String methodPath = getMethodPath(invocation, url);
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
     * 判断是否超过并发流控阈值
     * @param methodPath
     * @param activeLimit
     * @return
     */
    boolean isExceedActiveLimit(String methodPath,AtomicInteger activeLimit){
        int actives = activeLimit.get();
        //TODO 从本地及注册中心获取流控设置
        return actives > DEFAULT_ACTIVES_LIMIT;
    }

    /**
     * 获取方法标识路径
     * @param invocation
     * @param url
     * @return
     */
    String getMethodPath(Invocation invocation, URL url){
        String methodPath = String.format(
                "%s/%s?version=%s&method=%s",
                invocation.getMethod().getDeclaringClass().getName(),
                invocation.getService().name(),
                "0.0.0",
                invocation.getMethod().getName()
        );
        logger.info("methodPath:{}.", methodPath);
        return methodPath;
    }

    /**
     * 判断是否开户流控
     * @param invocation
     * @param url
     * @return
     */
    boolean isEnableLimit(Invocation invocation, URL url){
        //TODO 从本地及注册中心获取流控开关
        return true;
    }

    @Override
    public void destroy() throws RpcException {

    }

}


