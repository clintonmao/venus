package com.meidusa.venus.bus.handler;

import com.meidusa.toolkit.net.MessageHandler;
import com.meidusa.toolkit.util.StringUtil;
import com.meidusa.venus.Result;
import com.meidusa.venus.ErrorPacketWrapperException;
import com.meidusa.venus.bus.BusInvocation;
import com.meidusa.venus.bus.dispatch.BusDispatcherProxy;
import com.meidusa.venus.bus.network.BusFrontendConnection;
import com.meidusa.venus.bus.support.BusResponseHandler;
import com.meidusa.venus.bus.util.VenusTrafficCollector;
import com.meidusa.venus.registry.VenusRegistryFactory;
import com.meidusa.venus.exception.VenusExceptionCodeConstant;
import com.meidusa.venus.io.packet.*;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;
import com.meidusa.venus.io.serializer.Serializer;
import com.meidusa.venus.io.serializer.SerializerFactory;
import com.meidusa.venus.io.support.ShutdownListener;
import com.meidusa.venus.io.utils.RpcIdUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
        init();
    }

    static void init(){
        Runtime.getRuntime().addShutdownHook(listener);
        //初始化序列化配置
        SerializerFactory.init();
    }

    private VenusRegistryFactory venusRegistryFactory;

    private BusDispatcherProxy busDispatcherProxy;

    /**
     * 请求连接映射表 TODO 处理连接中断等情况，使用类旧版本observer机制或者定期清理机制?
     */
    private Map<String,BusFrontendConnection> requestConnectionMap = new ConcurrentHashMap<String,BusFrontendConnection>();

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

    Serializer serializer = SerializerFactory.getSerializer(PacketConstant.CONTENT_TYPE_JSON);

    /**
     * 处理请求消息
     * @param srcConn
     * @param message
     */
    public void doHandle(BusFrontendConnection srcConn, final byte[] message) {
        BusInvocation invocation = null;
        Result result = null;
        try {
            //TODO 内容不需要反序列化?
            SerializeServiceRequestPacket serviceRequestPacket = new SerializeServiceRequestPacket(serializer, null);
            serviceRequestPacket.init(message);

            //TODO 处理使用过清理问题
            String rpcId = RpcIdUtil.getRpcId(serviceRequestPacket);
            requestConnectionMap.put(rpcId,srcConn);

            //解析请求
            invocation = parseInvocation(srcConn, message,serviceRequestPacket);

            //通过分发代理分发消息
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
            //TODO 细化异常处理
            ErrorPacket errorPacket = new ErrorPacket();
            errorPacket.errorCode = result.getErrorCode();
            errorPacket.message = result.getErrorMessage();
            srcConn.write(errorPacket.toByteBuffer());
            BusResponseHandler.writeResponseForError(srcConn,errorPacket);
        }
    }

    /**
     * 解析并构造请求对象
     * @param conn
     * @param message
     * @return
     */
    BusInvocation parseInvocation(BusFrontendConnection conn, final byte[] message,SerializeServiceRequestPacket serviceRequestPacket){
        BusInvocation invocation = new BusInvocation();

        VenusTrafficCollector.getInstance().increaseRequest();
        ServicePacketBuffer packetBuffer = new ServicePacketBuffer(message);

        try {
            /*
            VenusRouterPacket routerPacket = new VenusRouterPacket();
            routerPacket.frontendConnectionID = srcConn.getSequenceID();
            routerPacket.frontendRequestID = srcConn.getNextRequestID();
            routerPacket.data = message;
            routerPacket.srcIP = InetAddressUtil.pack(srcConn.getInetAddress().getAddress());
            routerPacket.startTime = TimeUtil.currentTimeMillis();
            routerPacket.serializeType = srcConn.getSerializeType();
            */
            invocation.setSrcConn(conn);
            invocation.setMessage(message);
            invocation.setClientId(conn.getSequenceID());
            invocation.setClientRequestId(conn.getSequenceID());
            invocation.setSerializeType(conn.getSerializeType());
            invocation.setRequestTime(new Date());
            invocation.setConsumerIp(conn.getInetAddress().getHostAddress());

            //解析服务信息
            packetBuffer.skip(PacketConstant.SERVICE_HEADER_SIZE + 8);
            String apiName = packetBuffer.readLengthCodedString(PacketConstant.PACKET_CHARSET);
            int index = apiName.lastIndexOf(".");
            String serviceName = apiName.substring(0, index);
            invocation.setServiceName(serviceName);
            String methodName = apiName.substring(index + 1);
            invocation.setMethodName(methodName);
            //TODO 少serviceInterfaceName属性
            //routerPacket.api = apiName;
            int version = packetBuffer.readInt();
            //TODO version是1而不是1.0.0形式
            //解析traceID
            packetBuffer.skipLengthCodedBytes();
            byte[] traceId = new byte[16];
            // 兼容3.0.1之前的版本,3.0.2与之后的版本将支持traceID
            if (packetBuffer.hasRemaining()) {
                packetBuffer.readBytes(traceId, 0, 16);
            } else {
                traceId = PacketConstant.EMPTY_TRACE_ID;
            }
            //routerPacket.traceId = UUID.toString(traceId);

            //busInvocation.setRouterPacket(routerPacket);
            //List<Tuple<Range, BackendConnectionPool>> list = remoteManager.getRemoteList(serviceName);
            return invocation;
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
        if(venusRegistryFactory != null){
            busDispatcherProxy.setVenusRegistryFactory(venusRegistryFactory);
        }
        if(requestConnectionMap != null){
            busDispatcherProxy.setRequestConnectionMap(requestConnectionMap);
        }
        return busDispatcherProxy;
    }

    /**
     * 校验版本号是否可用 TODO 功能由路由filter替代
     * @param invocation
     */
    void validVersion(BusInvocation invocation){
        // Service version not match
        ServiceAPIPacket apiPacket = new ServiceAPIPacket();
        //invocation.getPacketBuffer().reset();
        //apiPacket.init(invocation.getPacketBuffer());

        ErrorPacket error = new ErrorPacket();
        AbstractServicePacket.copyHead(apiPacket, error);
        error.errorCode = VenusExceptionCodeConstant.SERVICE_VERSION_NOT_ALLOWD_EXCEPTION;
        error.message = "Service version not match";
    }

    public VenusRegistryFactory getVenusRegistryFactory() {
        return venusRegistryFactory;
    }

    public void setVenusRegistryFactory(VenusRegistryFactory venusRegistryFactory) {
        this.venusRegistryFactory = venusRegistryFactory;
    }
}
