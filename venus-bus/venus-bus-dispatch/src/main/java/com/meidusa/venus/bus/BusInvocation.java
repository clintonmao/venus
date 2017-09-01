package com.meidusa.venus.bus;

import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.toolkit.net.BackendConnectionPool;
import com.meidusa.venus.Invocation;
import com.meidusa.venus.backend.services.Endpoint;
import com.meidusa.venus.backend.services.EndpointInvocation;
import com.meidusa.venus.bus.network.BusFrontendConnection;
import com.meidusa.venus.io.network.VenusFrontendConnection;
import com.meidusa.venus.io.packet.ServicePacketBuffer;
import com.meidusa.venus.io.packet.VenusRouterPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;
import com.meidusa.venus.util.Range;

import java.util.List;

/**
 * bus调用对象，TODO 统一invocation
 * Created by Zhangzhihua on 2017/8/2.
 */
public class BusInvocation extends Invocation{

    BusFrontendConnection srcConn;

    ServicePacketBuffer packetBuffer;

    VenusRouterPacket routerPacket;

    String apiName;

    String serviceName ;

    int serviceVersion;

    List<Tuple<Range, BackendConnectionPool>> list;

    byte[] traceId;

    public VenusRouterPacket getRouterPacket() {
        return routerPacket;
    }

    public void setRouterPacket(VenusRouterPacket routerPacket) {
        this.routerPacket = routerPacket;
    }

    public BusFrontendConnection getSrcConn() {
        return srcConn;
    }

    public void setSrcConn(BusFrontendConnection srcConn) {
        this.srcConn = srcConn;
    }

    public ServicePacketBuffer getPacketBuffer() {
        return packetBuffer;
    }

    public void setPacketBuffer(ServicePacketBuffer packetBuffer) {
        this.packetBuffer = packetBuffer;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public int getServiceVersion() {
        return serviceVersion;
    }

    public void setServiceVersion(int serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    public List<Tuple<Range, BackendConnectionPool>> getList() {
        return list;
    }

    public void setList(List<Tuple<Range, BackendConnectionPool>> list) {
        this.list = list;
    }

    public byte[] getTraceId() {
        return traceId;
    }

    public void setTraceId(byte[] traceId) {
        this.traceId = traceId;
    }
}
