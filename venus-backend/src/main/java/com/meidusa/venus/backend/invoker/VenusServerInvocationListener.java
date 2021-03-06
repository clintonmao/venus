package com.meidusa.venus.backend.invoker;

import com.meidusa.venus.Result;
import com.meidusa.venus.backend.support.ServerResponseWrapper;
import com.meidusa.venus.backend.support.ServerResponseHandler;
import com.meidusa.venus.backend.ServerInvocation;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.slf4j.Logger;

import com.meidusa.venus.io.network.VenusFrontendConnection;
import com.meidusa.venus.io.packet.VenusRouterPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;
import com.meidusa.venus.notify.InvocationListener;
import com.meidusa.venus.notify.ReferenceInvocationListener;

/**
 * 服务端调用回调监听处理，应用于callback类型调用
 * @param <T>
 */
public class VenusServerInvocationListener<T> implements InvocationListener<T> {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    private static Logger tracerLogger = VenusLoggerFactory.getTracerLogger();

    private VenusFrontendConnection conn;

    private VenusRouterPacket routerPacket;

    private ReferenceInvocationListener<T> source;

    private SerializeServiceRequestPacket request;

    private ServerInvocation invocation;

    private boolean isResponsed = false;

    private ServerResponseHandler responseHandler;

    public boolean isResponsed() {
        return isResponsed;
    }

    public VenusServerInvocationListener(VenusFrontendConnection conn, ReferenceInvocationListener<T> source, SerializeServiceRequestPacket request,
                                         VenusRouterPacket routerPacket, ServerInvocation invocation) {
        this.conn = conn;
        this.source = source;
        this.request = request;
        this.routerPacket = routerPacket;
        this.invocation = invocation;
    }

    @Override
    public void callback(T object) {
        Result result = new Result(object);
        ServerResponseWrapper responseEntityWrapper = ServerResponseWrapper.parse(invocation,result,false);
        try {
            if(tracerLogger.isInfoEnabled()){
                tracerLogger.info("send notify response,rpcId:{}.",invocation.getRpcId());
            }
            responseHandler.writeResponseForNotify(responseEntityWrapper);
        } catch (Exception e) {
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error("send notify response error.",e);
            }
        }
    }

    @Override
    public void onException(Exception e) {
        Result result = new Result();
        result.setException(e);
        ServerResponseWrapper responseEntityWrapper = ServerResponseWrapper.parse(invocation,result,false);
        try {
            if(tracerLogger.isInfoEnabled()){
                tracerLogger.info("send notify response,rpcId:{}.",invocation.getRpcId());
            }
            responseHandler.writeResponseForNotify(responseEntityWrapper);
        } catch (Exception ex) {
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error("send notify response error.",ex);
            }
        }
    }

    public ServerResponseHandler getResponseHandler() {
        return responseHandler;
    }

    public void setResponseHandler(ServerResponseHandler responseHandler) {
        this.responseHandler = responseHandler;
    }


}
