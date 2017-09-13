package com.meidusa.venus.backend;

import com.meidusa.venus.io.packet.ErrorPacket;

/**
 * ErrorPacket包装异常，目的将原来ErrorPacket返回值处理改为按抛异常方式来处理异常分支
 * Created by Zhangzhihua on 2017/7/27.
 */
public class ErrorPacketWrapperException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private ErrorPacket errorPacket;

    public ErrorPacketWrapperException() {
    }

    public ErrorPacketWrapperException(String msg) {
        super(msg);
    }

    public ErrorPacketWrapperException(Throwable throwable) {
        super(throwable);
    }

    public ErrorPacketWrapperException(String msg, Throwable throwable) {
        super(msg, throwable);
    }

    public ErrorPacketWrapperException(ErrorPacket errorPacket) {
        super("error packet.");
        this.errorPacket = errorPacket;
    }

    public ErrorPacket getErrorPacket() {
        return errorPacket;
    }

    public void setErrorPacket(ErrorPacket errorPacket) {
        this.errorPacket = errorPacket;
    }
}
