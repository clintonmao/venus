package com.chexiang.venus.demo.consumer.model;

/**
 * Created by Zhangzhihua on 2017/9/25.
 */
public class Result {

    private boolean success = true;

    private int errorCode;

    private String errorMsg;

    private Object data;

    public Result(Object data){
        this.data = data;
    }

    public Result(Exception e){
        this.success = false;
        this.errorCode = 500;
        this.errorMsg = e.getMessage();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
