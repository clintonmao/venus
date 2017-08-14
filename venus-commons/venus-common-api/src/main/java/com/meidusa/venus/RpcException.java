package com.meidusa.venus;

/**
 * rpc调用异常
 * Created by Zhangzhihua on 2017/7/27.
 */
public class RpcException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RpcException(String msg) {
        super(msg);
    }

    public RpcException() {
    }

    public RpcException(Throwable throwable) {
        super(throwable);
    }

    public RpcException(String msg, Throwable throwable) {
        super(msg, throwable);
    }

}
