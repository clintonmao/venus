package com.meidusa.venus.client.filter.limit;

import com.meidusa.venus.*;
import com.meidusa.venus.client.VenusPathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
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
        ClientInvocation clientInvocation = (ClientInvocation)invocation;
        if(!isEnableActiveLimit(clientInvocation, url)){
            return null;
        }
        //获取方法路径及当前并发数
        String methodPath = VenusPathUtil.getMethodPath(clientInvocation, url);
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
    public Result throwInvoke(Invocation invocation, URL url, Throwable e) throws RpcException {
        return null;
    }

    @Override
    public Result afterInvoke(Invocation invocation, URL url) throws RpcException {
        ClientInvocation clientInvocation = (ClientInvocation)invocation;
        if(!isEnableActiveLimit(clientInvocation, url)){
            return null;
        }
        String methodPath = VenusPathUtil.getMethodPath(clientInvocation, url);
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
     * 判断是否开启并发流控
     * @param invocation
     * @param url
     * @return
     */
    boolean isEnableActiveLimit(ClientInvocation invocation, URL url){
        String limitType = getLimitType(invocation, url);
        return LIMIT_TYPE_ACTIVE.equalsIgnoreCase(limitType);
    }

    @Override
    public void destroy() throws RpcException {
    }

    /**
     * 获取流控类型
     * @param invocation
     * @param url
     * @return
     */
    String getLimitType(ClientInvocation invocation, URL url){
        //TODO 获取流控类型
        return LIMIT_TYPE_ACTIVE;
    }

}


