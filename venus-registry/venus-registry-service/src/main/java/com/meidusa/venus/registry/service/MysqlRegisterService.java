package com.meidusa.venus.registry.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import com.meidusa.venus.URL;
import com.meidusa.venus.registry.RegisterService;
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
import com.meidusa.venus.registry.domain.RegisteConstant;
import com.meidusa.venus.registry.domain.VenusApplicationDO;
import com.meidusa.venus.registry.domain.VenusServerDO;
import com.meidusa.venus.registry.domain.VenusServiceConfigDO;
import com.meidusa.venus.registry.domain.VenusServiceDO;
import com.meidusa.venus.registry.domain.VenusServiceMappingDO;
import com.meidusa.venus.service.registry.ServiceDefinition;

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

	private static BasicDataSource dataSource;

	private static JdbcTemplate jdbcTemplate;

	private static MysqlRegisterService mysqlRegisterService = new MysqlRegisterService();

	/**
	 * url =
	 * "mysql://10.32.173.250:3306/registry_new?username=registry&password=registry";
	 * 
	 * @param url
	 * @return
	 */
	public final static MysqlRegisterService getInstance(String url) {
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
				mysqlRegisterService.setVenusApplicationDAO(new VenusApplicationDaoImpl(jdbcTemplate));
				mysqlRegisterService.setVenusServerDAO(new VenusServerDaoImpl(jdbcTemplate));
				mysqlRegisterService.setVenusServiceConfigDAO(new VenusServiceConfigDaoImpl(jdbcTemplate));
				mysqlRegisterService.setVenusServiceDAO(new VenusServiceDaoImpl(jdbcTemplate));
				mysqlRegisterService.setVenusServiceMappingDAO(new VenusServiceMappingDaoImpl(jdbcTemplate));
			}
		}

		return mysqlRegisterService;
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
			venusServiceDO.setRegisteType(RegisteConstant.AUTO_REGISTE);
			venusServiceDO.setMethods(url.getMethods());
			venusServiceDO.setIsDelete(false);
			serviceId = venusServiceDAO.addService(venusServiceDO);
		} else {
			serviceId = service.getId();
			if (StringUtils.isNotBlank(url.getMethods())) {
				venusServiceDAO.updateService(url.getMethods(), false, serviceId);
			}
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
			venusServiceMappingDAO.updateServiceMapping(serviceMapping.getId(), true, false);
			String oldVersion = serviceMapping.getVersion();// 有区间的version需特殊处理

		}
	}

	@Override
	public boolean unregiste(URL url) throws VenusRegisteException {
		VenusServiceDO service = venusServiceDAO.getService(url.getServiceName(), url.getVersion(),
				url.getInterfaceName());
		if (null != service && null != service.getRegisteType()
				&& service.getRegisteType() == RegisteConstant.AUTO_REGISTE) {// 自动注册的逻辑删除,手动注册的不删除
			VenusServerDO server = venusServerDAO.getServer(url.getHost(), url.getPort());
			if (null != server) {
				int serverId = server.getId();
				int serviceId = service.getId();
				VenusServiceMappingDO serviceMapping = venusServiceMappingDAO.getServiceMapping(serverId, serviceId,
						RegisteConstant.PROVIDER);
				if (null != serviceMapping) {
					return venusServiceMappingDAO.deleteServiceMapping(serverId, serviceId, url.getVersion(),
							RegisteConstant.PROVIDER);
				}
			}
		}
		return false;
	}

	@Override
	public void subscrible(URL url) throws VenusRegisteException {
		VenusServiceDO service = venusServiceDAO.getService(url.getServiceName(), url.getVersion(),
				url.getInterfaceName());
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
			venusServiceMappingDAO.updateServiceMapping(serviceMapping.getId(), true, false);
		}
	}

	@Override
	public boolean unsubscrible(URL url) throws VenusRegisteException {
		VenusServiceDO service = venusServiceDAO.getService(url.getServiceName(), url.getVersion(),
				url.getInterfaceName());
		if (null != service) {
			VenusServerDO server = venusServerDAO.getServer(url.getHost(), 0);
			if (null != server) {
				VenusServiceMappingDO serviceMapping = venusServiceMappingDAO.getServiceMapping(server.getId(),
						service.getId(), RegisteConstant.CONSUMER);
				if (null != serviceMapping) {
					return venusServiceMappingDAO.deleteServiceMapping(server.getId(), service.getId(),
							url.getVersion(), RegisteConstant.CONSUMER);
				}
			}
		}
		return false;
	}

	@Override
	public void heartbeat() throws VenusRegisteException {

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

	public ServiceDefinition urlToServiceDefine(URL url) {
		String interfaceName = url.getInterfaceName();
		String serviceName = url.getServiceName();
		String version = url.getVersion();
		List<Integer> serverIds = new ArrayList<Integer>();
		try {
			VenusServiceDO service = venusServiceDAO.getService(serviceName, version, interfaceName);
			if (null == service || (null != service && service.getIsDelete())) {
				return null;
			}
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
				ServiceDefinition def = new ServiceDefinition();
				def.setName(serviceName);
				def.setIpAddress(hostPortSet);
				def.setActive(true);
				def.setDescription(service.getDescription());
				def.setVersionRange(version);
				List<VenusServiceConfigDO> serviceConfigs = venusServiceConfigDAO.getServiceConfigs(serviceId);
				ResultUtils.setServiceConfigs(serviceConfigs);
				def.setServiceConfigs(serviceConfigs);
				return def;
			}
		} catch (Exception e) {
			logger.error("服务{}ServiceDefineRunnable 运行异常 ,异常原因：{}", url.getServiceName(), e);
			throw new VenusRegisteException("ServiceDefineRunnable 运行异常,服务名：" + url.getServiceName(), e);
		}

		return null;
	}

	public void heartbeatSubcribe(URL url) {
		String interfaceName = url.getInterfaceName();
		String serviceName = url.getServiceName();
		String version = url.getVersion();
		try {
			VenusServiceDO service = venusServiceDAO.getService(serviceName, version, interfaceName);
			if (service.getIsDelete()) {
				return;
			}
			int serviceID = service.getId();
			String host = url.getHost();
			VenusServerDO server = venusServerDAO.getServer(host, 0);
			int serverID = server.getId();
			venusServiceMappingDAO.updateServiceMappingHeartBeatTime(serverID, serviceID, version,
					RegisteConstant.CONSUMER);
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
			VenusServiceDO service = venusServiceDAO.getService(serviceName, version, interfaceName);
			if (service.getIsDelete()) {
				return;
			}
			int serviceID = service.getId();
			String host = url.getHost();
			int port = url.getPort();
			VenusServerDO server = venusServerDAO.getServer(host, port);
			int serverID = server.getId();
			venusServiceMappingDAO.updateServiceMappingHeartBeatTime(serverID, serviceID, version,
					RegisteConstant.PROVIDER);
		} catch (Exception e) {
			logger.error("服务{}registe更新heartBeatTime异常 ,异常原因：{}", url.getServiceName(), e);
			throw new VenusRegisteException("registe更新heartBeatTime异常,服务名：" + url.getServiceName(), e);
		}

	}

	public void clearInvalidService(String currentDateTime) {
		List<VenusServiceMappingDO> serviceMappings = venusServiceMappingDAO.getServiceMappings(currentDateTime);
		if (CollectionUtils.isNotEmpty(serviceMappings)) {
			List<Integer> ids = new ArrayList<Integer>();
			for (Iterator<VenusServiceMappingDO> iterator = serviceMappings.iterator(); iterator.hasNext();) {
				VenusServiceMappingDO mapping = iterator.next();
				Integer id = mapping.getId();
				ids.add(id);
			}
			venusServiceMappingDAO.updateServiceMappings(ids);
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

}
