package com.meidusa.venus.backend.invoker.support;

import com.meidusa.venus.backend.invoker.sync.EndpointInvocation;
import com.meidusa.venus.backend.support.RequestContext;

public interface InvocationObserver {

    void beforeInvoke(EndpointInvocation invocation, RequestContext context);

    void afterInvoke(EndpointInvocation invocation, RequestContext context);
}
