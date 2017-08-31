package com.meidusa.venus.backend.invoker;

import com.meidusa.venus.Result;
import com.meidusa.venus.backend.invoker.support.ResponseEntityWrapper;
import com.meidusa.venus.backend.invoker.support.ResponseHandler;
import com.meidusa.venus.backend.invoker.support.RpcInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static Logger logger = LoggerFactory.getLogger(VenusServerInvocationListener.class);

    private VenusFrontendConnection conn;

    private VenusRouterPacket routerPacket;

    private ReferenceInvocationListener<T> source;

    private SerializeServiceRequestPacket request;

    private RpcInvocation invocation;

    private boolean isResponsed = false;

    private ResponseHandler responseHandler;

    public boolean isResponsed() {
        return isResponsed;
    }

    public VenusServerInvocationListener(VenusFrontendConnection conn, ReferenceInvocationListener<T> source, SerializeServiceRequestPacket request,
                                         VenusRouterPacket routerPacket, RpcInvocation invocation) {
        this.conn = conn;
        this.source = source;
        this.request = request;
        this.routerPacket = routerPacket;
        this.invocation = invocation;
    }

    @Override
    public void callback(T object) {
        ResponseEntityWrapper responseEntityWrapper = ResponseEntityWrapper.parse(invocation,new Result(object),false);
        try {
            responseHandler.writeResponseForNotify(responseEntityWrapper);
        } catch (Exception e) {
            logger.error("response callback error.",e);
        }
    }

    @Override
    public void onException(Exception e) {
        //TODO 细化异常
        Result result = new Result();
        result.setErrorCode(500);
        result.setErrorMessage(e.getMessage());
        result.setException(e);
        ResponseEntityWrapper responseEntityWrapper = ResponseEntityWrapper.parse(invocation,result,false);
        try {
            responseHandler.writeResponseForNotify(responseEntityWrapper);
        } catch (Exception ex) {
            logger.error("response onException error.",ex);
        }
    }

    public ResponseHandler getResponseHandler() {
        return responseHandler;
    }

    public void setResponseHandler(ResponseHandler responseHandler) {
        this.responseHandler = responseHandler;
    }


}
