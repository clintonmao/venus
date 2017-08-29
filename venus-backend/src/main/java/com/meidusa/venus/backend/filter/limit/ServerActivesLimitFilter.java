package com.meidusa.venus.backend.filter.limit;

import com.meidusa.venus.*;
import com.meidusa.venus.client.filter.limit.ClientActivesLimitFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * server 并发数流控处理
 * Created by Zhangzhihua on 2017/8/1.
 */
public class ServerActivesLimitFilter extends ClientActivesLimitFilter implements Filter {

    private static Logger logger = LoggerFactory.getLogger(ServerActivesLimitFilter.class);

}
