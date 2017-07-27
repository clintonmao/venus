package com.meidusa.venus.registry;

import com.meidusa.venus.exception.CodedException;
import com.meidusa.venus.exception.ExceptionLevel;
import com.meidusa.venus.exception.VenusExceptionLevel;

/**
 * 服务注册异常
 * Created by Zhangzhihua on 2017/7/27.
 */
public class VenusRegisteException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public VenusRegisteException(String msg) {
        super(msg);
    }

    public VenusRegisteException() {
    }

    public VenusRegisteException(Throwable throwable) {
        super(throwable);
    }

    public VenusRegisteException(String msg, Throwable throwable) {
        super(msg, throwable);
    }

}
