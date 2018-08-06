package com.meidusa.venus.client.invoker.venus.encode;

import com.meidusa.venus.io.packet.AbstractServiceRequestPacket;
import com.meidusa.venus.io.packet.ServiceNofityPacket;
import com.meidusa.venus.io.packet.ServiceResponsePacket;
import com.meidusa.venus.io.serializer.Serializer;

import java.lang.reflect.Type;

public interface BaseEncoder {

    /**
     * encode
     * @param object
     * @param serializer
     * @return
     */
    AbstractServiceRequestPacket encode(Object object,Serializer serializer);

    /**
     * decode
     * @param message
     * @param retType
     * @param serializer
     * @return
     */
    ServiceResponsePacket decode(byte[] message, Type retType,Serializer serializer);

    /**
     * decodeForNotify
     * @param message
     * @param retType
     * @param serializer
     * @return
     */
    ServiceNofityPacket decodeForNotify(byte[] message, Type retType, Serializer serializer);


}
