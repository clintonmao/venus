package com.meidusa.venus.registry.domain;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.meidusa.fastjson.annotation.JSONField;

import java.io.Serializable;
import java.util.Date;

public class VenusServiceMappingDO implements Serializable {

	private static final long serialVersionUID = -5989031405262772357L;

	private Integer id;

	private VenusServerDO server;

	private VenusServiceDO service;

	/** 根着角色走，如果角色是订阅方，就是订阅方的serverID,如果是注册方，就是注册方的serverID */
	private Integer serverId;

	private Integer serviceId;

	private String version;

	/** 是否激活 1为激活服务 */
	private boolean active;

	private boolean sync;

	@JSONField(format="yyyy-MM-dd HH:mm:ss")
	private Date createTime;

	@JSONField(format="yyyy-MM-dd HH:mm:ss")
	private Date updateTime;

	/** 注册时间 */
	@JSONField(format="yyyy-MM-dd HH:mm:ss")
	private Date registeTime;

	/** 心跳更新时间 */
	@JSONField(format="yyyy-MM-dd HH:mm:ss")
	private Date heartbeatTime;

	/** 是否删除 */
	private Boolean isDelete;

	/** 角色：provider||consumer */
	private String role;

	private Integer consumerAppId;
	
	private Integer providerAppId;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public VenusServerDO getServer() {
		return server;
	}

	public void setServer(VenusServerDO server) {
		this.server = server;
	}

	public VenusServiceDO getService() {
		return service;
	}

	public void setService(VenusServiceDO service) {
		this.service = service;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isSync() {
		return sync;
	}

	public void setSync(boolean sync) {
		this.sync = sync;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public Integer getServerId() {
		return serverId;
	}

	public void setServerId(Integer serverId) {
		this.serverId = serverId;
	}

	public Integer getServiceId() {
		return serviceId;
	}

	public void setServiceId(Integer serviceId) {
		this.serviceId = serviceId;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public Date getRegisteTime() {
		return registeTime;
	}

	public void setRegisteTime(Date registeTime) {
		this.registeTime = registeTime;
	}

	public Date getHeartbeatTime() {
		return heartbeatTime;
	}

	public void setHeartbeatTime(Date heartbeatTime) {
		this.heartbeatTime = heartbeatTime;
	}

	public Boolean getIsDelete() {
		return isDelete;
	}

	public void setIsDelete(Boolean isDelete) {
		this.isDelete = isDelete;
	}

	public Integer getConsumerAppId() {
		return consumerAppId;
	}

	public void setConsumerAppId(Integer consumerAppId) {
		this.consumerAppId = consumerAppId;
	}

	public Integer getProviderAppId() {
		return providerAppId;
	}

	public void setProviderAppId(Integer providerAppId) {
		this.providerAppId = providerAppId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (active ? 1231 : 1237);
		result = prime * result + ((consumerAppId == null) ? 0 : consumerAppId.hashCode());
		result = prime * result + ((createTime == null) ? 0 : createTime.hashCode());
		result = prime * result + ((heartbeatTime == null) ? 0 : heartbeatTime.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((isDelete == null) ? 0 : isDelete.hashCode());
		result = prime * result + ((providerAppId == null) ? 0 : providerAppId.hashCode());
		result = prime * result + ((registeTime == null) ? 0 : registeTime.hashCode());
		result = prime * result + ((role == null) ? 0 : role.hashCode());
		result = prime * result + ((server == null) ? 0 : server.hashCode());
		result = prime * result + ((serverId == null) ? 0 : serverId.hashCode());
		result = prime * result + ((service == null) ? 0 : service.hashCode());
		result = prime * result + ((serviceId == null) ? 0 : serviceId.hashCode());
		result = prime * result + (sync ? 1231 : 1237);
		result = prime * result + ((updateTime == null) ? 0 : updateTime.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VenusServiceMappingDO other = (VenusServiceMappingDO) obj;
		if (active != other.active)
			return false;
		if (consumerAppId == null) {
			if (other.consumerAppId != null)
				return false;
		} else if (!consumerAppId.equals(other.consumerAppId))
			return false;
		if (createTime == null) {
			if (other.createTime != null)
				return false;
		} else if (!createTime.equals(other.createTime))
			return false;
		if (heartbeatTime == null) {
			if (other.heartbeatTime != null)
				return false;
		} else if (!heartbeatTime.equals(other.heartbeatTime))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (isDelete == null) {
			if (other.isDelete != null)
				return false;
		} else if (!isDelete.equals(other.isDelete))
			return false;
		if (providerAppId == null) {
			if (other.providerAppId != null)
				return false;
		} else if (!providerAppId.equals(other.providerAppId))
			return false;
		if (registeTime == null) {
			if (other.registeTime != null)
				return false;
		} else if (!registeTime.equals(other.registeTime))
			return false;
		if (role == null) {
			if (other.role != null)
				return false;
		} else if (!role.equals(other.role))
			return false;
		if (server == null) {
			if (other.server != null)
				return false;
		} else if (!server.equals(other.server))
			return false;
		if (serverId == null) {
			if (other.serverId != null)
				return false;
		} else if (!serverId.equals(other.serverId))
			return false;
		if (service == null) {
			if (other.service != null)
				return false;
		} else if (!service.equals(other.service))
			return false;
		if (serviceId == null) {
			if (other.serviceId != null)
				return false;
		} else if (!serviceId.equals(other.serviceId))
			return false;
		if (sync != other.sync)
			return false;
		if (updateTime == null) {
			if (other.updateTime != null)
				return false;
		} else if (!updateTime.equals(other.updateTime))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}
