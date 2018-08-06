package com.meidusa.venus.io.packet.json;

import com.meidusa.venus.io.packet.ServiceNofityPacket;
import com.meidusa.venus.io.packet.ServicePacketBuffer;

public class JsonVenusNotifyPacket extends ServiceNofityPacket {

    @Override
    protected Object readCallBackObject(ServicePacketBuffer buffer) {
        return buffer.readLengthCodedString(PACKET_CHARSET);
    }

    @Override
    protected void writeCallBackObject(ServicePacketBuffer buffer, Object callbackObject) {
        buffer.writeLengthCodedString(callbackObject.toString(), PACKET_CHARSET);
    }

}
