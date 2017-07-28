package com.meidusa.venus.registry.mysql;

import java.util.ArrayList;
import java.util.List;

import com.meidusa.venus.URL;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.VenusRegisteException;
import com.meidusa.venus.service.registry.ServiceDefinition;

/**
 * mysql服务注册中心类 Created by Zhangzhihua on 2017/7/27.
 */
public class MysqlRegister implements Register {

	/** 已注册的URL */
	private List<URL> registeUrls = new ArrayList<URL>();

	/** 已订阅的URL */
	private List<URL> subscribleUrls = new ArrayList<URL>();

	@Override
	public void registe(URL url) throws VenusRegisteException {

		
	}

	@Override
	public void unregiste(URL url) throws VenusRegisteException {

	}

	@Override
	public void subscrible(URL url) throws VenusRegisteException {

	}

	@Override
	public void unsubscrible(URL url) throws VenusRegisteException {

	}

	@Override
	public void heartbeat() throws VenusRegisteException {

	}

	@Override
	public void clearInvalid() throws VenusRegisteException {

	}

	@Override
	public ServiceDefinition lookup(URL url) throws VenusRegisteException {
		return null;
	}

	@Override
	public void load() throws VenusRegisteException {

	}

	@Override
	public void destroy() throws VenusRegisteException {

	}
}
