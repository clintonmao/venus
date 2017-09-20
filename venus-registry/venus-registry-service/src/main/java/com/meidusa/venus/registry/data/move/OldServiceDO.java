package com.meidusa.venus.registry.data.move;

import java.io.Serializable;
import java.util.Date;

public class OldServiceDO implements Serializable {

	private static final long serialVersionUID = -4587687443119567361L;

	private int id;

	/** 服务名 */
	private String serviceName;

	/** 服务描述 */
	private String description;

	private Date createTime;

	private Date updateTime;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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

	@Override
	public String toString() {
		return "OldServiceDO [id=" + id + ", serviceName=" + serviceName + ", description=" + description
				+ ", createTime=" + createTime + ", updateTime=" + updateTime + "]";
	}

}
