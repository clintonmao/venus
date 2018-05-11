package com.meidusa.venus.monitor.filter;

import com.meidusa.venus.Filter;
import com.meidusa.venus.monitor.support.InvocationDetail;
import com.meidusa.venus.monitor.support.InvocationStatistic;
import com.meidusa.venus.monitor.support.VenusMonitorConstants;
import com.meidusa.venus.util.JSONUtil;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.slf4j.Logger;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * monitor基类
 * Created by Zhangzhihua on 2017/9/4.
 */
public abstract class AbstractMonitorFilter{

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    //明细队列
    protected Queue<InvocationDetail> detailQueue = new LinkedBlockingQueue<InvocationDetail>();
    //待上报明细队列
    protected Queue<InvocationDetail> reportDetailQueue = new LinkedBlockingQueue<InvocationDetail>();
    //方法调用汇总映射表
    protected Map<String,InvocationStatistic> statisticMap = Collections.synchronizedMap(new HashMap<String,InvocationStatistic>());

    /**
     * 添加到明细队列
     */
    public void putDetail2Queue(InvocationDetail detailDO){
        try {
            if(detailQueue.size() > VenusMonitorConstants.QUEU_MAX_SIZE){
                return;
            }
            detailQueue.add(detailDO);
        } catch (Exception e) {
            //不处理异常，避免影响主流程
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error("add monitor queue error.",e);
            }
        }
    }

    String serialize(Object object){
        return JSONUtil.toJSONString(object);
    }

}
