package com.meidusa.venus.registry.mysql;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.meidusa.toolkit.common.runtime.GlobalScheduler;
import com.meidusa.venus.URL;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.VenusRegisteException;
import com.meidusa.venus.registry.dao.VenusApplicationDAO;
import com.meidusa.venus.registry.dao.VenusServerDAO;
import com.meidusa.venus.registry.dao.VenusServiceConfigDAO;
import com.meidusa.venus.registry.dao.VenusServiceDAO;
import com.meidusa.venus.registry.dao.VenusServiceMappingDAO;
import com.meidusa.venus.registry.domain.VenusApplicationDO;
import com.meidusa.venus.registry.domain.VenusServerDO;
import com.meidusa.venus.registry.domain.VenusServiceConfigDO;
import com.meidusa.venus.registry.domain.VenusServiceDO;
import com.meidusa.venus.registry.domain.VenusServiceMappingDO;
import com.meidusa.venus.service.registry.ServiceDefinition;

/**
 * mysql服务注册中心类 Created by Zhangzhihua on 2017/7/27.
 */
@Component
public class MysqlRegister implements Register {

	public static final String CONSUMER = "consumer";

	public static final String PROVIDER = "provider";

	/** 已注册成功的URL */
	private Set<URL> registeUrls = new HashSet<URL>();

	/** 已订阅成功的URL */
	private Set<URL> subscribleUrls = new HashSet<URL>();

	/** 注册失败的URLS */
	private Set<URL> registeFailUrls = new HashSet<URL>();

	/** 订阅失败的URLS */
	private Set<URL> subscribleFailUrls = new HashSet<URL>();

	private Set<ServiceDefinition> serviceDefinitions = new HashSet<ServiceDefinition>();

	@Autowired
	private VenusServiceDAO venusServiceDAO;

	@Autowired
	private VenusServiceConfigDAO venusServiceConfigDAO;

	@Autowired
	private VenusApplicationDAO venusApplicationDAO;

	@Autowired
	private VenusServerDAO venusServerDAO;

	@Autowired
	private VenusServiceMappingDAO venusServiceMappingDAO;

	@Override
	public void registe(URL url) throws VenusRegisteException {
		try {
			String appCode = url.getApplication();
			int appId = 0;
			if (StringUtils.isNotBlank(appCode)) {
				VenusApplicationDO application = venusApplicationDAO.getApplication(appCode);
				if (null == application) {
					VenusApplicationDO venusApplicationDO = new VenusApplicationDO();
					venusApplicationDO.setAppCode(appCode);
					venusApplicationDO.setCreateName(PROVIDER);
					venusApplicationDO.setUpdateName(PROVIDER);
					venusApplicationDO.setProvider(true);
					venusApplicationDO.setConsumer(false);
					appId = venusApplicationDAO.addApplication(venusApplicationDO);
				} else {
					appId = application.getId();
				}
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
			VenusServiceDO service = venusServiceDAO.getService(url.getServiceName(), url.getVersion(),
					url.getInterfaceName());
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
			}

			VenusServiceMappingDO serviceMapping = venusServiceMappingDAO.getServiceMapping(serverId, serviceId,
					PROVIDER);
			if (null == serviceMapping) {
				VenusServiceMappingDO venusServiceMappingDO = new VenusServiceMappingDO();
				venusServiceMappingDO.setServerId(serverId);
				venusServiceMappingDO.setServiceId(serviceId);
				venusServiceMappingDO.setSync(true);
				venusServiceMappingDO.setActive(true);
				venusServiceMappingDO.setRegisteType(1);
				venusServiceMappingDO.setRole(PROVIDER);
				venusServiceMappingDO.setVersion(url.getVersion());
				venusServiceMappingDAO.addServiceMapping(venusServiceMappingDO);
			} else {
				String oldVersion = serviceMapping.getVersion();// 有区间的version需特殊处理
			}
		} catch (Exception e) {
			registeFailUrls.add(url);
			throw new VenusRegisteException("服务注册异常" + url.getServiceName(), e);
		}
		registeUrls.add(url);

	}

	@Override
	public void unregiste(URL url) throws VenusRegisteException {

	}

	@Override
	public void subscrible(URL url) throws VenusRegisteException {
		try {
			VenusServiceDO service = venusServiceDAO.getService(url.getServiceName(), url.getVersion(),
					url.getInterfaceName());
			if (null == service && url.isConsumerCheck()) {// 不存在，如果配置了检测抛异常
				throw new VenusRegisteException("服务订阅异常,原因:服务" + url.getServiceName() + "不存在");
			}
			String appCode = url.getApplication();
			if (StringUtils.isNotBlank(appCode)) {
				VenusApplicationDO application = venusApplicationDAO.getApplication(appCode);
				VenusApplicationDO venusApplicationDO = new VenusApplicationDO();
				venusApplicationDO.setAppCode(appCode);
				venusApplicationDO.setProvider(false);
				venusApplicationDO.setConsumer(true);
				venusApplicationDO.setUpdateName(CONSUMER);
				if (null == application) {
					venusApplicationDO.setCreateName(CONSUMER);
					venusApplicationDAO.addApplication(venusApplicationDO);
				}
			}
			VenusServerDO server = venusServerDAO.getServer(url.getHost(), 0);
			if (null == server) {
				VenusServerDO venusServerDO = new VenusServerDO();
				venusServerDO.setHostname(url.getHost());
				venusServerDO.setPort(0);
				venusServerDAO.addServer(venusServerDO);
			}
		} catch (Exception e) {
			subscribleFailUrls.add(url);
			throw new VenusRegisteException("服务订阅异常" + url.getServiceName(), e);
		}
		subscribleUrls.add(url);

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
		// 接口名 服务名 版本号 加载服务的server信息及serviceConfig信息
		// 根据本地 ServiceDefinition 列表去查找
		String serviceName = url.getServiceName();
		String interfaceName = url.getInterfaceName();
		String version = url.getVersion();
		for (Iterator<ServiceDefinition> iterator = serviceDefinitions.iterator(); iterator.hasNext();) {
			ServiceDefinition define = (ServiceDefinition) iterator.next();
			if (define.getName().equals(serviceName)) {
				if (version.equals(define.getVersionRange())) {// TODO version
					return define;
				}
			}
		}
		return null;
	}

	@Override
	public void load() throws VenusRegisteException {
		//TODO 开启定时器，定时根据 subscribleUrls 去db拉取信息，生成本地 ServiceDefinition 列表
		GlobalScheduler.getInstance().scheduleAtFixedRate(new ServiceDefineRunnable(), 10, 60 * 2, TimeUnit.SECONDS);
	}

	@Override
	public void destroy() throws VenusRegisteException {

	}

	private class ServiceDefineRunnable implements Runnable {

		public ServiceDefineRunnable() {
			// 可在构造函数中传入spring bean参数
		}

		public void run() {// 内部类的方式使用spring bean
			for (URL url : subscribleUrls) {
				String interfaceName = url.getInterfaceName();
				String serviceName = url.getServiceName();
				String version = url.getVersion();
				VenusServiceDO service = venusServiceDAO.getService(serviceName, version, interfaceName);
				Integer serviceId = service.getId();
				List<VenusServiceMappingDO> serviceMappings = venusServiceMappingDAO.getServiceMapping(serviceId,
						PROVIDER);
				List<Integer> serverIds = new ArrayList<Integer>();
				if (CollectionUtils.isNotEmpty(serviceMappings)) {
					for (VenusServiceMappingDO venusServiceMappingDO : serviceMappings) {
						if (venusServiceMappingDO.isActive()) {// 只取active的
							Integer serverId = venusServiceMappingDO.getServerId();
							serverIds.add(serverId);
						}
					}
				}

				Set<String> hostPortSet = new HashSet<String>();
				if (CollectionUtils.isNotEmpty(serverIds)) {
					List<VenusServerDO> servers = venusServerDAO.getServers(serverIds);
					for (Iterator<VenusServerDO> iterator = servers.iterator(); iterator.hasNext();) {
						VenusServerDO venusServerDO = (VenusServerDO) iterator.next();
						String hostPort = venusServerDO.getHostname() + ":" + venusServerDO.getPort();
						hostPortSet.add(hostPort);
					}
				}
				if (CollectionUtils.isNotEmpty(hostPortSet)) {
					ServiceDefinition def = new ServiceDefinition();
					def.setName(serviceName);
					def.setIpAddress(hostPortSet);
					def.setActive(true);
					def.setDescription(service.getDescription());
					def.setVersionRange(version);
					VenusServiceConfigDO serviceConfig = venusServiceConfigDAO.getServiceConfig(serviceId);
					def.setServiceConfig(serviceConfig);
					if (serviceDefinitions.size() < 1000) {
						serviceDefinitions.add(def);
					}
				}
			}

		}
	}
}
