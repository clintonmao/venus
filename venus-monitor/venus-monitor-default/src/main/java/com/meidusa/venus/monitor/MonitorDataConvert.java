package com.meidusa.venus.monitor;

import com.athena.venus.domain.VenusMethodCallDetailDO;
import com.athena.venus.domain.VenusMethodStaticDO;
import com.meidusa.venus.monitor.support.InvocationDetail;
import com.meidusa.venus.monitor.support.InvocationStatistic;

/**
 * venus监控数据转换接口
 * Created by Zhangzhihua on 2017/11/30.
 */
public interface MonitorDataConvert {

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
    VenusMethodCallDetailDO convertDetail(InvocationDetail detail);

    /**
     * 转化统计数据
     * @param statistic
     * @return
     */
    VenusMethodStaticDO convertStatistic(InvocationStatistic statistic);
}
