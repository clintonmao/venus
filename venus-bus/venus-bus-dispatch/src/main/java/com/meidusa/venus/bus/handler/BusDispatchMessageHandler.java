package com.meidusa.venus.bus.handler;

import com.meidusa.toolkit.net.MessageHandler;
import com.meidusa.toolkit.net.util.InetAddressUtil;
import com.meidusa.toolkit.util.TimeUtil;
import com.meidusa.venus.ClientInvocation;
import com.meidusa.venus.RpcException;
import com.meidusa.venus.bus.network.BusBackendConnection;
import com.meidusa.venus.bus.network.BusFrontendConnection;
import com.meidusa.venus.bus.packet.SimpleServiceResponsePacket;
import com.meidusa.venus.bus.util.VenusTrafficCollector;
import com.meidusa.venus.io.packet.*;
import com.meidusa.venus.io.packet.serialize.SerializeServiceNofityPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceResponsePacket;
import com.meidusa.venus.io.serializer.Serializer;
import com.meidusa.venus.io.serializer.SerializerFactory;
import com.meidusa.venus.io.utils.RpcIdUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 向服务端分发消息响应处理
 * 
 * @author structchen
 * 
 */
public class BusDispatchMessageHandler extends BusBackendMessageHandler implements MessageHandler<BusBackendConnection, byte[]> {

	final static Logger logger = LoggerFactory.getLogger("venus.backend.performance");

    private ClientConnectionObserver clientConnectionObserver;

    private Map<String,BusFrontendConnection> requestConnectionMap;

    @Override
    public void handle(BusBackendConnection conn,byte[] message) {
        VenusTrafficCollector.getInstance().addInput(message.length);
        int type = AbstractServicePacket.getType(message);
        logger.info("bus dispatch recevier msg,type:{}.",type);
        Serializer serializer = SerializerFactory.getSerializer(conn.getSerializeType());

        switch (type) {
            case PacketConstant.PACKET_TYPE_ERROR:
                ErrorPacket error = new ErrorPacket();
                error.init(message);
                logger.info("recv error response,conn:{}.",conn);
                logger.info("recv error response,clientId:{},clientRequestId:{},response:{}.",error.clientId,error.clientRequestId,error);
                writeResponse(error);
                break;
            case PacketConstant.PACKET_TYPE_OK:
                OKPacket ok = new OKPacket();
                ok.init(message);
                logger.info("recv ok response,clientId:{},clientRequestId:{},response:{}.",ok.clientId,ok.clientRequestId,ok);
                writeResponse(ok);
                break;
            case PacketConstant.PACKET_TYPE_SERVICE_RESPONSE:
                //获取clientId/clientRequestId，用于获取invocation请求信息
                //ClientInvocation syncInvocation = serviceInvocationMap.get(RpcIdUtil.getRpcId(parseServicePacket(message)));

                //TODO 获取返回javaType
                ServiceResponsePacket response = new SerializeServiceResponsePacket(serializer, null);
                response.init(message);
                logger.info("recv resp response,conn:{}.",conn);
                logger.info("recv resp response,clientId:{},clientRequestId:{},response:{}.",response.clientId,response.clientRequestId,response);
                //添加rpcId->response映射表
                //TODO 处理已经超时的记录
                writeResponse(response);
                break;
            default:
                super.handle(conn, message);
        }

    }


    /**
     * 输出响应
     * @param object
     */
    void writeResponse(Object object){
        //TODO 与client响应改为统一
        if(object instanceof ErrorPacket){
            ErrorPacket errorPacket = (ErrorPacket)object;
            String rpcId = RpcIdUtil.getRpcId(errorPacket);
            BusFrontendConnection busFrontendConnection = requestConnectionMap.get(rpcId);
            //TODO 输出响应，统一输出机制，与dispatchMesgHandler输出机制统一
            //busFrontendConnection.write();
        }else if(object instanceof OKPacket){
            OKPacket okPacket = (OKPacket)object;
            String rpcId = RpcIdUtil.getRpcId(okPacket);
            BusFrontendConnection busFrontendConnection = requestConnectionMap.get(rpcId);
        }else if(object instanceof SerializeServiceResponsePacket){
            SerializeServiceResponsePacket serializeServiceResponsePacket = (SerializeServiceResponsePacket)object;
            String rpcId = RpcIdUtil.getRpcId(serializeServiceResponsePacket);
            BusFrontendConnection busFrontendConnection = requestConnectionMap.get(rpcId);

        }
        //RpcIdUtil.getRpcId()
    }

    /**
     * 解析基本报文信息
     * @param message
     * @return
     */
    AbstractServicePacket parseServicePacket(byte[] message){
        OKPacket okPacket = new OKPacket();
        okPacket.init(message);
        return okPacket;
    }

//    @Override
//    public void handle(BusBackendConnection conn,final byte[] message) {
//    	VenusTrafficCollector.getInstance().addInput(message.length);
//        int type = AbstractServicePacket.getType(message);
//        logger.info("bus dispatch recevier msg,type:{}.",type);
//        if (type == AbstractVenusPacket.PACKET_TYPE_ROUTER) {
//            //TODO 根据rpcId来获取对应conn
//            BusFrontendConnection clientConn = (BusFrontendConnection) clientConnectionObserver.getConnection(VenusRouterPacket
//                    .getConnectionSequenceID(message));
//            conn.removeRequest(VenusRouterPacket.getRemoteRequestID(message));
//            byte[] response = VenusRouterPacket.getData(message);
//            if (clientConn != null) {
//            	VenusRouterPacket router = clientConn.removeUnCompleted(VenusRouterPacket.getSourceRequestID(message));
//            	if(router != null){
//            		if(logger.isDebugEnabled()){
//            			long cost = (TimeUtil.currentTimeMillis()-router.startTime);
//            			logger.debug("[{}] traceId={}, api={}, srcIP={}, destIP={}, closed={}",cost, router.traceId,router.api,InetAddressUtil.intToAddress(router.srcIP),conn.getHost(),clientConn.isClosed());
//
//            		}
//            		if(!clientConn.isClosed()){
//            			clientConn.write(response);
//            		}
//                }else{
//                	VenusRouterPacket routerResp = new VenusRouterPacket();
//                	routerResp.init(message);
//                	SimpleServiceResponsePacket packet = new SimpleServiceResponsePacket();
//                	packet.init(response);
//        			logger.error("*abandoned* requestId={}, srcIP={}, destIP={}",packet.clientRequestId,InetAddressUtil.intToAddress(routerResp.srcIP),conn.getHost());
//                }
//            }else{
//            	VenusRouterPacket routerResp = new VenusRouterPacket();
//            	routerResp.init(message);
//            	SimpleServiceResponsePacket packet = new SimpleServiceResponsePacket();
//            	packet.init(response);
//    			logger.error("*abandoned* requestId={}, srcIP={}, destIP={}",packet.clientRequestId,InetAddressUtil.intToAddress(routerResp.srcIP),conn.getHost());
//            }
//        }else if(type == AbstractVenusPacket.PACKET_TYPE_SERVICE_RESPONSE){
//            BusFrontendConnection clientConn = (BusFrontendConnection) clientConnectionObserver.getConnection(VenusRouterPacket
//                    .getConnectionSequenceID(message));
//        }else if(type == AbstractVenusPacket.PACKET_TYPE_PONG){
//        	super.handle(conn, message);
//        }
//
//    }

    public ClientConnectionObserver getClientConnectionObserver() {
        return clientConnectionObserver;
    }

    public void setClientConnectionObserver(ClientConnectionObserver clientConnectionObserver) {
        this.clientConnectionObserver = clientConnectionObserver;
    }

}
