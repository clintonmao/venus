package com.meidusa.venus.bus.handler;

import com.meidusa.toolkit.common.poolable.InvalidVirtualPoolException;
import com.meidusa.toolkit.common.util.Tuple;
import com.meidusa.toolkit.net.BackendConnectionPool;
import com.meidusa.toolkit.net.MessageHandler;
import com.meidusa.toolkit.net.util.InetAddressUtil;
import com.meidusa.toolkit.util.StringUtil;
import com.meidusa.toolkit.util.TimeUtil;
import com.meidusa.venus.bus.network.BusBackendConnection;
import com.meidusa.venus.bus.network.BusFrontendConnection;
import com.meidusa.venus.bus.service.ServiceRemoteManager;
import com.meidusa.venus.bus.util.VenusTrafficCollector;
import com.meidusa.venus.exception.VenusExceptionCodeConstant;
import com.meidusa.venus.io.packet.*;
import com.meidusa.venus.io.support.ShutdownListener;
import com.meidusa.venus.util.Range;
import com.meidusa.venus.util.UUID;
import com.meidusa.venus.util.VenusTracerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 总线消息分发处理,负责接收并分发服务请求
 * 
 * @author structchen
 * 
 */
public class BusMessageDispatcherHandler extends BusFrontendMessageHandler implements MessageHandler<BusFrontendConnection, byte[]> {

    private static Logger logger = LoggerFactory.getLogger(BusMessageDispatcherHandler.class);

    private static ShutdownListener listener = new ShutdownListener();

    private static Logger REUEST_LOGGER = LoggerFactory.getLogger("venus.tracer");

    static {
        Runtime.getRuntime().addShutdownHook(listener);
    }

    private ServiceRemoteManager remoteManager;

    public ServiceRemoteManager getRemoteManager() {
        return remoteManager;
    }

    public void setRemoteManager(ServiceRemoteManager remoteManager) {
        this.remoteManager = remoteManager;
    }

    @Override
    public void handle(BusFrontendConnection srcConn, final byte[] message) {
    	VenusTrafficCollector.getInstance().addInput(message.length);
        int type = AbstractServicePacket.getType(message);
        switch (type) {
            case PacketConstant.PACKET_TYPE_PING:
                super.handle(srcConn, message);
                break;
            // ignore this packet
            case PacketConstant.PACKET_TYPE_PONG:

                break;
            case PacketConstant.PACKET_TYPE_VENUS_STATUS_REQUEST:
                super.handle(srcConn, message);
                break;
            case PacketConstant.PACKET_TYPE_SERVICE_REQUEST: {
                ServicePacketBuffer packetBuffer = new ServicePacketBuffer(message);
                VenusTrafficCollector.getInstance().increaseRequest();
                try {
                    VenusRouterPacket routerPacket = new VenusRouterPacket();
                    routerPacket.srcIP = InetAddressUtil.pack(srcConn.getInetAddress().getAddress());
                    routerPacket.data = message;
                    routerPacket.startTime = TimeUtil.currentTimeMillis();
                    routerPacket.frontendConnectionID = srcConn.getSequenceID();
                    routerPacket.frontendRequestID = srcConn.getNextRequestID();
                    routerPacket.serializeType = srcConn.getSerializeType();
                    try {
                        packetBuffer.skip(PacketConstant.SERVICE_HEADER_SIZE + 8);
                        final String apiName = packetBuffer.readLengthCodedString(PacketConstant.PACKET_CHARSET);
                        final int serviceVersion = packetBuffer.readInt();
                        int index = apiName.lastIndexOf(".");
                        String serviceName = apiName.substring(0, index);
                        // String methodName = apiName.substring(index + 1);
                        List<Tuple<Range, BackendConnectionPool>> list = remoteManager.getRemoteList(serviceName);
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
                        
                        // service not found
                        if (list == null || list.size() == 0) {
                            ServiceAPIPacket apiPacket = new ServiceAPIPacket();
                            packetBuffer.reset();
                            apiPacket.init(packetBuffer);
                            ErrorPacket error = new ErrorPacket();
                            AbstractServicePacket.copyHead(apiPacket, error);
                            error.errorCode = VenusExceptionCodeConstant.SERVICE_NOT_FOUND;
                            error.message = "service not found :" + serviceName;
                            srcConn.write(error.toByteBuffer());
                            return;
                        }

                        for (Tuple<Range, BackendConnectionPool> tuple : list) {

                            if (tuple.left.contains(serviceVersion)) {
                                BusBackendConnection remoteConn = null;
                                try {
                                    remoteConn = (BusBackendConnection) tuple.right.borrowObject();
                                    routerPacket.backendRequestID = remoteConn.getNextRequestID();
                                    remoteConn.addRequest(routerPacket.backendRequestID, routerPacket.frontendConnectionID, routerPacket.frontendRequestID);
                                    srcConn.addUnCompleted(routerPacket.frontendRequestID, routerPacket);
                                    remoteConn.write(VenusRouterPacket.toByteBuffer(routerPacket));
                                    VenusTracerUtil.logRouter(traceId, apiName, srcConn.getInetAddress().getHostAddress(), remoteConn.getHost()+":"+remoteConn.getPort());
                                    return;
                                } catch(InvalidVirtualPoolException e){
                                	ServiceAPIPacket apiPacket = new ServiceAPIPacket();
                                    packetBuffer.reset();
                                    apiPacket.init(packetBuffer);
                                	ErrorPacket error = new ErrorPacket();
                                    AbstractServicePacket.copyHead(apiPacket, error);
                                    error.errorCode = VenusExceptionCodeConstant.SERVICE_UNAVAILABLE_EXCEPTION;
                                    error.message = e.getMessage();
                                    srcConn.write(error.toByteBuffer());
                                    return;
                                }catch (Exception e) {
                                	srcConn.addUnCompleted(routerPacket.frontendRequestID, routerPacket);
                                    srcConn.getRetryHandler().addRetry(srcConn, routerPacket);
                                    return;
                                } finally {
                                    if (remoteConn != null) {
                                        tuple.right.returnObject(remoteConn);
                                    }
                                }
                            }
                        }

                        // Service version not match

                        ServiceAPIPacket apiPacket = new ServiceAPIPacket();
                        packetBuffer.reset();
                        apiPacket.init(packetBuffer);

                        ErrorPacket error = new ErrorPacket();
                        AbstractServicePacket.copyHead(apiPacket, error);
                        error.errorCode = VenusExceptionCodeConstant.SERVICE_VERSION_NOT_ALLOWD_EXCEPTION;
                        error.message = "Service version not match";
                        srcConn.write(error.toByteBuffer());

                    } catch (Exception e) {
                        ServiceAPIPacket apiPacket = new ServiceAPIPacket();
                        packetBuffer.reset();
                        apiPacket.init(packetBuffer);

                        logger.error("decode error", e);
                        ErrorPacket error = new ErrorPacket();
                        AbstractServicePacket.copyHead(apiPacket, error);
                        error.errorCode = VenusExceptionCodeConstant.PACKET_DECODE_EXCEPTION;
                        error.message = "decode packet exception:" + e.getMessage();
                        srcConn.write(error.toByteBuffer());
                        return;
                    }

                } catch (Exception e) {
                    ServiceAPIPacket apiPacket = new ServiceAPIPacket();
                    packetBuffer.reset();
                    apiPacket.init(packetBuffer);

                    ErrorPacket error = new ErrorPacket();
                    AbstractServicePacket.copyHead(apiPacket, error);
                    error.errorCode = VenusExceptionCodeConstant.SERVICE_UNAVAILABLE_EXCEPTION;
                    error.message = e.getMessage();
                    srcConn.write(error.toByteBuffer());
                    logger.error("error when invoke", e);
                    return;
                } catch (Error e) {

                    ServiceAPIPacket apiPacket = new ServiceAPIPacket();
                    packetBuffer.reset();
                    apiPacket.init(packetBuffer);

                    ErrorPacket error = new ErrorPacket();
                    AbstractServicePacket.copyHead(apiPacket, error);
                    error.errorCode = VenusExceptionCodeConstant.SERVICE_UNAVAILABLE_EXCEPTION;
                    error.message = e.getMessage();
                    srcConn.write(error.toByteBuffer());
                    logger.error("error when invoke", e);
                    return;
                }
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

}
