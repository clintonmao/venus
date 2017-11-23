package com.meidusa.venus.registry.data.move;

import java.io.Serializable;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;

public class ServiceMappingDTO implements Serializable {

	private static final long serialVersionUID = 6806349255635357389L;

	/** service mapping id */
	private int mapId;

	/** server id */
	private int serverId;

	/** 服务 id */
	private int serviceId;

	/** server 的ip */
	private String hostName;

	/** server 的端口 */
	private int port;

	/** 服务名称 */
	private String serviceName;

	private String versionRange;

	public String getHostName() {
		return hostName;
	}

	public int getMapId() {
		return mapId;
	}

	public int getPort() {
		return port;
	}

	public int getServerId() {
		return serverId;
	}

	public int getServiceId() {
		return serviceId;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public void setMapId(int mapId) {
		this.mapId = mapId;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setServerId(int serverId) {
		this.serverId = serverId;
	}

	public void setServiceId(int serviceId) {
		this.serviceId = serviceId;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getVersionRange() {
		return versionRange;
	}

	public void setVersionRange(String versionRange) {
		this.versionRange = versionRange;
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}

}
