package com.meidusa.venus.registry.dao.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.dbcp.BasicDataSource;

public class DataSourceUtil {

	private static ConcurrentMap<String, BasicDataSource> dsMap = new ConcurrentHashMap<String, BasicDataSource>();

	public final static BasicDataSource getBasicDataSource(String url) {
		BasicDataSource basicDataSource = dsMap.get(url);
		if (null == basicDataSource) {
			synchronized (DataSourceUtil.class) {
				BasicDataSource ds = new BasicDataSource();
				ds.setDriverClassName("com.mysql.jdbc.Driver");
				String substring = url.substring(0, url.indexOf("?"));
				String keyValues = url.substring(url.indexOf("?") + 1);
				String[] split = keyValues.split("&");
				ds.setValidationQueryTimeout(5);
				for (int i = 0; i < split.length; i++) {
					String str = split[i];
					String[] map = str.split("=");
					if (map[0].equals("username")) {
						ds.setUsername(map[1]);
					}
					if (map[0].equals("password")) {
						ds.setPassword(map[1]);
					}
					if (map[0].equalsIgnoreCase("maxActive")) {
						ds.setMaxActive(Integer.valueOf(map[1].trim()));
					}
					if (map[0].equalsIgnoreCase("maxIdle")) {
						ds.setMaxIdle(Integer.valueOf(map[1].trim()));
					}
					if (map[0].equalsIgnoreCase("minIdle")) {
						ds.setMinIdle(Integer.valueOf(map[1].trim()));
					}
					if (map[0].equalsIgnoreCase("initialSize")) {
						ds.setInitialSize(Integer.valueOf(map[1].trim()));
					}
					if (map[0].equalsIgnoreCase("validationQueryTimeout")) {
						ds.setValidationQueryTimeout(Integer.parseInt(map[1].trim()));
					}
				}
				ds.setUrl(substring.startsWith("jdbc:") ? substring : ("jdbc:" + substring));
				ds.setTestOnBorrow(true);
				ds.setTestOnReturn(true);
				ds.setTestWhileIdle(true);
				ds.setValidationQuery("select 1");
				dsMap.put(url, ds);
				return ds;
			}
		} else {
			return basicDataSource;
		}
	}

}
