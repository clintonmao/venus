package com.meidusa.venus.backend.services;

import com.meidusa.venus.backend.services.EndpointInvocation;
import com.meidusa.venus.backend.services.RequestContext;

public interface InvocationObserver {

    void beforeInvoke(EndpointInvocation invocation, RequestContext context);

    void afterInvoke(EndpointInvocation invocation, RequestContext context);
}
