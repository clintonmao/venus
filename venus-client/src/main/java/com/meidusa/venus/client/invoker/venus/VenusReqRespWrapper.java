package com.meidusa.venus.client.invoker.venus;

import com.meidusa.toolkit.net.BackendConnection;
import com.meidusa.venus.ClientInvocation;
import com.meidusa.venus.Result;
import com.meidusa.venus.io.packet.AbstractServicePacket;

import java.util.concurrent.CountDownLatch;

/**
 * venus协议请求响应包装类
 * Created by Zhangzhihua on 2017/10/24.
 */
public class VenusReqRespWrapper {

    //请求对象
    private ClientInvocation invocation;

    //当前请求使用连接
    private BackendConnection backendConnection;

    //响应结果
    private Result result = null;

    //计数latch
    private CountDownLatch reqRespLatch = new CountDownLatch(1);

    public VenusReqRespWrapper(ClientInvocation invocation) {
        this.invocation = invocation;
    }

    public ClientInvocation getInvocation() {
        return invocation;
    }

    public void setInvocation(ClientInvocation invocation) {
        this.invocation = invocation;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public CountDownLatch getReqRespLatch() {
        return reqRespLatch;
    }

    public void setReqRespLatch(CountDownLatch reqRespLatch) {
        this.reqRespLatch = reqRespLatch;
    }

    public BackendConnection getBackendConnection() {
        return backendConnection;
    }

    public void setBackendConnection(BackendConnection backendConnection) {
        this.backendConnection = backendConnection;
    }
}
