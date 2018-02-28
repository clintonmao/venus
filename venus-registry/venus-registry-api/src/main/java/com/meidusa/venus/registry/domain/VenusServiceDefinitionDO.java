package com.meidusa.venus.registry.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.meidusa.venus.ServiceDefinitionExtra;
import org.apache.commons.lang.StringUtils;

import com.meidusa.toolkit.common.util.ObjectUtil;

/**
 * 服务定义
 * 
 * @author structchen
 * 
 */
public class VenusServiceDefinitionDO implements ServiceDefinitionExtra,java.io.Serializable{

	private static final long serialVersionUID = -4527715273482256858L;

	/**
	 * 服务名称
	 */
	private String name;
	
	/**
	 * 接口名称
	 */
	private String interfaceName;

	/**
	 * 版本号
	 */
	private String version;

	/**
	 * 服务版本范围
	 */
	private String versionRange;

	/**
	 * 服务提供方
	 */
	private String provider;

	/**
	 * 是否激活状态
	 */
	private boolean active;

	/**
	 * 服务IP地址列表,默认格式: host:port
	 */
	private Set<String> ipAddress;

	/** 服务配置信息 */
	private List<VenusServiceConfigDO> serviceConfigs = new ArrayList<VenusServiceConfigDO>();

	private String description;

	public String getName() {
		return name;
	}

	public Set<String> getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(Set<String> ipAddress) {
		this.ipAddress = ipAddress;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersionRange() {
		return versionRange;
	}

	public void setVersionRange(String versionRange) {
		this.versionRange = versionRange;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<VenusServiceConfigDO> getServiceConfigs() {
		return serviceConfigs;
	}

	public void setServiceConfigs(List<VenusServiceConfigDO> serviceConfigs) {
		this.serviceConfigs = serviceConfigs;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public int hashCode() {
		int result = 114 + name.hashCode() + (versionRange == null ? 0 : versionRange.hashCode()) + (active ? 1 : 2);
		if (getIpAddress() == null) {
			return result;
		} else {
			for (String ip : getIpAddress()) {
				result += ip.hashCode();
			}
			return result;
		}
	}

	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof VenusServiceDefinitionDO) {
			VenusServiceDefinitionDO target = (VenusServiceDefinitionDO) obj;
			boolean result = StringUtils.equals(name, target.getName());
			result = result && StringUtils.equals(versionRange, target.getVersionRange());
			result = result && this.getIpAddress() != null && target.getIpAddress().equals(this.getIpAddress());
			result = result && ObjectUtil.equals(active, target.isActive());
			return result;
		}

		return false;
	}

	public String getInterfaceName() {
		return interfaceName;
	}

	public void setInterfaceName(String interfaceName) {
		this.interfaceName = interfaceName;
	}

	public String getPath(){
		String path = new StringBuilder()
			.append(interfaceName)
			.append("/")
			.append(name)
			.append("?version=")
			.append(version).toString();
		return path;
	}

	@Override
	public String toString() {
		return "VenusServiceDefinitionDO [name=" + name + ", interfaceName=" + interfaceName + ", versionRange=" + versionRange
				+ ", active=" + active + ", ipAddress=" + ipAddress + ", description=" + description
				+ ", serviceConfigs=" + serviceConfigs + "]";
	}


}
