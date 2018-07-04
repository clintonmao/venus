package com.chexiang.venus.demo.provider.exception;

import com.meidusa.venus.exception.CodedException;
import com.meidusa.venus.exception.ExceptionLevel;
import com.meidusa.venus.exception.VenusExceptionLevel;

/**
 * Created by Zhangzhihua on 2018/1/9.
 */
public class InvalidParamException extends Exception implements CodedException, VenusExceptionLevel {

    /**
     * 自动生成版本号
     */
    private static final long serialVersionUID = -7480530666583360597L;

    /**
     * 异常编码
     */
    private int errorCode;

    /**
     * 异常信息
     */
    private String errorMsg;

    /**
     * 默认构造函数,初始化errorCode和errorMsg
     */
    public InvalidParamException() {
        this.errorCode = 100001;
        this.errorMsg = "invalid request.";
    }

    /**
     * @return the errorCode
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * @param errorCode the errorCode to set
     */
    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * @return the errorMsg
     */
    public String getErrorMsg() {
        return errorMsg;
    }

    /**
     * @param errorMsg the errorMsg to set
     */
    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    @Override
    public String getMessage() {
        return this.errorMsg;
    }

    /**
     * @return the ExceptionLevel.ERROR
     */
    public ExceptionLevel getLevel() {
        return ExceptionLevel.ERROR;
    }

}
