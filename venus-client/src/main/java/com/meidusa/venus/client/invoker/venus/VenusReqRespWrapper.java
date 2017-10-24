package com.meidusa.venus.client.invoker.venus;

import com.meidusa.venus.ClientInvocation;
import com.meidusa.venus.io.packet.AbstractServicePacket;

import java.util.concurrent.CountDownLatch;

/**
 * venus协议请求响应包装类
 * Created by Zhangzhihua on 2017/10/24.
 */
public class VenusReqRespWrapper {

    //请求对象
    private ClientInvocation invocation;

    //响应报文
    private AbstractServicePacket packet;


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

    public AbstractServicePacket getPacket() {
        return packet;
    }

    public void setPacket(AbstractServicePacket packet) {
        this.packet = packet;
    }

    public CountDownLatch getReqRespLatch() {
        return reqRespLatch;
    }

    public void setReqRespLatch(CountDownLatch reqRespLatch) {
        this.reqRespLatch = reqRespLatch;
    }
}
