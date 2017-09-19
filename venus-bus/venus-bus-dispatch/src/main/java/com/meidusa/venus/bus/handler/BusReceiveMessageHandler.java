package com.meidusa.venus.bus.handler;

import com.meidusa.toolkit.net.MessageHandler;
import com.meidusa.toolkit.net.util.InetAddressUtil;
import com.meidusa.toolkit.util.StringUtil;
import com.meidusa.toolkit.util.TimeUtil;
import com.meidusa.venus.Result;
import com.meidusa.venus.backend.ErrorPacketWrapperException;
import com.meidusa.venus.bus.BusInvocation;
import com.meidusa.venus.bus.dispatch.BusDispatcherProxy;
import com.meidusa.venus.bus.network.BusFrontendConnection;
import com.meidusa.venus.bus.registry.ServiceManager;
import com.meidusa.venus.bus.util.VenusTrafficCollector;
import com.meidusa.venus.client.VenusRegistryFactory;
import com.meidusa.venus.exception.VenusExceptionCodeConstant;
import com.meidusa.venus.io.packet.*;
import com.meidusa.venus.io.support.ShutdownListener;
import com.meidusa.venus.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 接收客户端请求消息处理
 * 
 * @author structchen
 * 
 */
public class BusReceiveMessageHandler extends BusFrontendMessageHandler implements MessageHandler<BusFrontendConnection, byte[]> {

    private static Logger logger = LoggerFactory.getLogger(BusReceiveMessageHandler.class);

    private static ShutdownListener listener = new ShutdownListener();

    private static Logger REUEST_LOGGER = LoggerFactory.getLogger("venus.tracer");

    static {
        Runtime.getRuntime().addShutdownHook(listener);
    }

    private ServiceManager serviceManager;

    private VenusRegistryFactory venusRegistryFactory;

    private BusDispatcherProxy busDispatcherProxy;

    //TODO 将连接统一为VenusFrontendConnection，可能出现不兼容问题?
    @Override
    public void handle(BusFrontendConnection srcConn, final byte[] message) {
    	VenusTrafficCollector.getInstance().addInput(message.length);
        int type = AbstractServicePacket.getType(message);
        switch (type) {
            case PacketConstant.PACKET_TYPE_PING:
                super.handle(srcConn, message);
                break;
            case PacketConstant.PACKET_TYPE_PONG:
                // ignore this packet
                break;
            case PacketConstant.PACKET_TYPE_VENUS_STATUS_REQUEST:
                super.handle(srcConn, message);
                break;
            case PacketConstant.PACKET_TYPE_SERVICE_REQUEST: {
                //TODO 多线程？
                doHandle(srcConn, message);
                break;
            }
            case PacketConstant.AUTHEN_TYPE_PASSWORD:
                break;
            default:
                StringBuilder buffer = new StringBuilder("receive unknown type packet from ");
                buffer.append(srcConn.getId()).append("\n");
                buffer.append("-------------------------------").append("\n");
                buffer.append(StringUtil.dumpAsHex(message, message.length)).append("\n");
                buffer.append("-------------------------------").append("\n");
                logger.warn(buffer.toString());

        }
    }

    /**
     * 处理请求消息
     * @param srcConn
     * @param message
     */
    public void doHandle(BusFrontendConnection srcConn, final byte[] message) {
        BusInvocation invocation = null;
        Result result = null;
        try {
            //解析请求
            invocation = parseInvocation(srcConn, message);

            //通过分发代理分发消息 TODO 通过线程池处理
            result = getBusDispatcherProxy().invoke(invocation,null);
        } catch (Exception e) {
            //TODO 异常信息包装
            result = new Result();
            result.setErrorCode(500);
            result.setErrorMessage(e.getMessage());
            result.setException(e);
        }

        //若寻址路由出错或者分发异常等，则直接返回
        if(result != null){
            //TODO 输出响应
        }
    }

    /**
     * 解析并构造请求对象
     * @param srcConn
     * @param message
     * @return
     */
    BusInvocation parseInvocation(BusFrontendConnection srcConn, final byte[] message){
        VenusTrafficCollector.getInstance().increaseRequest();

        BusInvocation busInvocation = new BusInvocation();

        ServicePacketBuffer packetBuffer = new ServicePacketBuffer(message);

        try {
            VenusRouterPacket routerPacket = new VenusRouterPacket();
            routerPacket.srcIP = InetAddressUtil.pack(srcConn.getInetAddress().getAddress());
            routerPacket.data = message;
            routerPacket.startTime = TimeUtil.currentTimeMillis();
            routerPacket.frontendConnectionID = srcConn.getSequenceID();
            routerPacket.frontendRequestID = srcConn.getNextRequestID();
            routerPacket.serializeType = srcConn.getSerializeType();

            //解析服务信息
            packetBuffer.skip(PacketConstant.SERVICE_HEADER_SIZE + 8);
            final String apiName = packetBuffer.readLengthCodedString(PacketConstant.PACKET_CHARSET);
            final int serviceVersion = packetBuffer.readInt();
            //TODO 新增serviceInterfaceName、version兼容性处理
            int index = apiName.lastIndexOf(".");
            String serviceName = apiName.substring(0, index);
            // String methodName = apiName.substring(index + 1);
            routerPacket.api = apiName;

            /**
             * 解析traceID
             * 跳过参数字节
             */
            packetBuffer.skipLengthCodedBytes();
            byte[] traceId = new byte[16];
            // 兼容3.0.1之前的版本,3.0.2与之后的版本将支持traceID
            if (packetBuffer.hasRemaining()) {
                packetBuffer.readBytes(traceId, 0, 16);
            } else {
                traceId = PacketConstant.EMPTY_TRACE_ID;
            }
            routerPacket.traceId = UUID.toString(traceId);

            busInvocation.setRouterPacket(routerPacket);
            //List<Tuple<Range, BackendConnectionPool>> list = remoteManager.getRemoteList(serviceName);
            return busInvocation;
        } catch (Exception e) {
            ServiceAPIPacket apiPacket = new ServiceAPIPacket();
            packetBuffer.reset();
            apiPacket.init(packetBuffer);

            logger.error("decode error", e);
            ErrorPacket error = new ErrorPacket();
            AbstractServicePacket.copyHead(apiPacket, error);
            error.errorCode = VenusExceptionCodeConstant.PACKET_DECODE_EXCEPTION;
            error.message = "decode packet exception:" + e.getMessage();
            //最后统一响应 fixed
            //srcConn.write(error.toByteBuffer());
            throw new ErrorPacketWrapperException(error);
        }
    }

    public BusDispatcherProxy getBusDispatcherProxy() {
        if(busDispatcherProxy != null){
            return busDispatcherProxy;
        }
        busDispatcherProxy = new BusDispatcherProxy();
        if(serviceManager != null){
            busDispatcherProxy.setServiceManager(serviceManager);
        }
        if(venusRegistryFactory != null){
            busDispatcherProxy.setVenusRegistryFactory(venusRegistryFactory);
        }
        return busDispatcherProxy;
    }

    /**
     * 校验版本号是否可用 TODO 放到寻址中实现，尽量与client复用
     * @param invocation
     */
    void validVersion(BusInvocation invocation){
        // Service version not match
        ServiceAPIPacket apiPacket = new ServiceAPIPacket();
        invocation.getPacketBuffer().reset();
        apiPacket.init(invocation.getPacketBuffer());

        ErrorPacket error = new ErrorPacket();
        AbstractServicePacket.copyHead(apiPacket, error);
        error.errorCode = VenusExceptionCodeConstant.SERVICE_VERSION_NOT_ALLOWD_EXCEPTION;
        error.message = "Service version not match";
    }

    public ServiceManager getServiceManager() {
        return serviceManager;
    }

    public void setServiceManager(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    public VenusRegistryFactory getVenusRegistryFactory() {
        return venusRegistryFactory;
    }

    public void setVenusRegistryFactory(VenusRegistryFactory venusRegistryFactory) {
        this.venusRegistryFactory = venusRegistryFactory;
    }
}