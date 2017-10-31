package com.meidusa.venus.registry.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.meidusa.venus.registry.domain.*;
import com.meidusa.venus.registry.service.RegisterService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import com.meidusa.fastjson.JSON;
import com.meidusa.venus.URL;
import com.meidusa.venus.registry.VenusRegisteException;
import com.meidusa.venus.registry.dao.VenusApplicationDAO;
import com.meidusa.venus.registry.dao.VenusServerDAO;
import com.meidusa.venus.registry.dao.VenusServiceConfigDAO;
import com.meidusa.venus.registry.dao.VenusServiceDAO;
import com.meidusa.venus.registry.dao.VenusServiceMappingDAO;
import com.meidusa.venus.registry.dao.impl.DataSourceUtil;
import com.meidusa.venus.registry.dao.impl.ResultUtils;
import com.meidusa.venus.registry.dao.impl.VenusApplicationDaoImpl;
import com.meidusa.venus.registry.dao.impl.VenusServerDaoImpl;
import com.meidusa.venus.registry.dao.impl.VenusServiceConfigDaoImpl;
import com.meidusa.venus.registry.dao.impl.VenusServiceDaoImpl;
import com.meidusa.venus.registry.dao.impl.VenusServiceMappingDaoImpl;
import com.meidusa.venus.registry.data.move.OldServiceDO;
import com.meidusa.venus.registry.data.move.OldServiceMappingDO;
import org.springframework.stereotype.Component;

/**
 * Created by Zhangzhihua on 2017/8/16.
 */
public class MysqlRegisterService implements RegisterService {

	private static Logger logger = LoggerFactory.getLogger(MysqlRegisterService.class);

	private VenusServiceDAO venusServiceDAO;

	private VenusServiceConfigDAO venusServiceConfigDAO;

	private VenusApplicationDAO venusApplicationDAO;

	private VenusServerDAO venusServerDAO;

	private VenusServiceMappingDAO venusServiceMappingDAO;

	private BasicDataSource dataSource;

	private JdbcTemplate jdbcTemplate;

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
		String url = this.getConnectUrl();
		if (!url.startsWith("mysql://")) {
			logger.error("URL 参数异常,非jdbc mysql协议,url=>{}", url);
			throw new IllegalArgumentException("URL 参数异常,非jdbc mysql协议,url=>" + url);
		}
		if (!url.contains("username=")) {
			logger.error("URL 参数异常,未包含用户名,url=>{}", url);
			throw new IllegalArgumentException("URL 参数异常,未包含用户名,url=>" + url);
		}
		if (!url.contains("password=")) {
			logger.error("URL 参数异常,未包含密码,url=>{}", url);
			throw new IllegalArgumentException("URL 参数异常,未包含密码,url=>" + url);
		}
		dataSource = DataSourceUtil.getBasicDataSource(url);
		if (jdbcTemplate == null) {
			synchronized (MysqlRegisterService.class) {
				if (jdbcTemplate == null) {
					jdbcTemplate = new JdbcTemplate(dataSource);
				}
				if (null == this.getVenusApplicationDAO()) {
					this.setVenusApplicationDAO(new VenusApplicationDaoImpl(jdbcTemplate));
				}
				if (null == this.getVenusServerDAO()) {
					this.setVenusServerDAO(new VenusServerDaoImpl(jdbcTemplate));
				}
				if (null == this.getVenusServiceConfigDAO()) {
					this.setVenusServiceConfigDAO(new VenusServiceConfigDaoImpl(jdbcTemplate));
				}
				if (null == this.getVenusServiceDAO()) {
					this.setVenusServiceDAO(new VenusServiceDaoImpl(jdbcTemplate));
				}
				if (null == this.getVenusServiceMappingDAO()) {
					this.setVenusServiceMappingDAO(new VenusServiceMappingDaoImpl(jdbcTemplate));
				}
			}
		}
		//updateServiceAppIds();
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
			venusServiceDO.setIsDelete(false);
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
			venusServiceMappingDAO.updateProviderServiceMapping(serviceMapping.getId(), true, false,appId);
			String oldVersion = serviceMapping.getVersion();// 有区间的version需特殊处理

		}
	}

	public int addServer(String host,int port) {
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
	
	public int addService(String serviceName, String description, String version){
		VenusServiceDO service = venusServiceDAO.getService(serviceName,version);
		int serviceId = 0;
		if (null == service) {
			String oldAppCode=serviceName+"_app";
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
			VenusServiceDO venusServiceDO = new VenusServiceDO();
			venusServiceDO.setName(serviceName);
			venusServiceDO.setAppId(appId);
			venusServiceDO.setVersion(version);
			venusServiceDO.setRegisteType(RegisteConstant.OPERATOR_REGISTE);
			venusServiceDO.setMethods(null);
			venusServiceDO.setDescription(description);
			venusServiceDO.setIsDelete(false);
			serviceId = venusServiceDAO.addService(venusServiceDO);
		}
		return serviceId;
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
			logger.error("服务订阅异常,原因:服务{}不存在 ", url.getServiceName());
			throw new VenusRegisteException("服务订阅异常,原因:服务" + url.getServiceName() + "不存在");
		}
		if (service.getIsDelete() && url.isConsumerCheck()) {// 服务不存在并且配置了检测则抛出异常
			logger.error("服务订阅异常,原因:服务{}已删除", url.getServiceName());
			throw new VenusRegisteException("服务订阅异常,原因:服务" + url.getServiceName() + "不存在");
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
			venusServiceMappingDAO.updateSubcribeServiceMapping(serviceMapping.getId(),appId, true, false);
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
						 boolean deleteServiceMapping = venusServiceMappingDAO.deleteServiceMapping(serviceMapping.getId());
						 deleteServer(server.getId());
						 return deleteServiceMapping;
					}
				}
			}
		}
		return false;
	}

	public List<VenusServiceDefinitionDO> findServiceDefinitions(URL url) {
		List<VenusServiceDefinitionDO> returnList=new ArrayList<VenusServiceDefinitionDO>();
		String interfaceName = url.getInterfaceName();
		String serviceName = url.getServiceName();
		String version = url.getVersion();
		List<Integer> serverIds = new ArrayList<Integer>();
		try {
			List<VenusServiceDO> services = venusServiceDAO.queryServices(interfaceName, serviceName, version);//servicePath interfaceName/serviceName?version=version
			for (Iterator<VenusServiceDO> ite = services.iterator(); ite.hasNext();) {
			VenusServiceDO service =  ite.next();
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
				def.setVersionRange(service.getVersion());
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
			throw new VenusRegisteException("findServiceDefinitions调用异常,服务名：" + url.getServiceName(), e);
		}
		return returnList;
	}
	
	public void addNewServiceMapping(String hostName,int port,String serviceName,String version) {
		boolean exists = venusServiceMappingDAO.existServiceMapping(hostName, port,
				serviceName, version);
		if (!exists) {// 不存在则添加
			VenusServiceMappingDO venusServiceMappingDO = new VenusServiceMappingDO();
			VenusServerDO server = venusServerDAO.getServer(hostName, port);
			if (null != server) {
				VenusServiceDO service = venusServiceDAO.getService(serviceName,RegisteConstant.OPERATOR_REGISTE);
				if (null != service) {
					venusServiceMappingDO.setServerId(server.getId());
					venusServiceMappingDO.setServiceId(service.getId());
					venusServiceMappingDO.setProviderAppId(0);
					venusServiceMappingDO.setConsumerAppId(0);
					venusServiceMappingDO.setSync(true);
					venusServiceMappingDO.setActive(true);
					venusServiceMappingDO.setRole(RegisteConstant.PROVIDER);
					venusServiceMappingDO.setVersion(version);
					venusServiceMappingDO.setIsDelete(false);
					venusServiceMappingDAO.addServiceMapping(venusServiceMappingDO);
				}
			}
		}
	}
	
//	public List<VenusServiceDefinitionDO> finderviceDefinitionList(String interfaceName, String serviceName)
//			throws VenusRegisteException {
//		List<Integer> serverIds = new ArrayList<Integer>();
//		List<VenusServiceDefinitionDO> serviceDefinitions = new ArrayList<VenusServiceDefinitionDO>();
//		try {
//			List<VenusServiceDO> services = venusServiceDAO.getServices(interfaceName, serviceName);
//			if (null == services) {
//				return serviceDefinitions;
//			}
//			for (Iterator<VenusServiceDO> ite = services.iterator(); ite.hasNext();) {
//				VenusServiceDO venusServiceDO = ite.next();
//				if (venusServiceDO.getIsDelete()) {
//					ite.remove();
//				}
//			}
//			for (Iterator<VenusServiceDO> ite = services.iterator(); ite.hasNext();) {
//				VenusServiceDO venusServiceDO = ite.next();
//				Integer serviceId = venusServiceDO.getId();
//
//				List<VenusServiceMappingDO> serviceMappings = venusServiceMappingDAO.getServiceMapping(serviceId,
//						RegisteConstant.PROVIDER, false);
//				if (CollectionUtils.isNotEmpty(serviceMappings)) {
//					for (VenusServiceMappingDO venusServiceMappingDO : serviceMappings) {
//						if (venusServiceMappingDO.isActive()) {// 只取active的
//							Integer serverId = venusServiceMappingDO.getServerId();
//							serverIds.add(serverId);
//						}
//					}
//				}
//
//				Set<String> hostPortSet = new HashSet<String>();
//				if (CollectionUtils.isNotEmpty(serverIds)) {
//					List<VenusServerDO> servers = venusServerDAO.getServers(serverIds);
//					if (CollectionUtils.isNotEmpty(servers)) {
//						for (Iterator<VenusServerDO> iterator = servers.iterator(); iterator.hasNext();) {
//							VenusServerDO venusServerDO = iterator.next();
//							String hostPort = venusServerDO.getHostname() + ":" + venusServerDO.getPort();
//							hostPortSet.add(hostPort);
//						}
//					}
//				}
//				if (CollectionUtils.isNotEmpty(hostPortSet)) {
//					VenusServiceDefinitionDO def = new VenusServiceDefinitionDO();
//					def.setInterfaceName(interfaceName);
//					def.setName(serviceName);
//					def.setIpAddress(hostPortSet);
//					def.setActive(true);
//					def.setDescription(venusServiceDO.getDescription());
//					def.setVersionRange(venusServiceDO.getVersion());
//					List<VenusServiceConfigDO> serviceConfigs = venusServiceConfigDAO.getServiceConfigs(serviceId);
//					ResultUtils.setServiceConfigs(serviceConfigs);
//					def.setServiceConfigs(serviceConfigs);
//					serviceDefinitions.add(def);
//				}
//				return serviceDefinitions;
//			}
//		} catch (Exception e) {
//			logger.error("服务{}ServiceDefineRunnable 运行异常 ,异常原因：{}", serviceName, e);
//			throw new VenusRegisteException("ServiceDefineRunnable 运行异常,服务名：" + serviceName, e);
//		}
//		return serviceDefinitions;
//	}

	public void heartbeatSubcribe(URL url) {
		String interfaceName = url.getInterfaceName();
		String serviceName = url.getServiceName();
		String version = url.getVersion();
		try {
			/*
			 * List<VenusServiceDO> services =
			 * venusServiceDAO.queryServices(interfaceName, serviceName,
			 * version); for (Iterator<VenusServiceDO> iterator =
			 * services.iterator(); iterator.hasNext();) { VenusServiceDO
			 * service = iterator.next(); int serviceID = service.getId(); }
			 */
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
			logger.error("服务{}subscrible更新heartBeatTime异常 ,异常原因：{}", url.getServiceName(), e);
			throw new VenusRegisteException("subscrible更新heartBeatTime异常,服务名：" + url.getServiceName(), e);
		}

	}

	public void heartbeatRegister(URL url) {
		String interfaceName = url.getInterfaceName();
		String serviceName = url.getServiceName();
		String version = url.getVersion();

		try {
			/*
			 * List<VenusServiceDO> services =
			 * venusServiceDAO.queryServices(interfaceName, serviceName,
			 * version); for (Iterator<VenusServiceDO> iterator =
			 * services.iterator(); iterator.hasNext();) { VenusServiceDO
			 * service = iterator.next(); int serviceID = service.getId(); }
			 */
			String host = url.getHost();
			int port = url.getPort();
			VenusServerDO server = venusServerDAO.getServer(host, port);
			if (null != server) {
				int serverID = server.getId();
				boolean update = venusServiceMappingDAO.updateHeartBeatTime(serverID,
						RegisteConstant.PROVIDER);
				logger.info("heartbeatRegister serverID=>{},role=>{},isSuccess=>{},currentDate=>{},url=>{}", serverID,
						RegisteConstant.PROVIDER, update, new Date(), url);
			}
		} catch (Exception e) {
			logger.error("服务{}registe更新heartBeatTime异常 ,异常原因：{}", url.getServiceName(), e);
			throw new VenusRegisteException("registe更新heartBeatTime异常,服务名：" + url.getServiceName(), e);
		}

	}

	public void clearInvalidService(String currentDateTime, int second) {
		/* 订阅方提供方都清理 */
		List<VenusServiceMappingDO> serviceMappings = venusServiceMappingDAO.getServiceMappings(currentDateTime,second);
		if (CollectionUtils.isNotEmpty(serviceMappings)) {
			/*Map<Integer, Integer> map = new HashMap<Integer, Integer>();
			for (Iterator<VenusServiceMappingDO> iterator = serviceMappings.iterator(); iterator.hasNext();) {
				VenusServiceMappingDO mapping = iterator.next();
				map.put(mapping.getId(), mapping.getServiceId());
			}

			List<Integer> delete_mapping_ids = new ArrayList<Integer>();
			if (MapUtils.isNotEmpty(map)) {
				List<VenusServiceDO> services = venusServiceDAO.getServices(map.values());
				if (CollectionUtils.isNotEmpty(services)) {
					for (VenusServiceDO venusServiceDO : services) {
						if (null != venusServiceDO.getRegisteType()
								&& venusServiceDO.getRegisteType() == RegisteConstant.AUTO_REGISTE) {
							for (Map.Entry<Integer, Integer> ent : map.entrySet()) {
								if (venusServiceDO.getId().intValue() == ent.getValue().intValue()) {
									delete_mapping_ids.add(ent.getKey());
								}
							}
						}
					}
				}
			}*/
			
			List<Integer> delete_mapping_ids = new ArrayList<Integer>();
			List<Integer> server_ids = new ArrayList<Integer>();
			for (Iterator<VenusServiceMappingDO> iterator = serviceMappings.iterator(); iterator.hasNext();) {
				VenusServiceMappingDO mapping = iterator.next();
				delete_mapping_ids.add(mapping.getId());
				server_ids.add(mapping.getServerId());
			}

			if (CollectionUtils.isNotEmpty(delete_mapping_ids)) {
				logger.info("@@@@@@currentDateTime=>{},delete_mapping_ids=>{},serviceMappings=>{}@@@@@@@",currentDateTime,JSON.toJSONString(delete_mapping_ids, true),JSON.toJSONString(serviceMappings));
				venusServiceMappingDAO.deleteServiceMappings(delete_mapping_ids);
			}
			
			for (Iterator<Integer> iterator = server_ids.iterator(); iterator.hasNext();) {
				Integer serverId = iterator.next();
				deleteServer(serverId);
			}

			/*List<VenusServiceMappingDO> deleteServiceMappings = venusServiceMappingDAO
					.getDeleteServiceMappings(updateTime, RegisteConstant.PROVIDER, true);// 取两分钟内删除的服务提供者
			Set<Integer> serviceIds = new HashSet<Integer>();
			if (CollectionUtils.isNotEmpty(deleteServiceMappings)) {
				for (VenusServiceMappingDO venusServiceMappingDO : deleteServiceMappings) {
					serviceIds.add(venusServiceMappingDO.getServiceId());
				}

				if (CollectionUtils.isNotEmpty(serviceIds)) {
					for (Integer sid : serviceIds) {
						List<VenusServiceMappingDO> serviceMappings2 = venusServiceMappingDAO.getServiceMappings(sid);
						int deleteSize = 0;
						for (Iterator<VenusServiceMappingDO> iterator = serviceMappings2.iterator(); iterator
								.hasNext();) {
							VenusServiceMappingDO vsd = iterator.next();
							if (vsd.getIsDelete()) {
								deleteSize++;
							}
						}

						if (deleteSize != 0 && deleteSize == serviceMappings2.size()) {
							venusServiceDAO.updateService(sid, true);// 更新service表
						}
					}
				}
			}*/
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

	public String getConnectUrl() {
		return connectUrl;
	}

	public void setConnectUrl(String connectUrl) {
		this.connectUrl = connectUrl;
	}

	public static void main(String args[]){
		Date d=new Date(1506498850000L);
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
						if ((serviceDO.getAppId() ==0 ||serviceDO.getAppId() ==null) && StringUtils.isNotBlank(appCode)) {
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

}
