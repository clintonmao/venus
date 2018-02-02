package com.meidusa.venus.backend.interceptor;

import com.meidusa.venus.backend.services.EndpointInvocation;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Struct
 */
public class AccessControlInterceptor extends AbstractInterceptor {

    private static Logger logger = LoggerFactory.getLogger(AccessControlInterceptor.class);

    @Override
    public Object intercept(EndpointInvocation invocation) {
        logger.info("invoke AccessControlInterceptor...");
        return invocation.invoke();
    }

}
