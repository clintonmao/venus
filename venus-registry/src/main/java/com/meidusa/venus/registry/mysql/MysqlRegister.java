package com.meidusa.venus.registry.mysql;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.meidusa.venus.URL;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.VenusRegisteException;
import com.meidusa.venus.registry.dao.VenusApplicationDAO;
import com.meidusa.venus.registry.dao.VenusServerDAO;
import com.meidusa.venus.registry.dao.VenusServiceDAO;
import com.meidusa.venus.registry.dao.VenusServiceMappingDAO;
import com.meidusa.venus.registry.domain.VenusApplicationDO;
import com.meidusa.venus.registry.domain.VenusServerDO;
import com.meidusa.venus.registry.domain.VenusServiceDO;
import com.meidusa.venus.registry.domain.VenusServiceMappingDO;
import com.meidusa.venus.service.registry.ServiceDefinition;

/**
 * mysql服务注册中心类 Created by Zhangzhihua on 2017/7/27.
 */
@Component
public class MysqlRegister implements Register {

	/** 已注册的URL */
	private List<URL> registeUrls = new ArrayList<URL>();

	/** 已订阅的URL */
	private List<URL> subscribleUrls = new ArrayList<URL>();

	@Autowired
	private VenusServiceDAO venusServiceDAO;

	@Autowired
	private VenusApplicationDAO venusApplicationDAO;

	@Autowired
	private VenusServerDAO venusServerDAO;

	@Autowired
	private VenusServiceMappingDAO venusServiceMappingDAO;

	@Override
	public void registe(URL url) throws VenusRegisteException {
		registeUrls.add(url);
		VenusApplicationDO application = venusApplicationDAO.getApplication(url.getApplication());
		int appId = 0;
		if (null == application) {
			VenusApplicationDO venusApplicationDO = new VenusApplicationDO();
			venusApplicationDO.setAppCode(url.getApplication());
			venusApplicationDO.setCreateName("register");
			venusApplicationDO.setUpdateName("register");
			appId = venusApplicationDAO.addApplication(venusApplicationDO);
		} else {
			appId = application.getId();
		}
		VenusServerDO server = venusServerDAO.getServer(url.getHost(), url.getPort());
		int serverId = 0;
		if (null == server) {
			VenusServerDO venusServerDO = new VenusServerDO();
			venusServerDO.setHostname(url.getHost());
			venusServerDO.setPort(url.getPort());
			serverId = venusServerDAO.addServer(venusServerDO);
		} else {
			serverId = server.getId();
		}
		VenusServiceDO service = venusServiceDAO.getService(url.getServiceName(), url.getInterfaceName());
		int serviceId = 0;
		if (null == service) {
			VenusServiceDO venusServiceDO = new VenusServiceDO();
			venusServiceDO.setInterfaceName(url.getInterfaceName());
			venusServiceDO.setName(url.getServiceName());
			venusServiceDO.setAppId(appId);
			venusServiceDO.setVersion(url.getVersion());
			serviceId = venusServiceDAO.addService(venusServiceDO);
		} else {
			serviceId = service.getId();
			if (!url.getVersion().equals(service.getVersion())) {
				VenusServiceDO venusServiceDO = new VenusServiceDO();
				venusServiceDO.setInterfaceName(url.getInterfaceName());
				venusServiceDO.setName(url.getServiceName());
				venusServiceDO.setAppId(appId);
				venusServiceDO.setVersion(url.getVersion());
				venusServiceDAO.updateService(venusServiceDO);
			}
		}

		VenusServiceMappingDO serviceMapping = venusServiceMappingDAO.getServiceMapping(serverId, serviceId);
		if (null == serviceMapping) {
			VenusServiceMappingDO venusServiceMappingDO = new VenusServiceMappingDO();
			venusServiceMappingDO.setServerId(serverId);
			venusServiceMappingDO.setServiceId(serviceId);
			venusServiceMappingDO.setSync(true);
			venusServiceMappingDO.setActive(true);
			venusServiceMappingDO.setVersion(url.getVersion());
			venusServiceMappingDAO.addServiceMapping(venusServiceMappingDO);
		} else {
			String oldVersion = serviceMapping.getVersion();//有区间的version需特殊处理
		}

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
