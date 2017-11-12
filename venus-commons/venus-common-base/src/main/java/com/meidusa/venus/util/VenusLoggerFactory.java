package com.meidusa.venus.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * venus日志工厂
 * Created by Zhangzhihua on 2017/11/10.
 */
public class VenusLoggerFactory {

    private static Logger defaultLogger = LoggerFactory.getLogger("venus.default");

    private static Logger tracerLogger = LoggerFactory.getLogger("venus.tracer");

    private static Logger exceptionLogger = LoggerFactory.getLogger("venus.exception");

    private static Logger statusLogger = LoggerFactory.getLogger("venus.status");

    public static Logger getDefaultLogger(){
        return defaultLogger;
    }

    public static Logger getExceptionLogger() {
        return exceptionLogger;
    }

    public static Logger getTracerLogger() {
        return tracerLogger;
    }

    public static Logger getStatusLogger() {
        return statusLogger;
    }

}
