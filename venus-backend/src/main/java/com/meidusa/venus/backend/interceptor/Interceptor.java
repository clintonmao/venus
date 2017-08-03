package com.meidusa.venus.backend.interceptor;

import com.meidusa.toolkit.common.bean.util.Initialisable;
import com.meidusa.toolkit.common.bean.util.InitialisationException;
import com.meidusa.venus.backend.invoker.EndpointInvocation;

public interface Interceptor extends Initialisable {
    void init() throws InitialisationException;

    void destroy();

    Object intercept(EndpointInvocation invocation);
}
