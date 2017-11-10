package com.meidusa.venus.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * venus日志工厂
 * Created by Zhangzhihua on 2017/11/10.
 */
public class VenusLoggerFactory {

    //client
    private static Logger clientDefaultLogger = LoggerFactory.getLogger("venus.client.default");
    private static Logger clientTracerLogger = LoggerFactory.getLogger("venus.client.tracer");
    private static Logger clientExceptionLogger = LoggerFactory.getLogger("venus.client.exception");

    //backend
    private static Logger backendDefaultLogger = LoggerFactory.getLogger("venus.backend.default");
    private static Logger backendTracerLogger = LoggerFactory.getLogger("venus.backend.tracer");
    private static Logger backendExceptionLogger = LoggerFactory.getLogger("venus.backend.exception");

    //common
    private static Logger statusLogger = LoggerFactory.getLogger("venus.status");

    public static Logger getClientDefaultLogger(){
        return clientDefaultLogger;
    }

    public static Logger getClientTracerLogger() {
        return clientTracerLogger;
    }

    public static Logger getClientExceptionLogger() {
        return clientExceptionLogger;
    }

    public static Logger getBackendDefaultLogger() {
        return backendDefaultLogger;
    }

    public static Logger getBackendTracerLogger() {
        return backendTracerLogger;
    }

    public static Logger getBackendExceptionLogger() {
        return backendExceptionLogger;
    }

    public static Logger getStatusLogger() {
        return statusLogger;
    }
}
