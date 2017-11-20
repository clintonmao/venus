package com.meidusa.venus.notify;

import com.meidusa.venus.annotations.Param;

public interface InvocationListener<T> {

    void callback(@Param(name = "object") T object);

    void onException(Exception e);
}
