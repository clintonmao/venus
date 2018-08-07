package com.meidusa.venus.client.invoker.venus.encode;

import com.meidusa.venus.client.ClientInvocation;
import com.meidusa.venus.exception.InvalidParameterException;
import com.meidusa.venus.io.packet.AbstractServiceRequestPacket;
import com.meidusa.venus.io.packet.ServiceNofityPacket;
import com.meidusa.venus.io.packet.ServicePacketBuffer;
import com.meidusa.venus.io.packet.ServiceResponsePacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceNofityPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceRequestPacket;
import com.meidusa.venus.io.packet.serialize.SerializeServiceResponsePacket;
import com.meidusa.venus.io.serializer.Serializer;
import com.meidusa.venus.metainfo.EndpointParameter;
import com.meidusa.venus.notify.InvocationListener;
import com.meidusa.venus.notify.ReferenceInvocationListener;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;

public class VenusEncoder implements BaseEncoder {

    @Override
    public AbstractServiceRequestPacket encode(Object object,Serializer serializer) {
        ClientInvocation invocation = (ClientInvocation)object;
        Method method = invocation.getMethod();
        //ServiceWrapper service = invocation.getService();
        //EndpointWrapper endpoint = invocation.getEndpoint();
        EndpointParameter[] endpointParameters = invocation.getEndpointParameters();
        Object[] args = invocation.getArgs();

        //构造请求报文
        SerializeServiceRequestPacket serviceRequestPacket = new SerializeServiceRequestPacket(serializer, null);
        serviceRequestPacket.clientId = invocation.getClientId();
        serviceRequestPacket.clientRequestId = invocation.getClientRequestId();
        //设置traceId
        serviceRequestPacket.traceId = invocation.getTraceID();
        //设置athenaId
        if (invocation.getAthenaId() != null) {
            serviceRequestPacket.rootId = invocation.getAthenaId();
        }
        if (invocation.getParentId() != null) {
            serviceRequestPacket.parentId = invocation.getParentId();
        }
        if (invocation.getMessageId() != null) {
            serviceRequestPacket.messageId = invocation.getMessageId();
        }
        serviceRequestPacket.apiName = invocation.getApiName();
        serviceRequestPacket.serviceVersion = Integer.parseInt(invocation.getVersion());
        serviceRequestPacket.parameterMap = new HashMap<String, Object>();
        if (endpointParameters != null) {
            for (int i = 0; i < endpointParameters.length; i++) {
                if (args[i] instanceof InvocationListener) {
                    ReferenceInvocationListener listener = new ReferenceInvocationListener();
                    ServicePacketBuffer buffer = new ServicePacketBuffer(16);
                    buffer.writeLengthCodedString(args[i].getClass().getName(), "utf-8");
                    buffer.writeInt(System.identityHashCode(args[i]));
                    listener.setIdentityData(buffer.toByteBuffer().array());
                    Type type = method.getGenericParameterTypes()[i];
                    if (type instanceof ParameterizedType) {
                        ParameterizedType genericType = ((ParameterizedType) type);
                        //container.putInvocationListener((InvocationListener) args[i], genericType.getActualTypeArguments()[0]);
                        invocation.setInvocationListener((InvocationListener)args[i]);
                        invocation.setParamType(genericType.getActualTypeArguments()[0]);
                    } else {
                        throw new InvalidParameterException("invocationListener is not generic");
                    }
                    serviceRequestPacket.parameterMap.put(endpointParameters[i].getParamName(), listener);
                } else {
                    serviceRequestPacket.parameterMap.put(endpointParameters[i].getParamName(), args[i]);
                }

            }
        }
        return serviceRequestPacket;
    }

    @Override
    public ServiceResponsePacket decode(byte[] message, Type retType,Serializer serializer) {
        ServiceResponsePacket responsePacket = new SerializeServiceResponsePacket(serializer, retType);
        responsePacket.init(message);
        return responsePacket;
    }

    @Override
    public ServiceNofityPacket decodeForNotify(byte[] message, Type retType, Serializer serializer) {
        SerializeServiceNofityPacket nofityPacket = new SerializeServiceNofityPacket(serializer, retType);
        nofityPacket.init(message);
        return nofityPacket;
    }
}
