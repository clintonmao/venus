package com.meidusa.venus.registry.data.move;

import java.io.Serializable;
import java.util.Date;

public class OldServiceMappingDO implements Serializable {

	private static final long serialVersionUID = -3593262168758758368L;

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

	/** 服务描述 */
	private String description;

	/** 服务版本 */
	private String version;

	/** 服务映射状态 */
	private Boolean active;

	/** 服务映射是否同步 */
	private Boolean sync;

	/** 服务映射创建时间 */
	private Date createTime;

	/** 服务映射修改时间 */
	private Date updateTime;

	public Date getCreateTime() {
		return createTime;
	}

	public String getDescription() {
		return description;
	}

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

	public Date getUpdateTime() {
		return updateTime;
	}

	public String getVersion() {
		return version;
	}

	public Boolean isActive() {
		return active;
	}

	public Boolean isSync() {
		return sync;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public void setDescription(String description) {
		this.description = description;
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

	public void setSync(Boolean sync) {
		this.sync = sync;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	@Override
	public String toString() {
		return "OldServiceMappingDO [mapId=" + mapId + ", serverId=" + serverId + ", serviceId=" + serviceId
				+ ", hostName=" + hostName + ", port=" + port + ", serviceName=" + serviceName + ", description="
				+ description + ", version=" + version + ", active=" + active + ", sync=" + sync + ", createTime="
				+ createTime + ", updateTime=" + updateTime + "]";
	}

}
