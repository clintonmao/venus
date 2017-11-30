package com.meidusa.venus.monitor;

import com.athena.domain.MethodCallDetailDO;
import com.athena.domain.MethodStaticDO;
import com.meidusa.venus.monitor.support.InvocationDetail;
import com.meidusa.venus.monitor.support.InvocationStatistic;

/**
 * venus监控操作接口
 * Created by Zhangzhihua on 2017/11/30.
 */
public interface MonitorOperation {

    /**
     * 获取角色
     * @return
     */
    int getRole();

    /**
     * 转换明细数据
     * @param detail
     * @return
     */
    MethodCallDetailDO convertDetail(InvocationDetail detail);

    /**
     * 转化统计数据
     * @param statistic
     * @return
     */
    MethodStaticDO convertStatistic(InvocationStatistic statistic);
}
