package com.meidusa.venus.registry.util;

import org.apache.commons.lang.StringUtils;

import com.meidusa.venus.URL;
import com.meidusa.venus.registry.domain.VenusServiceDO;
import com.meidusa.venus.registry.domain.VenusServiceDefinitionDO;

public class RegistryUtil {

	public static String getKeyFromUrl(URL url) {
		StringBuilder buf = new StringBuilder();
		buf.append("/");
		buf.append(url.getInterfaceName());
		buf.append("/");
		buf.append(url.getServiceName());
		if (StringUtils.isNotEmpty(url.getVersion())) {
			buf.append("?version=").append(url.getVersion());
		}
		return buf.toString();
	}

	public static String getKey(VenusServiceDefinitionDO url) {
		StringBuilder buf = new StringBuilder();
		buf.append("/");
		buf.append(url.getInterfaceName());
		buf.append("/");
		buf.append(url.getName());
		if (StringUtils.isNotEmpty(url.getVersion())) {
			buf.append("?version=").append(url.getVersion());
		}
		return buf.toString();
	}

	public static String getCacheKey(String nameCode, String version) {
		StringBuilder buf = new StringBuilder();
		buf.append("/");
		buf.append(nameCode);
		if (StringUtils.isNotEmpty(version)) {
			buf.append("?version=").append(version);
		}
		return buf.toString();
	}

	public static String getCacheKey(VenusServiceDO vs) {
		StringBuilder buf = new StringBuilder();
		buf.append("/");
		buf.append(vs.getInterfaceName());
		buf.append("/");
		buf.append(vs.getName());
		if (StringUtils.isNotEmpty(vs.getVersion())) {
			buf.append("?version=").append(vs.getVersion());
		}
		return buf.toString();
	}

	public static String getCacheKey(String interfaceName, String name, String version) {
		StringBuilder buf = new StringBuilder();
		buf.append("/");
		buf.append(interfaceName);
		buf.append("/");
		buf.append(name);
		if (StringUtils.isNotEmpty(version)) {
			buf.append("?version=").append(version);
		}
		return buf.toString();
	}

	public static boolean isNotBlank(String param) {
		return StringUtils.isNotBlank(param) && !"null".equals(param);
	}

}
