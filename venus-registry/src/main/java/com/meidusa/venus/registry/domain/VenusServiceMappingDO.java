package com.meidusa.venus.registry.domain;

import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Date;

public class VenusServiceMappingDO implements Serializable {

	private static final long serialVersionUID = -5989031405262772357L;

	private Integer id;

	private VenusServerDO server;

	private VenusServiceDO service;

	private Integer serverId;

	private Integer serviceId;

	private String version;

	/** 是否激活 1为激活服务 */
	private boolean active;

	private boolean sync;

	private Date createTime;

	private Date updateTime;

	/** 角色：provider||consumer */
	private String role;

	/** 注册类型：0 手动：1 自动 */
	private int registeType;

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

	public int getRegisteType() {
		return registeType;
	}

	public void setRegisteType(int registeType) {
		this.registeType = registeType;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}
