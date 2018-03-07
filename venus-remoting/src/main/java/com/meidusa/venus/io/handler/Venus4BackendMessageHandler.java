/*
 * Copyright 2008-2108 amoeba.meidusa.com 
 * 
 * 	This program is free software; you can redistribute it and/or modify it under the terms of 
 * the GNU AFFERO GENERAL PUBLIC LICENSE as published by the Free Software Foundation; either version 3 of the License, 
 * or (at your option) any later version. 
 * 
 * 	This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU AFFERO GENERAL PUBLIC LICENSE for more details. 
 * 	You should have received a copy of the GNU AFFERO GENERAL PUBLIC LICENSE along with this program; 
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.meidusa.venus.io.handler;

import com.meidusa.toolkit.net.MessageHandler;
import com.meidusa.toolkit.util.TimeUtil;
import com.meidusa.venus.io.network.Venus4BackendConnection;
import com.meidusa.venus.io.network.VenusBackendConnection;
import com.meidusa.venus.io.packet.AbstractServicePacket;
import com.meidusa.venus.io.packet.PacketConstant;
import com.meidusa.venus.io.packet.PongPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * venuw客户端消息处理类
 */
public class Venus4BackendMessageHandler implements MessageHandler<Venus4BackendConnection, byte[]> {

    private static Logger logger = LoggerFactory.getLogger(Venus4BackendMessageHandler.class);

    public void handle(Venus4BackendConnection conn, byte[] message) {
        int type = AbstractServicePacket.getType(message);
        switch (type) {
            case PacketConstant.PACKET_TYPE_PING:
                PongPacket pong = new PongPacket();
                conn.write(pong.toByteBuffer());
                break;
            case PacketConstant.PACKET_TYPE_PONG:
                conn.setLastPong(TimeUtil.currentTimeMillis());
                break;
            default:
        }
    }

}
