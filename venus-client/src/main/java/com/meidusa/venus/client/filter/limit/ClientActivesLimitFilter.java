package com.meidusa.venus.client.filter.limit;

import com.meidusa.venus.*;
import com.meidusa.venus.client.filter.ServiceConfigUtil;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.registry.domain.FlowControl;
import com.meidusa.venus.support.VenusUtil;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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
        String methodPath = VenusUtil.getMethodPath(clientInvocation);
        AtomicInteger activeLimit = methodActivesMapping.get(methodPath);
        if(activeLimit == null){
            activeLimit = new AtomicInteger(0);
            methodActivesMapping.put(methodPath,activeLimit);
        }
        //判断是否超过流控阈值
        boolean isExceedActiveLimit = isExceedActiveLimit(clientInvocation,url,methodPath,activeLimit);
        if(isExceedActiveLimit){
            throw new RpcException("exceed actives limit.");
        }
        //+1
        activeLimit.incrementAndGet();
        methodActivesMapping.put(methodPath,activeLimit);
        if(logger.isDebugEnabled()){
            logger.debug("method active mappings of before invoke :{}.",methodActivesMapping);
        }
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
        String methodPath = VenusUtil.getMethodPath(clientInvocation);
        AtomicInteger activeLimit = methodActivesMapping.get(methodPath);
        if(activeLimit != null){
            //-1
            activeLimit.decrementAndGet();
            methodActivesMapping.put(methodPath,activeLimit);
            if(logger.isDebugEnabled()){
                logger.debug("method active mappings of after invoke :{}.",methodActivesMapping);
            }
        }
        return null;
    }

    /**
     * 判断是否开启并发流控
     * @param invocation
     * @param url
     * @return
     */
    boolean isEnableActiveLimit(ClientInvocation invocation, URL url){
        FlowControl flowControl = filteMatchConfig(invocation, url);
        if(flowControl == null){
            return false;
        }
        return LIMIT_TYPE_ACTIVE.equalsIgnoreCase(flowControl.getFcType());
    }

    /**
     * 判断是否超过并发流控阈值
     * @param methodPath
     * @param activeLimit
     * @return
     */
    boolean isExceedActiveLimit(ClientInvocation invocation, URL url,String methodPath,AtomicInteger activeLimit){
        int actives = activeLimit.get();
        FlowControl flowControl = filteMatchConfig(invocation, url);
        if(flowControl == null){
            return false;
        }
        return actives > flowControl.getThreshold();
    }

    /**
     * 过滤匹配的规则定义
     * @param invocation
     * @param url
     * @return
     */
    FlowControl filteMatchConfig(ClientInvocation invocation, URL url){
        List<FlowControl> configList = ServiceConfigUtil.getFlowConfigList(url);
        if(CollectionUtils.isEmpty(configList)){
            return null;
        }
        for(FlowControl control:configList){
            if(control.isActive() && "consumer".equalsIgnoreCase(control.getPosition())){
                if(invocation.getMethod().getName().equalsIgnoreCase(control.getMethod())){
                    return control;
                }
            }
        }
        return null;
    }

    @Override
    public void destroy() throws RpcException {
    }

}


