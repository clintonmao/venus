package com.meidusa.venus.backend.filter.limit;

import com.meidusa.venus.Filter;
import com.meidusa.venus.client.filter.limit.ClientTpsLimitFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * server tps流控处理filter
 * Created by Zhangzhihua on 2017/8/29.
 */
public class ServerTpsLimitFilter extends ClientTpsLimitFilter implements Filter {

    private static Logger logger = LoggerFactory.getLogger(ServerTpsLimitFilter.class);

    //TODO 覆写差异性
}
