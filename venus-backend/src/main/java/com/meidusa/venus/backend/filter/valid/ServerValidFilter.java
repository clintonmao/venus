package com.meidusa.venus.backend.filter.valid;

import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.venus.*;
import com.meidusa.venus.ErrorPacketWrapperException;
import com.meidusa.venus.ServerInvocation;
import com.meidusa.venus.backend.services.Endpoint;
import com.meidusa.venus.backend.services.Service;
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
        String finalSourceIp = invocation.getFinalSourceIp();
        VenusRouterPacket routerPacket = invocation.getRouterPacket();
        byte serializeType = invocation.getSerializeType();
        SerializeServiceRequestPacket request = invocation.getRequest();
        final String apiName = request.apiName;
        final Endpoint endpoint = invocation.getEndpointDef();

        checkVersion(endpoint, request);
        checkActive(endpoint, request);
        checkTimeout(endpoint, request, waitTime,invocation);
        /*
        __TIMEOUT:{
            if (errorPacket != null) {
                if (resultType == EndpointInvocation.ResultType.NOTIFY) {
                    if(isTimeout){
                        break __TIMEOUT;
                    }
                    if (invocationListener != null) {
                        invocationListener.onException(new ServiceVersionNotAllowException(errorPacket.message));
                    } else {
                        postMessageBack(conn, routerPacket, request, errorPacket);
                    }
                } else {
                    postMessageBack(conn, routerPacket, request, errorPacket);
                }
                if(filte != null){
                    filte.before(request);
                }
                logPerformance(endpoint,UUID.toString(request.traceId),apiName,waitTime,0,conn.getHost(),finalSourceIp,request.clientId,request.clientRequestId,request.parameterMap, errorPacket);
                if(filte != null){
                    filte.after(errorPacket);
                }
            }
        }
        */

    }

    /**
     * 校验超时时间
     * @param endpoint
     * @param request
     * @param waitTime
     * @param invocation
     */
    void checkTimeout(Endpoint endpoint, AbstractServiceRequestPacket request, long waitTime, ServerInvocation invocation) {
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
    void checkActive(Endpoint endpoint, AbstractServiceRequestPacket request) {
        Service service = endpoint.getService();
        if (!service.isActive() || !endpoint.isActive()) {
            ErrorPacket error = new ErrorPacket();
            AbstractServicePacket.copyHead(request, error);
            error.errorCode = VenusExceptionCodeConstant.SERVICE_INACTIVE_EXCEPTION;
            StringBuffer buffer = new StringBuffer();
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
    void checkVersion(Endpoint endpoint, AbstractServiceRequestPacket request) {
        Service service = endpoint.getService();

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
}
