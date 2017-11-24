package com.meidusa.venus.registry.domain;

import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Date;

public class VenusServiceDO implements Serializable {

	private static final long serialVersionUID = 741259068222583617L;

	private Integer id;

	private String name;

	private String interfaceName;

	private Integer appId;

	private String version;

	private String description;

	private Date createTime;

	private Date updateTime;

	/** 注册类型：0 手动：1 自动 */
	private Integer registeType;

	private String methods;

	private boolean isDelete;
	
	private String versionRange;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public String getInterfaceName() {
		return interfaceName;
	}

	public void setInterfaceName(String interfaceName) {
		this.interfaceName = interfaceName;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Integer getAppId() {
		return appId;
	}

	public void setAppId(Integer appId) {
		this.appId = appId;
	}

	public Integer getRegisteType() {
		return registeType;
	}

	public void setRegisteType(Integer registeType) {
		this.registeType = registeType;
	}

	public String getMethods() {
		return methods;
	}

	public void setMethods(String methods) {
		this.methods = methods;
	}

	public boolean getIsDelete() {
		return isDelete;
	}

	public void setDelete(boolean isDelete) {
		this.isDelete = isDelete;
	}

	public String getVersionRange() {
		return versionRange;
	}

	public void setVersionRange(String versionRange) {
		this.versionRange = versionRange;
	}
	
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((appId == null) ? 0 : appId.hashCode());
		result = prime * result + ((createTime == null) ? 0 : createTime.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((interfaceName == null) ? 0 : interfaceName.hashCode());
		result = prime * result + (isDelete ? 1231 : 1237);
		result = prime * result + ((methods == null) ? 0 : methods.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((registeType == null) ? 0 : registeType.hashCode());
//		result = prime * result + ((updateTime == null) ? 0 : updateTime.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		result = prime * result + ((versionRange == null) ? 0 : versionRange.hashCode());
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
		VenusServiceDO other = (VenusServiceDO) obj;
		if (appId == null) {
			if (other.appId != null)
				return false;
		} else if (!appId.equals(other.appId))
			return false;
		if (createTime == null) {
			if (other.createTime != null)
				return false;
		} else if (!createTime.equals(other.createTime))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (interfaceName == null) {
			if (other.interfaceName != null)
				return false;
		} else if (!interfaceName.equals(other.interfaceName))
			return false;
		if (isDelete != other.isDelete)
			return false;
		if (methods == null) {
			if (other.methods != null)
				return false;
		} else if (!methods.equals(other.methods))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (registeType == null) {
			if (other.registeType != null)
				return false;
		} else if (!registeType.equals(other.registeType))
			return false;
//		if (updateTime == null) {
//			if (other.updateTime != null)
//				return false;
//		} else if (!updateTime.equals(other.updateTime))
//			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		if (versionRange == null) {
			if (other.versionRange != null)
				return false;
		} else if (!versionRange.equals(other.versionRange))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}
