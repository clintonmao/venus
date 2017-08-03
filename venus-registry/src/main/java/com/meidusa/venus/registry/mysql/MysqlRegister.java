package com.meidusa.venus.registry.mysql;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.DisposableBean;
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
import com.meidusa.venus.registry.dao.impl.NetworkInterfaceManager;
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
public class MysqlRegister implements Register, DisposableBean {

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
				if (null == application) {// 不存在添加
					VenusApplicationDO venusApplicationDO = new VenusApplicationDO();
					venusApplicationDO.setAppCode(appCode);
					venusApplicationDO.setCreateName(PROVIDER);
					venusApplicationDO.setUpdateName(PROVIDER);
					venusApplicationDO.setProvider(true);
					venusApplicationDO.setConsumer(false);
					appId = venusApplicationDAO.addApplication(venusApplicationDO);
				} else {
					appId = application.getId();
					if (null != application.isProvider() && !application.isProvider()) {// 非提供方，更新
						application.setProvider(true);
						venusApplicationDAO.updateApplication(application);
					}
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
				venusServiceMappingDO.setRegisteType(VenusServiceMappingDO.AUTO_REGISTE);
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
		try {
			VenusServerDO server = venusServerDAO.getServer(url.getHost(), url.getPort());
			if (null != server) {
				int serverId = server.getId();
				VenusServiceDO service = venusServiceDAO.getService(url.getServiceName(), url.getVersion(),
						url.getInterfaceName());
				if (null != service) {
					int serviceId = service.getId();
					VenusServiceMappingDO serviceMapping = venusServiceMappingDAO.getServiceMapping(serverId, serviceId,
							PROVIDER);
					if (null != serviceMapping) {
						venusServiceMappingDAO.deleteServiceMapping(serverId, serviceId, url.getVersion(), PROVIDER);// 逻辑删除自动注册的,手动注册的不更新
						registeUrls.remove(url);
					}
				}
			}
		} catch (Exception e) {
			throw new VenusRegisteException("取消注册异常" + url.getServiceName(), e);
		}
	}

	@Override
	public void subscrible(URL url) throws VenusRegisteException {
		try {
			VenusServiceDO service = venusServiceDAO.getService(url.getServiceName(), url.getVersion(),
					url.getInterfaceName());
			if (null == service && url.isConsumerCheck()) {// 服务不存在并且配置了检测则抛出异常
				throw new VenusRegisteException("服务订阅异常,原因:服务" + url.getServiceName() + "不存在");
			}
			String appCode = url.getApplication();
			if (StringUtils.isNotBlank(appCode)) {
				VenusApplicationDO application = venusApplicationDAO.getApplication(appCode);
				if (null == application) {
					VenusApplicationDO venusApplicationDO = new VenusApplicationDO();
					venusApplicationDO.setAppCode(appCode);
					venusApplicationDO.setProvider(false);
					venusApplicationDO.setConsumer(true);
					venusApplicationDO.setUpdateName(CONSUMER);
					venusApplicationDO.setCreateName(CONSUMER);
					venusApplicationDAO.addApplication(venusApplicationDO);
				} else {
					application.setConsumer(true);// 更新应用为订阅方
					venusApplicationDAO.updateApplication(application);
				}
			}
			VenusServerDO server = venusServerDAO.getServer(url.getHost(), 0);// 订阅server的端口为0
			if (null == server) {
				VenusServerDO venusServerDO = new VenusServerDO();
				venusServerDO.setHostname(url.getHost());
				venusServerDO.setPort(0);
				venusServerDAO.addServer(venusServerDO);
			}
			int serverId = server.getId();
			int serviceId = service.getId();
			VenusServiceMappingDO serviceMapping = venusServiceMappingDAO.getServiceMapping(serverId, serviceId,
					CONSUMER);
			if (null == serviceMapping) {
				VenusServiceMappingDO venusServiceMappingDO = new VenusServiceMappingDO();
				venusServiceMappingDO.setServerId(serverId);
				venusServiceMappingDO.setServiceId(serviceId);
				venusServiceMappingDO.setSync(true);
				venusServiceMappingDO.setActive(true);
				venusServiceMappingDO.setRegisteType(VenusServiceMappingDO.AUTO_REGISTE);
				venusServiceMappingDO.setRole(CONSUMER);
				venusServiceMappingDO.setVersion(url.getVersion());
				venusServiceMappingDAO.addServiceMapping(venusServiceMappingDO);
			}
		} catch (Exception e) {
			subscribleFailUrls.add(url);
			throw new VenusRegisteException("服务订阅异常" + url.getServiceName(), e);
		}
		subscribleUrls.add(url);

	}

	@Override
	public void unsubscrible(URL url) throws VenusRegisteException {
		try {
			VenusServiceDO service = venusServiceDAO.getService(url.getServiceName(), url.getVersion(),
					url.getInterfaceName());
			String localIp = NetworkInterfaceManager.INSTANCE.getLocalHostAddress();
			VenusServerDO server = venusServerDAO.getServer(localIp, 0);
			VenusServiceMappingDO serviceMapping = venusServiceMappingDAO.getServiceMapping(server.getId(),
					service.getId(), CONSUMER);
			if (null != serviceMapping) {
				venusServiceMappingDAO.deleteServiceMapping(server.getId(), service.getId(), url.getVersion(),
						CONSUMER);// 逻辑删除自动注册的,手动注册的不更新
				subscribleUrls.remove(url);
			}
		} catch (Exception e) {
			throw new VenusRegisteException("取消订阅异常" + url.getServiceName(), e);
		}
	}

	@Override
	public void heartbeat() throws VenusRegisteException {
		GlobalScheduler.getInstance().scheduleAtFixedRate(new HeartBeatRunnable(), 10, 10, TimeUnit.SECONDS);
	}

	@Override
	public void clearInvalid() throws VenusRegisteException {
		registeFailUrls.clear();
		subscribleFailUrls.clear();
	}

	@Override
	public ServiceDefinition lookup(URL url) throws VenusRegisteException {
		// 接口名 服务名 版本号 加载服务的server信息及serviceConfig信息
		// 根据本地 ServiceDefinition 列表去查找
		String serviceName = url.getServiceName();
		String interfaceName = url.getInterfaceName();
		String version = url.getVersion();
		for (Iterator<ServiceDefinition> iterator = serviceDefinitions.iterator(); iterator.hasNext();) {
			ServiceDefinition define = iterator.next();
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
		GlobalScheduler.getInstance().scheduleAtFixedRate(new ServiceDefineRunnable(), 10, 60 * 2, TimeUnit.SECONDS);
	}

	@Override
	public void destroy() throws VenusRegisteException {
		registeUrls.clear();
		subscribleUrls.clear();
		registeFailUrls.clear();
		subscribleFailUrls.clear();
		serviceDefinitions.clear();
	}

	private class ServiceDefineRunnable implements Runnable {

		public ServiceDefineRunnable() {

		}

		public void run() {
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
						VenusServerDO venusServerDO = iterator.next();
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

	private class HeartBeatRunnable implements Runnable {

		@Override
		public void run() {
			if (CollectionUtils.isNotEmpty(registeUrls)) {
				for (Iterator<URL> iterator = registeUrls.iterator(); iterator.hasNext();) {
					URL url = iterator.next();
					String interfaceName = url.getInterfaceName();
					String serviceName = url.getServiceName();
					String version = url.getVersion();
					try {
						VenusServiceDO service = venusServiceDAO.getService(serviceName, version, interfaceName);
						int serviceID = service.getId();
						String host = url.getHost();
						int port = url.getPort();
						VenusServerDO server = venusServerDAO.getServer(host, port);
						int serverID = server.getId();
						venusServiceMappingDAO.updateServiceMappingHeartBeatTime(serverID, serviceID, version,
								PROVIDER);
					} catch (Exception e) {
						throw new VenusRegisteException("registe更新heartBeatTime异常,服务名：" + url.getServiceName(), e);
					}
				}
			}
			if (CollectionUtils.isNotEmpty(subscribleUrls)) {
				for (Iterator<URL> iterator = subscribleUrls.iterator(); iterator.hasNext();) {
					URL url = iterator.next();
					String interfaceName = url.getInterfaceName();
					String serviceName = url.getServiceName();
					String version = url.getVersion();
					try {
						VenusServiceDO service = venusServiceDAO.getService(serviceName, version, interfaceName);
						int serviceID = service.getId();
						String host = url.getHost();
						VenusServerDO server = venusServerDAO.getServer(host, 0);
						int serverID = server.getId();
						venusServiceMappingDAO.updateServiceMappingHeartBeatTime(serverID, serviceID, version,
								CONSUMER);
					} catch (Exception e) {
						throw new VenusRegisteException("subscrible更新heartBeatTime异常,服务名：" + url.getServiceName(), e);
					}
				}
			}
		}

	}
}
