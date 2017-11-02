package com.meidusa.venus;

/**
 * 统一响应对象
 * Created by Zhangzhihua on 2017/7/31.
 */
public class Result {

    private Object result;

    private int errorCode = 0;

    private String errorMessage;

    private Throwable exception;

    public Result(){}

    public Result(Object result){
        this.result = result;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Throwable getException() {
        return exception;
    }

    public Result setException(Throwable exception) {
        this.exception = exception;
        if(this.errorCode == 0){
            this.errorCode = 500;
        }
        return this;
    }
}
