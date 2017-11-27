package com.meidusa.venus.registry.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.meidusa.fastjson.JSON;
import com.meidusa.venus.URL;
import com.meidusa.venus.registry.VenusRegisteException;
import com.meidusa.venus.registry.dao.CacheVenusServerDAO;
import com.meidusa.venus.registry.dao.CacheVenusServiceDAO;
import com.meidusa.venus.registry.dao.VenusApplicationDAO;
import com.meidusa.venus.registry.dao.VenusServerDAO;
import com.meidusa.venus.registry.dao.VenusServiceConfigDAO;
import com.meidusa.venus.registry.dao.VenusServiceDAO;
import com.meidusa.venus.registry.dao.VenusServiceMappingDAO;
import com.meidusa.venus.registry.dao.impl.ResultUtils;
import com.meidusa.venus.registry.data.move.UpdateHeartBeatTimeDTO;
import com.meidusa.venus.registry.domain.RegisteConstant;
import com.meidusa.venus.registry.domain.VenusApplicationDO;
import com.meidusa.venus.registry.domain.VenusServerDO;
import com.meidusa.venus.registry.domain.VenusServiceConfigDO;
import com.meidusa.venus.registry.domain.VenusServiceDO;
import com.meidusa.venus.registry.domain.VenusServiceDefinitionDO;
import com.meidusa.venus.registry.domain.VenusServiceMappingDO;
import com.meidusa.venus.registry.service.RegisterService;
import com.meidusa.venus.support.VenusConstants;

/**
 * Created by Zhangzhihua on 2017/8/16.
 */
public class MysqlRegisterService implements RegisterService,DisposableBean {

	private VenusServiceDAO venusServiceDAO;

	private VenusServiceConfigDAO venusServiceConfigDAO;

	private VenusApplicationDAO venusApplicationDAO;

	private VenusServerDAO venusServerDAO;

	private CacheVenusServerDAO cacheVenusServerDAO;
	
	private CacheVenusServiceDAO cacheVenusServiceDAO;

	private VenusServiceMappingDAO venusServiceMappingDAO;

	private TransactionTemplate transactionTemplate;
	
	private boolean needRun = true;
	
	private ExecutorService es = Executors.newSingleThreadExecutor();
	
	private static final int QUEUE_SIZE_2000 = 2000;
	
	private static final LinkedBlockingQueue<UpdateHeartBeatTimeDTO> HEARTBEAT_QUEUE  =new   LinkedBlockingQueue<UpdateHeartBeatTimeDTO>(QUEUE_SIZE_2000);
	
	private static final Logger logger = LoggerFactory.getLogger(MysqlRegisterService.class);
	
	/** 新注册中心数据库地址 */
	private String connectUrl;

	public MysqlRegisterService() {
	}

	/**
	 * url =
	 * "mysql://10.32.173.250:3306/registry_new?username=registry&password=registry";
	 * 
	 * @param connectUrl
	 * @return
	 */
	public MysqlRegisterService(String connectUrl) {
		this.setConnectUrl(connectUrl);
		init();
	}

	public void init() {
		// updateServiceAppIds();
		UpdateHeartbeatTimeRunnable heartbeatThread = new UpdateHeartbeatTimeRunnable("update-heartbeat-time-thread");
		es.submit(heartbeatThread);
	}

	@Override
	public void registe(URL url) throws VenusRegisteException {
		String appCode = url.getApplication();
		int appId = 0;
		if (StringUtils.isNotBlank(appCode)) {
			VenusApplicationDO application = venusApplicationDAO.getApplication(appCode);
			if (null == application) {// 不存在添加
				VenusApplicationDO venusApplicationDO = new VenusApplicationDO();
				venusApplicationDO.setAppCode(appCode);
				venusApplicationDO.setCreateName(RegisteConstant.PROVIDER);
				venusApplicationDO.setUpdateName(RegisteConstant.PROVIDER);
				venusApplicationDO.setProvider(true);
				venusApplicationDO.setConsumer(false);
				appId = venusApplicationDAO.addApplication(venusApplicationDO);
			} else {
				appId = application.getId();
				if (null == application.isProvider()
						|| (null != application.isProvider() && !application.isProvider())) {// 非提供方，更新
					application.setProvider(true);
					venusApplicationDAO.updateApplication(application);
				}
			}
		}
		int serverId = addServer(url.getHost(), url.getPort());
		VenusServiceDO service = venusServiceDAO.getService(url.getInterfaceName(), url.getServiceName(),
				url.getVersion());
		int serviceId = 0;
		if (null == service) {
			VenusServiceDO venusServiceDO = new VenusServiceDO();
			venusServiceDO.setInterfaceName(url.getInterfaceName());
			venusServiceDO.setName(url.getServiceName());
			venusServiceDO.setAppId(appId);
			venusServiceDO.setVersion(url.getVersion());
			venusServiceDO.setRegisteType(RegisteConstant.AUTO_REGISTE);
			venusServiceDO.setMethods(url.getMethods());
			venusServiceDO.setVersionRange(url.getVersionRange());
			venusServiceDO.setDelete(false);
			serviceId = venusServiceDAO.addService(venusServiceDO);
		} else {
			serviceId = service.getId();
			if (StringUtils.isBlank(url.getMethods())) {
				url.setMethods("");
			}
			venusServiceDAO.updateService(url.getMethods(), false, serviceId, appId);
		}

		VenusServiceMappingDO serviceMapping = venusServiceMappingDAO.getServiceMapping(serverId, serviceId,
				RegisteConstant.PROVIDER);
		if (null == serviceMapping) {
			VenusServiceMappingDO venusServiceMappingDO = new VenusServiceMappingDO();
			venusServiceMappingDO.setServerId(serverId);
			venusServiceMappingDO.setServiceId(serviceId);
			venusServiceMappingDO.setProviderAppId(appId);
			venusServiceMappingDO.setConsumerAppId(0);
			venusServiceMappingDO.setSync(true);
			venusServiceMappingDO.setActive(true);
			venusServiceMappingDO.setRole(RegisteConstant.PROVIDER);
			venusServiceMappingDO.setVersion(url.getVersion());
			venusServiceMappingDO.setIsDelete(false);
			venusServiceMappingDAO.addServiceMapping(venusServiceMappingDO);
		} else {
			venusServiceMappingDAO.updateProviderServiceMapping(serviceMapping.getId(), true, false, appId);
			String oldVersion = serviceMapping.getVersion();// 有区间的version需特殊处理

		}
	}

	public int addServer(String host, int port) {
		VenusServerDO server = venusServerDAO.getServer(host, port);
		int serverId = 0;
		if (null == server) {
			VenusServerDO venusServerDO = new VenusServerDO();
			venusServerDO.setHostname(host);
			venusServerDO.setPort(port);
			serverId = venusServerDAO.addServer(venusServerDO);
		} else {
			serverId = server.getId();
		}
		return serverId;
	}

	public int addService(String serviceName, String description, String versionRange) {
		// VenusServiceDO service =
		// venusServiceDAO.getService(serviceName,RegisteConstant.OPERATOR_REGISTE,versionRange);
		// int serviceId = 0;
		// if (null == service) {
		// int appId = saveApplication(serviceName);
		// VenusServiceDO venusServiceDO = new VenusServiceDO();
		// venusServiceDO.setName(serviceName);
		// venusServiceDO.setAppId(appId);
		// venusServiceDO.setVersion(String.valueOf(VenusConstants.VERSION_DEFAULT));//导入时version默认为0
		// venusServiceDO.setVersionRange(versionRange);
		// venusServiceDO.setRegisteType(RegisteConstant.OPERATOR_REGISTE);
		// venusServiceDO.setMethods(null);
		// venusServiceDO.setDescription(description);
		// venusServiceDO.setDelete(false);
		// serviceId = venusServiceDAO.addService(venusServiceDO);
		// }
		// return serviceId;
		return 0;
	}

	private int saveApplication(String serviceName) {
		String oldAppCode = serviceName + "_app";
		int appId = 0;
		if (StringUtils.isNotBlank(oldAppCode)) {
			VenusApplicationDO application = venusApplicationDAO.getApplication(oldAppCode);
			if (null == application) {// 不存在添加
				VenusApplicationDO venusApplicationDO = new VenusApplicationDO();
				venusApplicationDO.setAppCode(oldAppCode);
				venusApplicationDO.setCreateName(RegisteConstant.PROVIDER);
				venusApplicationDO.setUpdateName(RegisteConstant.PROVIDER);
				venusApplicationDO.setProvider(true);
				venusApplicationDO.setConsumer(false);
				appId = venusApplicationDAO.addApplication(venusApplicationDO);
			} else {
				appId = application.getId();
				if (null == application.isProvider()
						|| (null != application.isProvider() && !application.isProvider())) {// 非提供方，更新
					application.setProvider(true);
					venusApplicationDAO.updateApplication(application);
				}
			}
		}
		return appId;
	}

	@Override
	public boolean unregiste(URL url) throws VenusRegisteException {
		VenusServiceDO service = venusServiceDAO.getService(url.getInterfaceName(), url.getServiceName(),
				url.getVersion());
		if (null != service && null != service.getRegisteType()
				&& service.getRegisteType() == RegisteConstant.AUTO_REGISTE) {// 自动注册的逻辑删除,手动注册的不删除
			VenusServerDO server = venusServerDAO.getServer(url.getHost(), url.getPort());
			if (null != server) {
				int serverId = server.getId();
				int serviceId = service.getId();
				VenusServiceMappingDO serviceMapping = venusServiceMappingDAO.getServiceMapping(serverId, serviceId,
						RegisteConstant.PROVIDER);
				if (null != serviceMapping) {
					boolean deleteServiceMapping = venusServiceMappingDAO.deleteServiceMapping(serviceMapping.getId());
					deleteServer(serverId);
					return deleteServiceMapping;
				}
			}
		}
		return false;
	}

	@Override
	public void subscrible(URL url) throws VenusRegisteException {
		List<VenusServiceDO> services = venusServiceDAO.queryServices(url.getInterfaceName(), url.getServiceName(),
				url.getVersion());
		for (Iterator<VenusServiceDO> iterator = services.iterator(); iterator.hasNext();) {
			VenusServiceDO service = iterator.next();
			if (null == service && url.isConsumerCheck()) {// 服务不存在并且配置了检测则抛出异常
				String name = log_service_name(url);
				logger.error("服务订阅异常,原因:服务{}不存在 ", name);
				throw new VenusRegisteException("服务订阅异常,原因:服务" + name + "不存在");
			}
			if (service.getIsDelete() && url.isConsumerCheck()) {// 服务不存在并且配置了检测则抛出异常
				String name = log_service_name(url);
				logger.error("服务订阅异常,原因:服务{}已删除", name);
				throw new VenusRegisteException("服务订阅异常,原因:服务" + name + "不存在");
			}
			String appCode = url.getApplication();
			int appId = 0;
			if (StringUtils.isNotBlank(appCode)) {
				VenusApplicationDO application = venusApplicationDAO.getApplication(appCode);
				if (null == application) {
					VenusApplicationDO venusApplicationDO = new VenusApplicationDO();
					venusApplicationDO.setAppCode(appCode);
					venusApplicationDO.setProvider(false);
					venusApplicationDO.setConsumer(true);
					venusApplicationDO.setUpdateName(RegisteConstant.CONSUMER);
					venusApplicationDO.setCreateName(RegisteConstant.CONSUMER);
					appId = venusApplicationDAO.addApplication(venusApplicationDO);
				} else {
					appId = application.getId();
					if (null == application.isConsumer()
							|| (null != application.isConsumer() && !application.isConsumer())) {
						application.setConsumer(true);// 更新应用为订阅方
						venusApplicationDAO.updateApplication(application);
					}
				}
			}
			VenusServerDO server = venusServerDAO.getServer(url.getHost(), 0);// 订阅server的端口为0
			int serverId = 0;
			if (null == server) {
				VenusServerDO venusServerDO = new VenusServerDO();
				venusServerDO.setHostname(url.getHost());
				venusServerDO.setPort(0);
				serverId = venusServerDAO.addServer(venusServerDO);
			} else {
				serverId = server.getId();
			}
			int serviceId = service.getId();
			VenusServiceMappingDO serviceMapping = venusServiceMappingDAO.getServiceMapping(serverId, serviceId,
					RegisteConstant.CONSUMER);
			if (null == serviceMapping) {
				VenusServiceMappingDO venusServiceMappingDO = new VenusServiceMappingDO();
				venusServiceMappingDO.setServerId(serverId);
				venusServiceMappingDO.setServiceId(serviceId);
				venusServiceMappingDO.setSync(true);
				venusServiceMappingDO.setActive(true);
				venusServiceMappingDO.setRole(RegisteConstant.CONSUMER);
				venusServiceMappingDO.setVersion(url.getVersion());
				venusServiceMappingDO.setIsDelete(false);
				venusServiceMappingDO.setProviderAppId(0);
				venusServiceMappingDO.setConsumerAppId(appId);
				venusServiceMappingDAO.addServiceMapping(venusServiceMappingDO);
			} else {
				venusServiceMappingDAO.updateSubcribeServiceMapping(serviceMapping.getId(), appId, true, false);
			}
		}
	}

	@Override
	public boolean unsubscrible(URL url) throws VenusRegisteException {
		List<VenusServiceDO> services = venusServiceDAO.queryServices(url.getInterfaceName(), url.getServiceName(),
				url.getVersion());
		for (Iterator<VenusServiceDO> iterator = services.iterator(); iterator.hasNext();) {
			VenusServiceDO service = iterator.next();
			if (null != service) {
				VenusServerDO server = venusServerDAO.getServer(url.getHost(), 0);
				if (null != server) {
					VenusServiceMappingDO serviceMapping = venusServiceMappingDAO.getServiceMapping(server.getId(),
							service.getId(), RegisteConstant.CONSUMER);
					if (null != serviceMapping) {
						boolean deleteServiceMapping = venusServiceMappingDAO
								.deleteServiceMapping(serviceMapping.getId());
						deleteServer(server.getId());
						return deleteServiceMapping;
					}
				}
			}
		}
		return false;
	}

	public List<VenusServiceDefinitionDO> findServiceDefinitions(URL url) {
		List<VenusServiceDefinitionDO> returnList = new ArrayList<VenusServiceDefinitionDO>();
		String interfaceName = url.getInterfaceName();
		String serviceName = url.getServiceName();
		String version = url.getVersion();
		try {
			List<VenusServiceDO> services = venusServiceDAO.queryServices(interfaceName, serviceName, version);// servicePath
																												// interfaceName/serviceName?version=version
			for (Iterator<VenusServiceDO> ite = services.iterator(); ite.hasNext();) {
				List<Integer> serverIds = new ArrayList<Integer>();
				VenusServiceDO service = ite.next();
				Integer serviceId = service.getId();
				List<VenusServiceMappingDO> serviceMappings = venusServiceMappingDAO.getServiceMapping(serviceId,
						RegisteConstant.PROVIDER, false);
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
					if (CollectionUtils.isNotEmpty(servers)) {
						for (Iterator<VenusServerDO> iterator = servers.iterator(); iterator.hasNext();) {
							VenusServerDO venusServerDO = iterator.next();
							String hostPort = venusServerDO.getHostname() + ":" + venusServerDO.getPort();
							hostPortSet.add(hostPort);
						}
					}
				}
				if (CollectionUtils.isNotEmpty(hostPortSet)) {
					VenusApplicationDO application = venusApplicationDAO.getApplication(service.getAppId());
					VenusServiceDefinitionDO def = new VenusServiceDefinitionDO();
					def.setInterfaceName(interfaceName);
					def.setName(serviceName);
					def.setIpAddress(hostPortSet);
					def.setActive(true);
					def.setDescription(service.getDescription());
					def.setVersion(service.getVersion());
					def.setVersionRange(service.getVersionRange());
					if (null != application) {
						def.setProvider(application.getAppCode());
					}
					List<VenusServiceConfigDO> serviceConfigs = venusServiceConfigDAO.getServiceConfigs(serviceId);
					ResultUtils.setServiceConfigs(serviceConfigs);
					def.setServiceConfigs(serviceConfigs);
					returnList.add(def);
				}
			}
		} catch (Exception e) {
			logger.error("findServiceDefinitions调用异常,url=>{},异常原因：{}", url, e);
			throw new VenusRegisteException("findServiceDefinitions调用异常,服务名：" + log_service_name(url), e);
		}
		return returnList;
	}

	public void addNewServiceMapping(String hostName, int port, String serviceName, String version,
			String description) {
		boolean exists = venusServiceMappingDAO.existServiceMapping(hostName, port, serviceName, version);
		String versionRange = version;
		if (!exists) {// 不存在则添加
			StringBuilder sb = new StringBuilder();
			sb.append("hostName=>");
			sb.append(hostName);
			sb.append(",port=>");
			sb.append(port);
			sb.append(",serviceName=>");
			sb.append(serviceName);
			sb.append(",version=>");
			sb.append(version);
			logger.error("not exits=>" + sb.toString());
			VenusServerDO server = venusServerDAO.getServer(hostName, port);
			if (null != server) {
				int serviceId = 0;
				VenusServiceDO service = venusServiceDAO.getService(serviceName, RegisteConstant.OPERATOR_REGISTE,
						versionRange);
				if (null == service) {
					int appId = saveApplication(serviceName);
					VenusServiceDO venusServiceDO = new VenusServiceDO();
					venusServiceDO.setName(serviceName);
					venusServiceDO.setAppId(appId);
					venusServiceDO.setVersion(String.valueOf(VenusConstants.VERSION_DEFAULT));// 导入时version默认为0
					venusServiceDO.setVersionRange(versionRange);
					venusServiceDO.setRegisteType(RegisteConstant.OPERATOR_REGISTE);
					venusServiceDO.setMethods(null);
					venusServiceDO.setDescription(description);
					venusServiceDO.setDelete(false);
					serviceId = venusServiceDAO.addService(venusServiceDO);
				} else {
					serviceId = service.getId();
				}

				VenusServiceMappingDO venusServiceMappingDO = new VenusServiceMappingDO();
				venusServiceMappingDO.setServerId(server.getId());
				venusServiceMappingDO.setServiceId(serviceId);
				venusServiceMappingDO.setProviderAppId(0);
				venusServiceMappingDO.setConsumerAppId(0);
				venusServiceMappingDO.setSync(true);
				venusServiceMappingDO.setActive(true);
				venusServiceMappingDO.setRole(RegisteConstant.PROVIDER);
				venusServiceMappingDO.setVersion(version);
				venusServiceMappingDO.setIsDelete(false);
				venusServiceMappingDAO.addServiceMapping(venusServiceMappingDO);
				// String versionRange=version;
				// venusServiceDAO.updateServiceVersionRange(service.getId(),
				// versionRange);

			}
		} else {
			// logger.error("exits=>"+sb.toString());
		}
	}/*
		 * else { VenusServiceDO service =
		 * venusServiceDAO.getService(serviceName,
		 * RegisteConstant.OPERATOR_REGISTE,version); if (null != service) { if
		 * (StringUtils.isNotBlank(service.getVersionRange()) &&
		 * StringUtils.isNotBlank(version)) { if
		 * (!service.getVersionRange().equals(version)) { String versionRange =
		 * version; venusServiceDAO.updateServiceVersionRange(service.getId(),
		 * versionRange); } } } }
		 */

	// public List<VenusServiceDefinitionDO> finderviceDefinitionList(String
	// interfaceName, String serviceName)
	// throws VenusRegisteException {
	// List<Integer> serverIds = new ArrayList<Integer>();
	// List<VenusServiceDefinitionDO> serviceDefinitions = new
	// ArrayList<VenusServiceDefinitionDO>();
	// try {
	// List<VenusServiceDO> services =
	// venusServiceDAO.getServices(interfaceName, serviceName);
	// if (null == services) {
	// return serviceDefinitions;
	// }
	// for (Iterator<VenusServiceDO> ite = services.iterator(); ite.hasNext();)
	// {
	// VenusServiceDO venusServiceDO = ite.next();
	// if (venusServiceDO.getIsDelete()) {
	// ite.remove();
	// }
	// }
	// for (Iterator<VenusServiceDO> ite = services.iterator(); ite.hasNext();)
	// {
	// VenusServiceDO venusServiceDO = ite.next();
	// Integer serviceId = venusServiceDO.getId();
	//
	// List<VenusServiceMappingDO> serviceMappings =
	// venusServiceMappingDAO.getServiceMapping(serviceId,
	// RegisteConstant.PROVIDER, false);
	// if (CollectionUtils.isNotEmpty(serviceMappings)) {
	// for (VenusServiceMappingDO venusServiceMappingDO : serviceMappings) {
	// if (venusServiceMappingDO.isActive()) {// 只取active的
	// Integer serverId = venusServiceMappingDO.getServerId();
	// serverIds.add(serverId);
	// }
	// }
	// }
	//
	// Set<String> hostPortSet = new HashSet<String>();
	// if (CollectionUtils.isNotEmpty(serverIds)) {
	// List<VenusServerDO> servers = venusServerDAO.getServers(serverIds);
	// if (CollectionUtils.isNotEmpty(servers)) {
	// for (Iterator<VenusServerDO> iterator = servers.iterator();
	// iterator.hasNext();) {
	// VenusServerDO venusServerDO = iterator.next();
	// String hostPort = venusServerDO.getHostname() + ":" +
	// venusServerDO.getPort();
	// hostPortSet.add(hostPort);
	// }
	// }
	// }
	// if (CollectionUtils.isNotEmpty(hostPortSet)) {
	// VenusServiceDefinitionDO def = new VenusServiceDefinitionDO();
	// def.setInterfaceName(interfaceName);
	// def.setName(serviceName);
	// def.setIpAddress(hostPortSet);
	// def.setActive(true);
	// def.setDescription(venusServiceDO.getDescription());
	// def.setSupportVersionRange(venusServiceDO.getSupportVersionRange());
	// List<VenusServiceConfigDO> serviceConfigs =
	// venusServiceConfigDAO.getServiceConfigs(serviceId);
	// ResultUtils.setServiceConfigs(serviceConfigs);
	// def.setServiceConfigs(serviceConfigs);
	// serviceDefinitions.add(def);
	// }
	// return serviceDefinitions;
	// }
	// } catch (Exception e) {
	// logger.error("服务{}ServiceDefineRunnable 运行异常 ,异常原因：{}", serviceName, e);
	// throw new VenusRegisteException("ServiceDefineRunnable 运行异常,服务名：" +
	// serviceName, e);
	// }
	// return serviceDefinitions;
	// }

	@Deprecated
	public void heartbeatSubcribe(URL url) {
		try {
			String host = url.getHost();
			VenusServerDO server = venusServerDAO.getServer(host, 0);
			if (null != server) {
				int serverID = server.getId();
				boolean update = venusServiceMappingDAO.updateHeartBeatTime(serverID, RegisteConstant.CONSUMER);
				logger.info(
						"heartbeatSubcribe updateServiceMappingHeartBeatTime serverID=>{},role=>{},isSuccess=>{},currentDate=>{},url=>{}",
						serverID, RegisteConstant.CONSUMER, update, new Date(), url);
			}
		} catch (Exception e) {
			String name = log_service_name(url);
			logger.error("服务{}subscrible更新heartBeatTime异常 ,异常原因：{}", name, e);
			throw new VenusRegisteException("subscrible更新heartBeatTime异常,服务名：" + name, e);
		}

	}

	@Deprecated
	public void heartbeatRegister(URL url) {
		try {
			String host = url.getHost();
			int port = url.getPort();
			VenusServerDO server = venusServerDAO.getServer(host, port);
			if (null != server) {
				int serverID = server.getId();
				boolean update = venusServiceMappingDAO.updateHeartBeatTime(serverID, RegisteConstant.PROVIDER);
				logger.info("heartbeatRegister serverID=>{},role=>{},isSuccess=>{},currentDate=>{},url=>{}", serverID,
						RegisteConstant.PROVIDER, update, new Date(), url);
			}
		} catch (Exception e) {
			String name = log_service_name(url);
			logger.error("服务{}registe更新heartBeatTime异常 ,异常原因：{}", name, e);
			throw new VenusRegisteException("registe更新heartBeatTime异常,服务名：" + name, e);
		}

	}

	public void heartbeatRegister(Set<URL> urls, String role) {
		if (CollectionUtils.isEmpty(urls)) {
			return;
		}
		Map<Integer, List<Integer>> maps = new HashMap<Integer, List<Integer>>();
		try {
			for (URL url : urls) {
				String host = url.getHost();
				int port = url.getPort();

				VenusServerDO server = cacheVenusServerDAO.getServer(host, port);
				if (null == server) {
					server = venusServerDAO.getServer(host, port);
				}
				if (null != server) {
					List<VenusServiceDO> services = cacheVenusServiceDAO.queryServices(url);
					if (CollectionUtils.isEmpty(services)) {
						services = venusServiceDAO.queryServices(url.getInterfaceName(), url.getServiceName(),
								url.getVersion());
					}
					if (CollectionUtils.isNotEmpty(services)) {
						for (Iterator<VenusServiceDO> iterator = services.iterator(); iterator.hasNext();) {
							VenusServiceDO venusServiceDO = iterator.next();
							List<Integer> list = maps.get(server.getId());
							if (list != null) {
								list.add(venusServiceDO.getId());
							} else {
								list = new ArrayList<>();
								list.add(venusServiceDO.getId());
								maps.put(server.getId(), list);
							}
						}
					}
				}
			}
			if (HEARTBEAT_QUEUE.size()>=QUEUE_SIZE_2000 -1) {
				logger.info("venus heartbeat drop message=>"+JSON.toJSONString(maps));
			}else {
				for (Map.Entry<Integer, List<Integer>> ent : maps.entrySet()) {
					UpdateHeartBeatTimeDTO heartBeatTimeDTO = new UpdateHeartBeatTimeDTO();
					heartBeatTimeDTO.setRole(role);
					heartBeatTimeDTO.setServerId(ent.getKey());
					heartBeatTimeDTO.setServiceIds(ent.getValue());
					HEARTBEAT_QUEUE.offer(heartBeatTimeDTO);
				}
				
			}
		} catch (Exception e) {
			logger.error("服务{}heartBeatTime入队列异常 ,异常原因：{}", JSON.toJSONString(urls, true), e);
			throw new VenusRegisteException("heartBeatTime入队列异常", e);
		}

	}

	@Deprecated
	private void update_heartbeat(final String role, final Map<Integer, List<Integer>> maps) {
		if (MapUtils.isNotEmpty(maps)) {
			this.transactionTemplate.execute(new TransactionCallback<Integer>() {
				@Override
				public Integer doInTransaction(TransactionStatus status) {
					for (Map.Entry<Integer, List<Integer>> ent : maps.entrySet()) {
						venusServiceMappingDAO.updateHeartBeatTime(ent.getKey(), ent.getValue(), role);
					}
					return 1;
				}
			});
		}
	}

	public void clearInvalidService(String currentDateTime, int second) {
		/* 订阅方提供方都清理 */
		List<VenusServiceMappingDO> serviceMappings = venusServiceMappingDAO.getServiceMappings(currentDateTime,
				second);
		if (CollectionUtils.isNotEmpty(serviceMappings)) {
			List<Integer> logic_mapping_ids = new ArrayList<Integer>();
			for (Iterator<VenusServiceMappingDO> iterator = serviceMappings.iterator(); iterator.hasNext();) {
				VenusServiceMappingDO mapping = iterator.next();
				logic_mapping_ids.add(mapping.getId());
			}
			if (CollectionUtils.isNotEmpty(logic_mapping_ids)) {
				logger.error(
						"@@@@@@logicDeleteServiceMappings currentDateTime=>{},logic_mapping_ids=>{},serviceMappings=>{}@@@@@@@",
						currentDateTime, JSON.toJSONString(logic_mapping_ids, true),
						JSON.toJSONString(serviceMappings));
				venusServiceMappingDAO.logicDeleteServiceMappings(logic_mapping_ids);
			}
		}

		List<VenusServiceMappingDO> needDeleteServiceMappings = venusServiceMappingDAO
				.queryServiceMappings(VenusConstants.DELELE_INVALID_SERVICE_HOUR);
		if (CollectionUtils.isNotEmpty(needDeleteServiceMappings)) {
			List<Integer> delete_mapping_ids = new ArrayList<Integer>();
			List<Integer> server_ids = new ArrayList<Integer>();
			for (Iterator<VenusServiceMappingDO> iterator = needDeleteServiceMappings.iterator(); iterator.hasNext();) {
				VenusServiceMappingDO mapping = iterator.next();
				delete_mapping_ids.add(mapping.getId());
				server_ids.add(mapping.getServerId());
			}

			if (CollectionUtils.isNotEmpty(delete_mapping_ids)) {
				logger.error("@@@@@@currentDateTime=>{},delete_mapping_ids=>{},serviceMappings=>{}@@@@@@@",
						currentDateTime, JSON.toJSONString(delete_mapping_ids, true),
						JSON.toJSONString(needDeleteServiceMappings));
				venusServiceMappingDAO.deleteServiceMappings(delete_mapping_ids);
			}

			for (Iterator<Integer> iterator = server_ids.iterator(); iterator.hasNext();) {
				Integer serverId = iterator.next();
				deleteServer(serverId);
			}
		}
	}

	private void deleteServer(Integer serverId) {
		int mappingCountByServerId = venusServiceMappingDAO.getMappingCountByServerId(serverId);
		if (mappingCountByServerId <= 0) {
			venusServerDAO.deleteServer(serverId);
		}
	}

	public VenusServiceDAO getVenusServiceDAO() {
		return venusServiceDAO;
	}

	public void setVenusServiceDAO(VenusServiceDAO venusServiceDAO) {
		this.venusServiceDAO = venusServiceDAO;
	}

	public VenusServiceConfigDAO getVenusServiceConfigDAO() {
		return venusServiceConfigDAO;
	}

	public void setVenusServiceConfigDAO(VenusServiceConfigDAO venusServiceConfigDAO) {
		this.venusServiceConfigDAO = venusServiceConfigDAO;
	}

	public VenusApplicationDAO getVenusApplicationDAO() {
		return venusApplicationDAO;
	}

	public void setVenusApplicationDAO(VenusApplicationDAO venusApplicationDAO) {
		this.venusApplicationDAO = venusApplicationDAO;
	}

	public VenusServerDAO getVenusServerDAO() {
		return venusServerDAO;
	}

	public void setVenusServerDAO(VenusServerDAO venusServerDAO) {
		this.venusServerDAO = venusServerDAO;
	}

	public VenusServiceMappingDAO getVenusServiceMappingDAO() {
		return venusServiceMappingDAO;
	}

	public void setVenusServiceMappingDAO(VenusServiceMappingDAO venusServiceMappingDAO) {
		this.venusServiceMappingDAO = venusServiceMappingDAO;
	}

	public CacheVenusServerDAO getCacheVenusServerDAO() {
		return cacheVenusServerDAO;
	}

	public void setCacheVenusServerDAO(CacheVenusServerDAO cacheVenusServerDAO) {
		this.cacheVenusServerDAO = cacheVenusServerDAO;
	}
	
	public CacheVenusServiceDAO getCacheVenusServiceDAO() {
		return cacheVenusServiceDAO;
	}

	public void setCacheVenusServiceDAO(CacheVenusServiceDAO cacheVenusServiceDAO) {
		this.cacheVenusServiceDAO = cacheVenusServiceDAO;
	}
	
	public TransactionTemplate getTransactionTemplate() {
		return transactionTemplate;
	}

	public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
		this.transactionTemplate = transactionTemplate;
	}

	public String getConnectUrl() {
		return connectUrl;
	}

	public void setConnectUrl(String connectUrl) {
		this.connectUrl = connectUrl;
	}

	public static void main(String args[]) {
		Date d = new Date(1506498850000L);
		System.out.println(d);
	}

	public void updateServiceAppIds() {
		logger.info("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
		Integer totalCount = venusServiceDAO.getServiceCount();
		int PAGE_SIZE_200 = 200;
		if (null != totalCount && totalCount > 0) {
			int mod = totalCount % PAGE_SIZE_200;
			int count = totalCount / PAGE_SIZE_200;
			if (mod > 0) {
				count = count + 1;
			}
			Integer mapId = null;
			for (int i = 0; i < count; i++) {
				List<VenusServiceDO> services = venusServiceDAO.queryServices(PAGE_SIZE_200, mapId);
				if (CollectionUtils.isNotEmpty(services)) {
					mapId = services.get(services.size() - 1).getId();
					for (VenusServiceDO serviceDO : services) {
						String appCode = serviceDO.getName() + "_app";
						int appId = 0;
						if ((serviceDO.getAppId() == 0 || serviceDO.getAppId() == null)
								&& StringUtils.isNotBlank(appCode)) {
							VenusApplicationDO application = venusApplicationDAO.getApplication(appCode);
							if (null == application) {// 不存在添加
								VenusApplicationDO venusApplicationDO = new VenusApplicationDO();
								venusApplicationDO.setAppCode(appCode);
								venusApplicationDO.setCreateName(RegisteConstant.PROVIDER);
								venusApplicationDO.setUpdateName(RegisteConstant.PROVIDER);
								venusApplicationDO.setProvider(true);
								venusApplicationDO.setConsumer(false);
								appId = venusApplicationDAO.addApplication(venusApplicationDO);
							} else {
								appId = application.getId();
								if (null == application.isProvider()
										|| (null != application.isProvider() && !application.isProvider())) {// 非提供方，更新
									application.setProvider(true);
									venusApplicationDAO.updateApplication(application);
								}
							}
							venusServiceDAO.updateServiceAppId(serviceDO.getId(), appId);
						}
					}
				}
			}
		}
	}

	private static String log_service_name(URL url) {
		String name = "";
		if (StringUtils.isNotBlank(url.getServiceName()) && !"null".equals(url.getServiceName())) {
			name = url.getServiceName();
		} else {
			name = url.getInterfaceName();
		}
		return name;
	}

	@Override
	public void heartbeat(Map<String, Set<URL>> maps) {
		long start = System.currentTimeMillis();
//		logger.info("heartbeat start =>" + start + "," + JSON.toJSON(maps));
		for (Map.Entry<String, Set<URL>> ent : maps.entrySet()) {
			heartbeatRegister(ent.getValue(), ent.getKey());
		}
		logger.info("heartbeat end =>" + (System.currentTimeMillis() - start));
	}
	
	private class UpdateHeartbeatTimeRunnable implements Runnable{
			
		private String threadName;
		
		public String getThreadName() {
			return threadName;
		}

		public void setThreadName(String threadName) {
			this.threadName = threadName;
		}

		public UpdateHeartbeatTimeRunnable(String threadName) {
			this.threadName = threadName;
		}

		@Override
		public void run() {
			try {
				while (needRun) {
					UpdateHeartBeatTimeDTO heartbeatDto = HEARTBEAT_QUEUE.take();
					venusServiceMappingDAO.updateHeartBeatTime(heartbeatDto.getServerId(), heartbeatDto.getServiceIds(), heartbeatDto.getRole());
				}
			} catch (Exception e) {
				logger.error("UpdateHeartbeatTimeRunnable consumer thread is error" + e.getMessage(), e);
			}
		}
		
	}

	@Override
	public void destroy() throws Exception {
		needRun = false;
		es.shutdown();
	}

}
