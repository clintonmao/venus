package com.meidusa.venus.registry.dao.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.dbcp.BasicDataSource;

public class DataSourceUtil {

	private static ConcurrentMap<String, BasicDataSource> dsMap = new ConcurrentHashMap<String, BasicDataSource>();

	public final static BasicDataSource getBasicDataSource(String url) {
		if (null == dsMap.get(url)) {
			BasicDataSource ds = new BasicDataSource();
			ds.setDriverClassName("com.mysql.jdbc.Driver");
			String substring = url.substring(0, url.indexOf("?"));
			String nameAndPwd = url.substring(url.indexOf("?") + 1);
			String[] split = nameAndPwd.split("&");
			for (int i = 0; i < split.length; i++) {
				String str = split[i];
				String[] map = str.split("=");
				if (map[0].equals("username")) {
					ds.setUsername(map[1]);
				}
				if (map[0].equals("password")) {
					ds.setPassword(map[1]);
				}
			}
			ds.setUrl("jdbc:" + substring);
			ds.setTestOnBorrow(true);
			ds.setTestOnReturn(true);
			ds.setTestWhileIdle(true);
			ds.setValidationQuery("select 1");
			ds.setValidationQueryTimeout(5);
			dsMap.put(url, ds);
		}
		return dsMap.get(url);
	}

}
