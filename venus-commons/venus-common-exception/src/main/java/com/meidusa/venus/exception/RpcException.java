package com.meidusa.venus.exception;

/**
 * rpc调用异常
 * Created by Zhangzhihua on 2017/7/27.
 */
public class RpcException extends RuntimeException {

    private static final long serialVersionUID = 7815426752583648734L;

    //业务异常
    public static final int BIZ_EXCEPTION = 100;

    //-----------以下为非框架及系统异常---------------
    //未知异常
    public static final int UNKNOWN_EXCEPTION = 200;

    //网络异常
    public static final int NETWORK_EXCEPTION = 300;

    //超时异常
    public static final int TIMEOUT_EXCEPTION = 400;

    //序列化异常
    public static final int SERIALIZATION_EXCEPTION = 500;

    private int code;

    public RpcException() {
        super();
    }

    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }

    public RpcException(String message) {
        super(message);
    }

    public RpcException(Throwable cause) {
        super(cause);
    }

    public RpcException(int code) {
        super();
        this.code = code;
    }

    public RpcException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public RpcException(int code, String message) {
        super(message);
        this.code = code;
    }

    public RpcException(int code, Throwable cause) {
        super(cause);
        this.code = code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public boolean isBiz() {
        return code == BIZ_EXCEPTION;
    }

    public boolean isTimeout() {
        return code == TIMEOUT_EXCEPTION;
    }

    public boolean isNetwork() {
        return code == NETWORK_EXCEPTION;
    }

    public boolean isSerialization() {
        return code == SERIALIZATION_EXCEPTION;
    }
}