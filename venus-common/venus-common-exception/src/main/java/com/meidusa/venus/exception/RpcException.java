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

    private int errorCode;

    public RpcException() {
        super();
    }

    public RpcException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = UNKNOWN_EXCEPTION;
    }

    public RpcException(String message) {
        super(message);
        this.errorCode = UNKNOWN_EXCEPTION;
    }

    public RpcException(Throwable cause) {
        super(cause);
        this.errorCode = UNKNOWN_EXCEPTION;
    }

    public RpcException(int code, String message) {
        super(message);
        this.errorCode = code;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public boolean isBiz() {
        return errorCode == BIZ_EXCEPTION;
    }

    public boolean isTimeout() {
        return errorCode == TIMEOUT_EXCEPTION;
    }

    public boolean isNetwork() {
        return errorCode == NETWORK_EXCEPTION;
    }

    public boolean isSerialization() {
        return errorCode == SERIALIZATION_EXCEPTION;
    }
}