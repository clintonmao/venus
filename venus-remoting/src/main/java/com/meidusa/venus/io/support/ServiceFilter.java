package com.meidusa.venus.io.support;

import com.meidusa.venus.io.packet.AbstractServicePacket;
import com.meidusa.venus.io.packet.AbstractServiceRequestPacket;

public interface ServiceFilter {
	
		/**
		 * 
		 * @param request <code>SerializeServiceRequestPacket</code>
		 */
		void before(AbstractServicePacket request);
		
		/**
		 * 
		 * @param request <code>ServiceResponsePacket</code> or <code>ErrorPacket</code> or <code>OKPacket</code>
		 */
		void after(AbstractServicePacket request);
}
