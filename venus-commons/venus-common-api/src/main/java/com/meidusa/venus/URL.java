package com.meidusa.venus;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * 各种地址抽象URL，如
 * 服务注册地址,venus://com.chexiang.order.OrderService/orderService?version=1.0.0&host=192.168.1.1&port=9000&methods=getOrderById[java.lang.String],selectAllOrder[java.lang.String]
 * 服务订阅地址,subscrible://com.chexiang.order.OrderService/orderService?version=1.0.0&host=192.168.1.2
 * Created by Zhangzhihua on 2017/7/27.
 */
public class URL implements Serializable {

	private static final long serialVersionUID = -4259657535674215341L;

	/** 协议，如 venus || subscrible */
	private String protocol;

	/** 路径，如 com.chexiang.order.OrderService/orderService */
	private String path;

	/** 接口名,老版本此值为null，如 com.chexiang.order.OrderService */
	private String interfaceName;

	/** 服务名，如 orderService */
	private String serviceName;

	/** 版本号，如 1.0.0 */
	private String version;

	private String methods;

	/** 应用名 */
	private String application;

	/** ip，如 192.168.1.1 */
	private String host;

	/** 端口，如 16800 */
	private int port;

	//机器权重 1~10，默认为1，若不提供服务，则通过禁止服务来设置
	private int weight = 1;

	/** 负载策略 */
	private String loadbanlance;

	/** 订阅检测 */
	private boolean consumerCheck;

	/** 服务地址属性映射表，即?后属性<K,V> */
	private Map<String, Object> properties = new HashMap<String, Object>();

	/*配置文件服务地址配置*/
	private RemoteConfig remoteConfig;

	/*注册中心服务定义*/
	private ServiceDefinitionDO serviceDefinition;

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}

	/**
	 * 根据KEY获取属性值
	 * @param key
	 * @return
	 */
	public String getProperty(String key){
		if(properties.get(key) == null){
			return null;
		}
		return String.valueOf(properties.get(key));
	}

	/**
	 * 解析url
	 * 
	 * @param url
	 * @return
	 */
	public static URL parse(String url) {
		if (StringUtils.isNotBlank(url)) {
			URL u = new URL();
			if (url.startsWith("venus://")) {
				u.setProtocol("venus");
				url = url.replaceFirst("venus://", "");
			}
			if (url.startsWith("subscrible://")) {
				u.setProtocol("subscrible");
				url = url.replaceFirst("subscrible://", "");
			}
			int indexOf = url.indexOf("?");
			if (indexOf != -1) {
				String path = url.substring(0, indexOf);
				u.setPath(path);
				String properties = url.substring(indexOf + 1);
				String[] splits = properties.split("&");
				Map<String, Object> map = new HashMap<String, Object>();
				for (int i = 0; i < splits.length; i++) {
					String str = splits[i];
					String[] split = str.split("=");
					if (split.length > 1) {
						map.put(split[0], split[1]);
						if (split[0].equals("port")) {
							u.setPort(Integer.valueOf(split[1]));
						}
						if (split[0].equals("host")) {
							u.setHost(split[1]);
						}
						if (split[0].equals("version")) {
							u.setVersion(split[1]);
						}
						if (split[0].equals("loadbanlance")) {
							u.setLoadbanlance(split[1]);
						}
						if (split[0].equals("application")) {
							u.setApplication(split[1]);
						}
						if (split[0].equals("methods")) {
							u.setMethods(split[1]);
						}
					}
				}
				u.setProperties(map);

				if (path.contains(".") || path.contains("/")) {
					if (path.contains("/")) {
						String interfaceName = path.substring(0, path.indexOf("/"));
						String serviceName = path.substring(path.indexOf("/") + 1);
						u.setServiceName(serviceName);
						u.setInterfaceName(interfaceName);
					} else {
						u.setServiceName(path);
					}
				} else {
					u.setServiceName(path);
				}
			}
			return u;
		}
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((application == null) ? 0 : application.hashCode());
		result = prime * result + (consumerCheck ? 1231 : 1237);
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + ((interfaceName == null) ? 0 : interfaceName.hashCode());
		result = prime * result + ((loadbanlance == null) ? 0 : loadbanlance.hashCode());
		result = prime * result + ((methods == null) ? 0 : methods.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + port;
		result = prime * result + ((properties == null) ? 0 : properties.hashCode());
		result = prime * result + ((protocol == null) ? 0 : protocol.hashCode());
		result = prime * result + ((serviceName == null) ? 0 : serviceName.hashCode());
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
		URL other = (URL) obj;
		if (application == null) {
			if (other.application != null)
				return false;
		} else if (!application.equals(other.application))
			return false;
		if (consumerCheck != other.consumerCheck)
			return false;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		if (interfaceName == null) {
			if (other.interfaceName != null)
				return false;
		} else if (!interfaceName.equals(other.interfaceName))
			return false;
		if (loadbanlance == null) {
			if (other.loadbanlance != null)
				return false;
		} else if (!loadbanlance.equals(other.loadbanlance))
			return false;
		if (methods == null) {
			if (other.methods != null)
				return false;
		} else if (!methods.equals(other.methods))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (port != other.port)
			return false;
		if (properties == null) {
			if (other.properties != null)
				return false;
		} else if (!properties.equals(other.properties))
			return false;
		if (protocol == null) {
			if (other.protocol != null)
				return false;
		} else if (!protocol.equals(other.protocol))
			return false;
		if (serviceName == null) {
			if (other.serviceName != null)
				return false;
		} else if (!serviceName.equals(other.serviceName))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	public String getInterfaceName() {
		return interfaceName;
	}

	public void setInterfaceName(String interfaceName) {
		this.interfaceName = interfaceName;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public String getLoadbanlance() {
		return loadbanlance;
	}

	public void setLoadbanlance(String loadbanlance) {
		this.loadbanlance = loadbanlance;
	}

	public boolean isConsumerCheck() {
		return consumerCheck;
	}

	public void setConsumerCheck(boolean consumerCheck) {
		this.consumerCheck = consumerCheck;
	}

	public String getMethods() {
		return methods;
	}

	public void setMethods(String methods) {
		this.methods = methods;
	}

	public RemoteConfig getRemoteConfig() {
		return remoteConfig;
	}

	public void setRemoteConfig(RemoteConfig remoteConfig) {
		this.remoteConfig = remoteConfig;
	}

	public ServiceDefinitionDO getServiceDefinition() {
		return serviceDefinition;
	}

	public void setServiceDefinition(ServiceDefinitionDO serviceDefinition) {
		this.serviceDefinition = serviceDefinition;
	}

	@Override
	public String toString() {
		return "URL [protocol=" + protocol + ", path=" + path + ", interfaceName=" + interfaceName + ", serviceName="
				+ serviceName + ", version=" + version + ", host=" + host + ", port=" + port + ", application="
				+ application + ", loadbanlance=" + loadbanlance + ", consumerCheck=" + consumerCheck + ", methods="
				+ methods + ", properties=" + properties + "]";
	}

	public static void main(String args[]) {
		String url = "subscrible://orderService?version=1.0.0&host=192.168.1.1&port=9000&application=order-service&loadbanlance=random";
		URL u = URL.parse(url);
		System.out.println(u);
	}

}
