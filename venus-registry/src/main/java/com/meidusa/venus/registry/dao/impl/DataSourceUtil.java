package com.meidusa.venus.registry.dao.impl;

import org.apache.commons.dbcp.BasicDataSource;

public class DataSourceUtil {

	private static BasicDataSource ds;

	public final static BasicDataSource getBasicDataSource(String url) {
		if (null == ds) {
			synchronized (DataSourceUtil.class) {
				if (ds == null) {
					ds = new BasicDataSource();
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
					ds.setUrl(substring);
					ds.setTestOnBorrow(true);
					ds.setTestOnReturn(true);
					ds.setTestWhileIdle(true);
					ds.setValidationQuery("select 1");
					ds.setValidationQueryTimeout(5);
				}
			}
		}
		return ds;
	}

}
