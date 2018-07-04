package com.meidusa.venus.backend.filter.valid;

import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.venus.*;
import com.meidusa.venus.backend.ServerInvocation;
import com.meidusa.venus.backend.services.EndpointItem;
import com.meidusa.venus.backend.services.ServiceObject;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.exception.VenusExceptionCodeConstant;
import com.meidusa.venus.io.packet.AbstractServicePacket;
import com.meidusa.venus.io.packet.AbstractServiceRequestPacket;
import com.meidusa.venus.io.packet.ErrorPacket;
import com.meidusa.venus.io.packet.VenusRouterPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;
import com.meidusa.venus.util.Range;

/**
 * 服务端校验处理
 * Created by Zhangzhihua on 2017/8/7.
 */
public class ServerValidFilter implements Filter {

    private static final String TIMEOUT = "waiting-timeout for execution,api=%s,ip=%s,time=%d (ms)";

    @Override
    public void init() throws RpcException {

    }

    @Override
    public Result beforeInvoke(Invocation invocation, URL url) throws RpcException {
        ServerInvocation rpcInvocation = (ServerInvocation)invocation;
        valid(rpcInvocation);
        return null;
    }

    @Override
    public Result throwInvoke(Invocation invocation, URL url, Throwable e) throws RpcException {
        return null;
    }

    @Override
    public Result afterInvoke(Invocation invocation, URL url) throws RpcException {
        return null;
    }

    @Override
    public void destroy() throws RpcException {

    }

    /**
     * 校验请求有效性
     * @param invocation
     * @return
     */
    void valid(ServerInvocation invocation){
        Tuple<Long, byte[]> data = invocation.getData();
        byte[] message = invocation.getMessage();
        byte packetSerializeType = invocation.getPacketSerializeType();
        long waitTime = invocation.getWaitTime();
        VenusRouterPacket routerPacket = invocation.getRouterPacket();
        byte serializeType = invocation.getSerializeType();
        SerializeServiceRequestPacket request = invocation.getServiceRequestPacket();
        final String apiName = request.apiName;
        final EndpointItem endpoint = invocation.getEndpointDef();

        checkVersion(endpoint, request);
        checkActive(endpoint, request);
        checkTimeout(endpoint, request, waitTime,invocation);
    }

    /**
     * 校验超时时间
     * @param endpoint
     * @param request
     * @param waitTime
     * @param invocation
     */
    void checkTimeout(EndpointItem endpoint, AbstractServiceRequestPacket request, long waitTime, ServerInvocation invocation) {
        if (waitTime > endpoint.getTimeWait()) {
            ErrorPacket error = new ErrorPacket();
            AbstractServicePacket.copyHead(request, error);
            error.errorCode = VenusExceptionCodeConstant.INVOCATION_ABORT_WAIT_TIMEOUT;
            error.message = String.format(TIMEOUT, request.apiName,invocation.getLocalHost(),waitTime);
            throw new ErrorPacketWrapperException(error);
        }
    }

    /**
     * 校验开启状态
     * @param endpoint
     * @param request
     */
    void checkActive(EndpointItem endpoint, AbstractServiceRequestPacket request) {
        ServiceObject service = endpoint.getService();
        if (!service.isActive() || !endpoint.isActive()) {
            ErrorPacket error = new ErrorPacket();
            AbstractServicePacket.copyHead(request, error);
            error.errorCode = VenusExceptionCodeConstant.SERVICE_INACTIVE_EXCEPTION;
            StringBuilder buffer = new StringBuilder();
            buffer.append("Service=").append(endpoint.getService().getName());
            if (!service.isActive()) {
                buffer.append(" is not active");
            }

            if (!endpoint.isActive()) {
                buffer.append(", endpoint=").append(endpoint.getName()).append(" is not active");
            }

            error.message = buffer.toString();
            throw new ErrorPacketWrapperException(error);
        }
        return;
    }

    /**
     * 校验版本号
     * @param endpoint
     * @param request
     */
    void checkVersion(EndpointItem endpoint, AbstractServiceRequestPacket request) {
        ServiceObject service = endpoint.getService();

        // service version check
        Range range = service.getSupportVersionRange();
        if (range == null || range.contains(request.serviceVersion)) {
            return;
        } else {
            ErrorPacket error = new ErrorPacket();
            AbstractServicePacket.copyHead(request, error);
            error.errorCode = VenusExceptionCodeConstant.SERVICE_VERSION_NOT_ALLOWD_EXCEPTION;
            error.message = "Service=" + endpoint.getService().getName() + ",version=" + request.serviceVersion + " not allow";
            throw new ErrorPacketWrapperException(error);
        }
    }


    class ErrorPacketWrapperException extends RuntimeException {
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
}
