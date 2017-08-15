package com.meidusa.venus.registry.mysql;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import com.meidusa.toolkit.common.runtime.GlobalScheduler;
import com.meidusa.venus.URL;
import com.meidusa.venus.registry.Register;
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
 * mysql服务注册中心类 Created by Zhangzhihua on 2017/7/27.
 */
public class MysqlRegister implements Register {

	private static Logger logger = LoggerFactory.getLogger(MysqlRegister.class);

	/** 已注册成功的URL */
	private Set<URL> registeUrls = new HashSet<URL>();

	/** 已订阅成功的URL */
	private Set<URL> subscribleUrls = new HashSet<URL>();

	/** 注册失败的URLS */
	private Set<URL> registeFailUrls = new HashSet<URL>();// 失败的继续跑启线程定时运行

	/** 订阅失败的URLS */
	private Set<URL> subscribleFailUrls = new HashSet<URL>();// 失败的继续跑启线程定时运行

	/** 已订阅成功的 服务定义对象 */
	private Set<ServiceDefinition> subscribleServiceDefinitions = new HashSet<ServiceDefinition>();

	private VenusServiceDAO venusServiceDAO;

	private VenusServiceConfigDAO venusServiceConfigDAO;

	private VenusApplicationDAO venusApplicationDAO;

	private VenusServerDAO venusServerDAO;

	private VenusServiceMappingDAO venusServiceMappingDAO;

	private boolean loadRunning = false;

	private boolean heartbeatRunning = false;

	private static BasicDataSource dataSource;

	private static JdbcTemplate jdbcTemplate;

	private static MysqlRegister register = new MysqlRegister();

	private MysqlRegister() {
		try {
			init();
		} catch (Exception e) {
			logger.error("init初始化异常,异常原因：{} ", e);
		}
	}

	/**
	 * url =
	 * "jdbc:mysql://10.32.173.250/registry_new?username=registry&password=registry";
	 * 
	 * @param url
	 * @return
	 */
	public final static MysqlRegister getInstance(String url) {
		dataSource = DataSourceUtil.getBasicDataSource(url);
		if (jdbcTemplate == null) {
			synchronized (MysqlRegister.class) {
				if (jdbcTemplate == null) {
					jdbcTemplate = new JdbcTemplate(dataSource);
				}
			}
			register.setVenusApplicationDAO(new VenusApplicationDaoImpl(jdbcTemplate));
			register.setVenusServerDAO(new VenusServerDaoImpl(jdbcTemplate));
			register.setVenusServiceConfigDAO(new VenusServiceConfigDaoImpl(jdbcTemplate));
			register.setVenusServiceDAO(new VenusServiceDaoImpl(jdbcTemplate));
			register.setVenusServiceMappingDAO(new VenusServiceMappingDaoImpl(jdbcTemplate));
		}

		return register;
	}

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
				serviceId = venusServiceDAO.addService(venusServiceDO);
			} else {
				serviceId = service.getId();
				if (StringUtils.isNotBlank(url.getMethods())) {
					venusServiceDAO.updateService(url.getMethods(), serviceId);
				}
			}

			VenusServiceMappingDO serviceMapping = venusServiceMappingDAO.getServiceMapping(serverId, serviceId,
					RegisteConstant.PROVIDER);
			if (null == serviceMapping) {
				VenusServiceMappingDO venusServiceMappingDO = new VenusServiceMappingDO();
				venusServiceMappingDO.setServerId(serverId);
				venusServiceMappingDO.setServiceId(serviceId);
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
		} catch (Exception e) {
			registeFailUrls.add(url);
			logger.error("服务{}注册异常,异常原因：{} ", url.getServiceName(), e);
			throw new VenusRegisteException("服务注册异常" + url.getServiceName(), e);
		}
		registeUrls.add(url);

	}

	@Override
	public void unregiste(URL url) throws VenusRegisteException {
		if (StringUtils.isBlank(url.getVersion())) {
			logger.error("服务{}取消注册异常,异常原因：{} ", url.getServiceName(), "version为空");
			throw new VenusRegisteException("取消注册异常" + url.getServiceName() + ",version为空");
		}
		try {
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
						venusServiceMappingDAO.deleteServiceMapping(serverId, serviceId, url.getVersion(),
								RegisteConstant.PROVIDER);
						registeUrls.remove(url);
					}
				}
			}
		} catch (Exception e) {
			logger.error("服务{}取消注册异常,异常原因：{} ", url.getServiceName(), e);
			throw new VenusRegisteException("取消注册异常" + url.getServiceName(), e);
		}
	}

	@Override
	public void subscrible(URL url) throws VenusRegisteException {
		try {
			VenusServiceDO service = venusServiceDAO.getService(url.getServiceName(), url.getVersion(),
					url.getInterfaceName());
			if (null == service && url.isConsumerCheck()) {// 服务不存在并且配置了检测则抛出异常
				logger.error("服务订阅异常,原因:服务{}不存在 ", url.getServiceName());
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
				venusServiceMappingDO.setConsumerAppId(appId);
				venusServiceMappingDAO.addServiceMapping(venusServiceMappingDO);
			} else {
				venusServiceMappingDAO.updateServiceMapping(serviceMapping.getId(), true, false);
			}
		} catch (Exception e) {
			subscribleFailUrls.add(url);
			logger.error("服务{}订阅异常 ,异常原因：{}", url.getServiceName(), e);
			throw new VenusRegisteException("服务订阅异常" + url.getServiceName(), e);
		}
		subscribleUrls.add(url);

	}

	@Override
	public void unsubscrible(URL url) throws VenusRegisteException {
		if (StringUtils.isBlank(url.getVersion())) {
			logger.error("服务{}取消订阅异常,异常原因：{} ", url.getServiceName(), "version为空");
			throw new VenusRegisteException("取消订阅异常" + url.getServiceName() + ",version为空");
		}
		try {
			VenusServiceDO service = venusServiceDAO.getService(url.getServiceName(), url.getVersion(),
					url.getInterfaceName());
			if (null != service) {
				VenusServerDO server = venusServerDAO.getServer(url.getHost(), 0);
				if (null != server) {
					VenusServiceMappingDO serviceMapping = venusServiceMappingDAO.getServiceMapping(server.getId(),
							service.getId(), RegisteConstant.CONSUMER);
					if (null != serviceMapping) {
						venusServiceMappingDAO.deleteServiceMapping(server.getId(), service.getId(), url.getVersion(),
								RegisteConstant.CONSUMER);
						subscribleUrls.remove(url);
					}
				}
			}
		} catch (Exception e) {
			logger.error("服务{}取消订阅异常 ,异常原因：{}", url.getServiceName(), e);
			throw new VenusRegisteException("取消订阅异常" + url.getServiceName(), e);
		}
	}

	@Override
	public void heartbeat() throws VenusRegisteException {
		if (!heartbeatRunning) {
			GlobalScheduler.getInstance().scheduleAtFixedRate(new HeartBeatRunnable(), 10, 10, TimeUnit.SECONDS);
			heartbeatRunning = true;
		}
	}

	@Override
	public void clearInvalid() throws VenusRegisteException {
		registeFailUrls.clear();
		subscribleFailUrls.clear();
	}

	@Override
	public ServiceDefinition lookup(URL url) throws VenusRegisteException {
		// ServiceDefineRunnable run = new ServiceDefineRunnable();
		// run.run();//测试接口时用
		// 接口名 服务名 版本号 加载服务的server信息及serviceConfig信息
		// 根据本地 ServiceDefinition 列表去查找
		String serviceName = url.getServiceName();
		String interfaceName = url.getInterfaceName();
		String version = url.getVersion();
		for (Iterator<ServiceDefinition> iterator = subscribleServiceDefinitions.iterator(); iterator.hasNext();) {
			ServiceDefinition define = iterator.next();
			if (null != define && define.getName().equals(serviceName)) {
				if (version.equals(define.getVersionRange())) {// TODO version
					return define;
				}
			}
		}
		return null;
	}

	@Override
	public void load() throws VenusRegisteException {
		if (!loadRunning) {
			GlobalScheduler.getInstance().scheduleAtFixedRate(new ServiceDefineRunnable(), 10, 60, TimeUnit.SECONDS);
			loadRunning = true;
		}
	}

	@Override
	public void destroy() throws VenusRegisteException {
		registeUrls.clear();
		subscribleUrls.clear();
		registeFailUrls.clear();
		subscribleFailUrls.clear();
		subscribleServiceDefinitions.clear();
	}

	private class ServiceDefineRunnable implements Runnable {
		public void run() {
			if (CollectionUtils.isNotEmpty(subscribleUrls)) {
				for (URL url : subscribleUrls) {
					String interfaceName = url.getInterfaceName();
					String serviceName = url.getServiceName();
					String version = url.getVersion();
					try {
						List<Integer> serverIds = new ArrayList<Integer>();
						VenusServiceDO service = venusServiceDAO.getService(serviceName, version, interfaceName);
						if (null == service) {
							continue;
						}
						Integer serviceId = service.getId();
						List<VenusServiceMappingDO> serviceMappings = venusServiceMappingDAO
								.getServiceMapping(serviceId, RegisteConstant.PROVIDER, false);
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
							List<VenusServiceConfigDO> serviceConfigs = venusServiceConfigDAO
									.getServiceConfigs(serviceId);
							ResultUtils.setServiceConfigs(serviceConfigs);
							def.setServiceConfigs(serviceConfigs);
							if (subscribleServiceDefinitions.size() < 1000) {
								subscribleServiceDefinitions.add(def);
							}
						}
					} catch (Exception e) {
						logger.error("服务{}ServiceDefineRunnable 运行异常 ,异常原因：{}", url.getServiceName(), e);
						throw new VenusRegisteException("ServiceDefineRunnable 运行异常,服务名：" + url.getServiceName(), e);
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
								RegisteConstant.PROVIDER);
					} catch (Exception e) {
						logger.error("服务{}registe更新heartBeatTime异常 ,异常原因：{}", url.getServiceName(), e);
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
								RegisteConstant.CONSUMER);
					} catch (Exception e) {
						logger.error("服务{}subscrible更新heartBeatTime异常 ,异常原因：{}", url.getServiceName(), e);
						throw new VenusRegisteException("subscrible更新heartBeatTime异常,服务名：" + url.getServiceName(), e);
					}
				}
			}
		}
	}

	public void init() throws Exception {
		GlobalScheduler.getInstance().scheduleAtFixedRate(new UrlFailRunnable(), 5, 10, TimeUnit.SECONDS);
	}

	private class UrlFailRunnable implements Runnable {
		@Override
		public void run() {
			if (CollectionUtils.isNotEmpty(registeFailUrls)) {
				for (Iterator<URL> iterator = registeFailUrls.iterator(); iterator.hasNext();) {
					URL url = iterator.next();
					try {
						registe(url);
						iterator.remove();
					} catch (Exception e) {
						logger.error("Fail服务{}重新注册异常 ,异常原因：{}", url.getServiceName(), e);
					}
				}
			}
			if (CollectionUtils.isNotEmpty(subscribleFailUrls)) {
				for (Iterator<URL> iterator = subscribleFailUrls.iterator(); iterator.hasNext();) {
					URL url = iterator.next();
					try {
						subscrible(url);
						iterator.remove();
					} catch (Exception e) {
						logger.error("Fail服务{}重新订阅异常 ,异常原因：{}", url.getServiceName(), e);
					}
				}
			}

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
