package com.meidusa.venus.util;

import org.apache.commons.lang.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Net util Created by Zhangzhihua on 2017/8/17.
 */
public class NetUtil {

	private static String localIp;// TODO localIp可能变化

	public static String getLocalIp(boolean isCache) {
		// return

		if (isCache) {
			return getLocalIp();
		}
		// InetAddress addr = InetAddress.getLocalHost();
		// String localIp = addr.getHostAddress();
		String localIp = NetInterfaceManager.INSTANCE.getLocalHostAddress();
		return localIp;

	}

	/**
	 * 获取本机ip,获取local ip太耗时，改为周期性获取
	 * 
	 * @return
	 */
	public static String getLocalIp() {
		if (StringUtils.isNotEmpty(localIp)) {
			return localIp;
		}
		// InetAddress addr = InetAddress.getLocalHost();
		// String newLocalIp = addr.getHostAddress();
		String newLocalIp = NetInterfaceManager.INSTANCE.getLocalHostAddress();
		localIp = newLocalIp;
		return localIp;
	}
}
