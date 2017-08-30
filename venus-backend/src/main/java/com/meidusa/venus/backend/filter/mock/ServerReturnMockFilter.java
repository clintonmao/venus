package com.meidusa.venus.backend.filter.mock;

import com.meidusa.venus.Filter;
import com.meidusa.venus.client.filter.mock.ClientCallbackMockFilter;
import com.meidusa.venus.client.filter.mock.ClientReturnMockFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * server return mock filter
 * Created by Zhangzhihua on 2017/8/30.
 */
public class ServerReturnMockFilter extends ClientReturnMockFilter implements Filter {

    private static Logger logger = LoggerFactory.getLogger(ServerReturnMockFilter.class);

}
