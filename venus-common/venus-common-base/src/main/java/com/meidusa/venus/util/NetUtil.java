package com.meidusa.venus.util;

import org.apache.commons.lang.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Net util Created by Zhangzhihua on 2017/8/17.
 */
public class NetUtil {

	private static String localIp;

	public static String getLocalIp(boolean isCache) {
		if (isCache) {
			return getLocalIp();
		}
		try {
			InetAddress addr = InetAddress.getLocalHost();
			String localIp = addr.getHostAddress();
			return localIp;
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
		/*
		String localIp = NetInterfaceManager.INSTANCE.getLocalHostAddress();
		*/
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
		try {
			InetAddress addr = InetAddress.getLocalHost();
			String newLocalIp = addr.getHostAddress();
			localIp = newLocalIp;
			return localIp;
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
		/*
		String newLocalIp = NetInterfaceManager.INSTANCE.getLocalHostAddress();
		*/
	}
}
