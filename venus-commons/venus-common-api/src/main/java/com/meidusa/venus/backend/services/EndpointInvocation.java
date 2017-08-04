package com.meidusa.venus.backend.services;

import com.meidusa.venus.backend.services.Endpoint;

/**
 * 
 * @author Struct
 * 
 */
public interface EndpointInvocation {
    enum ResultType {
        NOTIFY, RESPONSE, NONE, OK, ERROR
    }

    Object invoke();

    boolean isExecuted();

    RequestContext getContext();

    Endpoint getEndpoint();

    Object getResult();
}
