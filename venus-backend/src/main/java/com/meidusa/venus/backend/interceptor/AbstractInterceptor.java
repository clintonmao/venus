package com.meidusa.venus.backend.interceptor;

import com.meidusa.toolkit.common.bean.util.InitialisationException;
import com.meidusa.venus.backend.services.Interceptor;

public abstract class AbstractInterceptor implements Interceptor {

    @Override
    public void destroy() {

    }

    @Override
    public void init() throws InitialisationException {

    }

}
