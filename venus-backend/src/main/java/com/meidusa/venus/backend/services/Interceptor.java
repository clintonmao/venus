package com.meidusa.venus.backend.services;

import com.meidusa.toolkit.common.bean.util.Initialisable;
import com.meidusa.toolkit.common.bean.util.InitialisationException;

public interface Interceptor extends Initialisable {
    void init() throws InitialisationException;

    void destroy();

    Object intercept(EndpointInvocation invocation);
}
