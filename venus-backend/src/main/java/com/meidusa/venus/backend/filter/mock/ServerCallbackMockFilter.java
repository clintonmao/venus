package com.meidusa.venus.backend.filter.mock;

import com.meidusa.venus.Filter;
import com.meidusa.venus.client.filter.mock.ClientThrowMockFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * server callback mock filter
 * Created by Zhangzhihua on 2017/8/30.
 */
public class ServerCallbackMockFilter extends ClientThrowMockFilter implements Filter {

    private static Logger logger = LoggerFactory.getLogger(ServerCallbackMockFilter.class);
}
