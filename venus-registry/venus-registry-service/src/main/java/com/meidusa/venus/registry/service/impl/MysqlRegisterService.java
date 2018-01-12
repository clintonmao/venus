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
import org.springframework.beans.factory.DisposableBean;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.meidusa.fastjson.JSON;
import com.meidusa.venus.URL;
import com.meidusa.venus.registry.LogUtils;
import com.meidusa.venus.registry.VenusRegisteException;
import com.meidusa.venus.registry.dao.CacheVenusServerDAO;
import com.meidusa.venus.registry.dao.CacheVenusServiceDAO;
import com.meidusa.venus.registry.dao.VenusApplicationDAO;
import com.meidusa.venus.registry.dao.VenusServerDAO;
import com.meidusa.venus.registry.dao.VenusServiceConfigDAO;
import com.meidusa.venus.registry.dao.VenusServiceDAO;
import com.meidusa.venus.registry.dao.VenusServiceMappingDAO;
import com.meidusa.venus.registry.dao.CacheApplicationDAO;
import com.meidusa.venus.registry.dao.CacheServiceConfigDAO;
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
import com.meidusa.venus.registry.util.RegistryUtil;
import com.meidusa.venus.support.VenusConstants;

/**
 * Created by Zhangzhihua on 2017/8/16.
 */
public class MysqlRegisterService implements RegisterService, DisposableBean {

	private VenusServiceDAO venusServiceDAO;

	private VenusServiceConfigDAO venusServiceConfigDAO;

	private VenusApplicationDAO venusApplicationDAO;
	
	private CacheApplicationDAO cacheApplicationDAO;

	private VenusServerDAO venusServerDAO;

	private CacheVenusServerDAO cacheVenusServerDAO;

	private CacheVenusServiceDAO cacheVenusServiceDAO;
	
	private CacheServiceConfigDAO cacheServiceConfigDAO;

	private VenusServiceMappingDAO venusServiceMappingDAO;

	private TransactionTemplate transactionTemplate;

	private volatile boolean needRun = true;

	private ExecutorService es = Executors.newSingleThreadExecutor();

	private static final int QUEUE_SIZE_10000 = 10000;
	
	private int sampleMod = 10;
	
	private String enableLocalIp="on";//是否开启本机ip优先
	
	private String enableFilterIp="off"; //是否开启开发机器ip过滤
	
	private String envIpRange;

	public static final LinkedBlockingQueue<UpdateHeartBeatTimeDTO> HEARTBEAT_QUEUE = new LinkedBlockingQueue<UpdateHeartBeatTimeDTO>(
			QUEUE_SIZE_10000);

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
				venusApplicationDO.setNewApp(true);
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
			venusServiceDAO.updateService(url.getMethods(), false, serviceId, appId,url.getVersionRange());
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
				venusApplicationDO.setNewApp(false);
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
		if(CollectionUtils.isEmpty(services)){
			return;
		}
		for (Iterator<VenusServiceDO> iterator = services.iterator(); iterator.hasNext();) {
			VenusServiceDO service = iterator.next();
			if (null == service && url.isConsumerCheck()) {// 服务不存在并且配置了检测则抛出异常
				String name = log_service_name(url);
				LogUtils.ERROR_LOG.error("服务订阅异常,原因:服务{}不存在 ", name);
				throw new VenusRegisteException("服务订阅异常,原因:服务" + name + "不存在");
			}
			if (service.getIsDelete() && url.isConsumerCheck()) {// 服务不存在并且配置了检测则抛出异常
				String name = log_service_name(url);
				LogUtils.ERROR_LOG.error("服务订阅异常,原因:服务{}已删除", name);
				throw new VenusRegisteException("服务订阅异常,原因:服务" + name + "不存在");
			}
			String appCode = url.getApplication();
			int appId = 0;
			if (StringUtils.isNotBlank(appCode)) {
				VenusApplicationDO application=cacheApplicationDAO.getApplication(appCode);
				if (null == application) {
					application = venusApplicationDAO.getApplication(appCode);
				}
				if (null == application) {
					VenusApplicationDO venusApplicationDO = new VenusApplicationDO();
					venusApplicationDO.setAppCode(appCode);
					venusApplicationDO.setProvider(false);
					venusApplicationDO.setConsumer(true);
					venusApplicationDO.setUpdateName(RegisteConstant.CONSUMER);
					venusApplicationDO.setCreateName(RegisteConstant.CONSUMER);
					venusApplicationDO.setNewApp(true);
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
			
			VenusServerDO server =null;//cacheVenusServerDAO.getServer(url.getHost(), 0);
			if (null == server) {
				server = venusServerDAO.getServer(url.getHost(), 0);// 订阅server的端口为0
			}
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
		if(CollectionUtils.isNotEmpty(services)){
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
		}
		return false;
	}

	public List<VenusServiceDefinitionDO> findServiceDefinitions(URL url) {
		long start=System.currentTimeMillis();
		List<VenusServiceDefinitionDO> returnList = new ArrayList<VenusServiceDefinitionDO>();
		String interfaceName = url.getInterfaceName();
		String serviceName = url.getServiceName();
		String version = url.getVersion();
		List<VenusServiceDO> services = null;
		try {
			services = venusServiceDAO.queryServices(interfaceName, serviceName, version);// servicePath interfaceName/serviceName?version=version
		} catch (Exception e) {
			LogUtils.ERROR_LOG.error("findServiceDefinitions queryServices 调用异常,interfaceName=>"+interfaceName+",serviceName=>"+serviceName+",version=>"+version,e);
		}
		try{
			if(CollectionUtils.isNotEmpty(services)){
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
					List<VenusServerDO> servers = cacheVenusServerDAO.getServers(serverIds);
					if (CollectionUtils.isEmpty(servers)) {
						servers = venusServerDAO.getServers(serverIds);
					}
					if (CollectionUtils.isNotEmpty(servers)) {
						for (Iterator<VenusServerDO> iterator = servers.iterator(); iterator.hasNext();) {
							VenusServerDO venusServerDO = iterator.next();
							String hostPort = venusServerDO.getHostname() + ":" + venusServerDO.getPort();
							hostPortSet.add(hostPort);
						}
					}
				}
				// 本机->sit->其他;如果本机server ip发现 同subcribe ip相同，只返回本机ip
				Set<String> needHostPorts = new HashSet<String>();
				String msg="before=>"+JSON.toJSONString(hostPortSet);
				
				if(getEnableLocalIp().equals("on")){// 是否开启本地优先
					boolean filterIP = findLocalIP(hostPortSet,url.getHost());
					if(filterIP){//找到的是本机的
						needHostPorts = hostPortSet;
					}
				}
				
				if(getEnableFilterIp().equals("on")){// 是否开启同网络环境优先
					if (CollectionUtils.isEmpty(needHostPorts)){
						List<String> sitIpSegments = getSitIpSegments();//sit ip 网段列表
						LogUtils.DEFAULT_LOG.info("sitIpSegments=>{}",JSON.toJSONString(sitIpSegments));
						Set<String> afterFilterIps = filterSitIps(hostPortSet, sitIpSegments);//符合sit网段的IPs,从ucm读取网段，不在该网段中，排除 
						if(CollectionUtils.isNotEmpty(afterFilterIps)) {
							needHostPorts=afterFilterIps;
							LogUtils.DEFAULT_LOG.info(msg+",afterFilterIps=>{}",JSON.toJSONString(afterFilterIps));
						}
					}
				}
				
				if (CollectionUtils.isEmpty(needHostPorts)){
					needHostPorts=hostPortSet;
				}
				
				if (CollectionUtils.isNotEmpty(needHostPorts)) {
					VenusApplicationDO application = cacheApplicationDAO.getApplication(service.getAppId());
					if (null == application) {
						application = venusApplicationDAO.getApplication(service.getAppId());
					}
					VenusServiceDefinitionDO def = new VenusServiceDefinitionDO();
					def.setInterfaceName(interfaceName);
					def.setName(serviceName);
					def.setIpAddress(needHostPorts);
					LogUtils.DEFAULT_LOG.info("serviceName=>{},ipAddress=>{}",serviceName,JSON.toJSONString(needHostPorts));
					def.setActive(true);
					def.setDescription(service.getDescription());
					def.setVersion(service.getVersion());
					def.setVersionRange(service.getVersionRange());
					if (null != application) {
						def.setProvider(application.getAppCode());
					}
						if (cacheServiceConfigDAO.getVenusServiceConfigCount() > 0) {
							List<VenusServiceConfigDO> serviceConfigs = null;
							if (cacheServiceConfigDAO.isLoacCacheRunning()) {//缓存加载中，从数据库中查找,否则从当前缓存中查找
								serviceConfigs = venusServiceConfigDAO.getServiceConfigs(serviceId);
							}else{
								serviceConfigs = cacheServiceConfigDAO.getVenusServiceConfig(serviceId);
								//serviceConfigs = venusServiceConfigDAO.getServiceConfigs(serviceId);
							}
							ResultUtils.setServiceConfigs(serviceConfigs);
							def.setServiceConfigs(serviceConfigs);
						}
					returnList.add(def);
				}
			}
			}
		} catch (Exception e) {
			LogUtils.ERROR_LOG.error("findServiceDefinitions调用异常,url=>{},异常原因：{}", url, e);
			throw new VenusRegisteException("findServiceDefinitions调用异常,服务名：" + log_service_name(url), e);
		}
		long end = System.currentTimeMillis();
		long consumerTime = end - start;
		LogUtils.logSlow(consumerTime, "findServiceDefs is slow,url=>"+JSON.toJSONString(url));
		if(end % sampleMod ==1){
		LogUtils.LOAD_SERVICE_DEF_LOG.info("findServiceDefs sampling consumerTime=>{},url=>{}", consumerTime,
				JSON.toJSONString(url));
		}
		return returnList;
	}

	private Set<String> filterSitIps(Set<String> hostPortSet, List<String> sitIpSegments) {
		if (CollectionUtils.isNotEmpty(sitIpSegments)) {
			Set<String> set = new HashSet<String>();
			if (CollectionUtils.isNotEmpty(hostPortSet)) {
				for (String ip : hostPortSet) {
					for (String sitSegment : sitIpSegments) {
						if (ip.startsWith(sitSegment)) {
							set.add(ip);
							break;
						}
					}
				}
			}
			return set;
		} else {
			return hostPortSet;
		}
	}

	private List<String> getSitIpSegments() {
		List<String> sitIpSegments = new ArrayList<String>();
		if (RegistryUtil.isNotBlank(this.getEnvIpRange())) {
			String[] split = this.getEnvIpRange().split(",");
			int len = split.length;
			if (len > 0) {
				for (int i = 0; i < len; i++) {
					String ip = split[i];
					sitIpSegments.add(ip);
				}
			}
		}
		return sitIpSegments;
	}
	
	public static boolean findLocalIP(Set<String> hostPortSet, String localIp) {
		boolean hasFindLocalIp = false;
		if (RegistryUtil.isNotBlank(localIp)) {
			for (Iterator<String> iterator = hostPortSet.iterator(); iterator.hasNext();) {
				String str = iterator.next();
				if (str.startsWith(localIp)) {
					hasFindLocalIp = true;
					break;
				}
			}

			if (hasFindLocalIp) {
				for (Iterator<String> iterator = hostPortSet.iterator(); iterator.hasNext();) {
					String str = iterator.next();
					if (!str.startsWith(localIp)) {
						iterator.remove();
					}
				}
			}
		}
		return hasFindLocalIp;
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
			LogUtils.DEFAULT_LOG.info("not exits=>" + sb.toString());
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
				LogUtils.DEFAULT_LOG.info(
						"heartbeatSubcribe updateServiceMappingHeartBeatTime serverID=>{},role=>{},isSuccess=>{},currentDate=>{},url=>{}",
						serverID, RegisteConstant.CONSUMER, update, new Date(), url);
			}
		} catch (Exception e) {
			String name = log_service_name(url);
			LogUtils.ERROR_LOG.error("服务{}subscrible更新heartBeatTime异常 ,异常原因：{}", name, e);
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
				LogUtils.DEFAULT_LOG.info("heartbeatRegister serverID=>{},role=>{},isSuccess=>{},currentDate=>{},url=>{}", serverID,
						RegisteConstant.PROVIDER, update, new Date(), url);
			}
		} catch (Exception e) {
			String name = log_service_name(url);
			LogUtils.ERROR_LOG.error("服务{}registe更新heartBeatTime异常 ,异常原因：{}", name, e);
			throw new VenusRegisteException("registe更新heartBeatTime异常,服务名：" + name, e);
		}

	}

	private void heartbeatRegister(Set<URL> urls, String role) {
		if (CollectionUtils.isEmpty(urls)) {
			return;
		}
		Map<Integer, List<Integer>> maps = new HashMap<Integer, List<Integer>>();
		try {
			VenusServerDO server = getServer(urls);
			for (URL url : urls) {
				if (null != server) {
					List<VenusServiceDO> services = cacheVenusServiceDAO.queryServices(url.getInterfaceName(),
							url.getServiceName(), url.getVersion());
					if (CollectionUtils.isEmpty(services)) {
						services = venusServiceDAO.queryServices(url.getInterfaceName(), url.getServiceName(),
								url.getVersion());
					}
					if (CollectionUtils.isNotEmpty(services)) {
						for (Iterator<VenusServiceDO> iterator = services.iterator(); iterator.hasNext();) {
							VenusServiceDO venusServiceDO = iterator.next();
							List<Integer> list = maps.get(server.getId());
							Integer serviceId = venusServiceDO.getId();
							if (list != null) {
								if(!list.contains(serviceId)){
									list.add(serviceId);
								}
							} else {
								list = new ArrayList<>();
								if(!list.contains(serviceId)){
									list.add(serviceId);
								}
								maps.put(server.getId(), list);
							}
						}
					}
				}
			}
			if (HEARTBEAT_QUEUE.size() >= QUEUE_SIZE_10000 - 1) {
				LogUtils.HEARTBEAT_LOG.info("venus heartbeat drop message=>" + JSON.toJSONString(maps));
			} else {
				for (Map.Entry<Integer, List<Integer>> ent : maps.entrySet()) {
					UpdateHeartBeatTimeDTO heartBeatTimeDTO = new UpdateHeartBeatTimeDTO();
					heartBeatTimeDTO.setRole(role);
					heartBeatTimeDTO.setServerId(ent.getKey());
					heartBeatTimeDTO.setServiceIds(ent.getValue());
					heartBeatTimeDTO.setServerDO(server);
					boolean offer = HEARTBEAT_QUEUE.offer(heartBeatTimeDTO);
					if (!offer) {
						LogUtils.HEARTBEAT_LOG.info("heartbeat_queue size=>{},venus heartbeat message maps=>{},urls=>{}", HEARTBEAT_QUEUE.size(),JSON.toJSONString(maps),JSON.toJSONString(urls));
					}
				}

			}
		} catch (Exception e) {
			LogUtils.ERROR_LOG.error("服务{}heartBeatTime入队列异常 ,异常原因：{}", JSON.toJSONString(urls, true), e);
			throw new VenusRegisteException("heartBeatTime入队列异常", e);
		}

	}

	private VenusServerDO getServer(Set<URL> urls) {
		VenusServerDO server = null;
		for (URL url : urls) {
			String host = url.getHost();
			int port = url.getPort();

			server = cacheVenusServerDAO.getServer(host, port);
			if (null == server) {
				try{
					server = venusServerDAO.getServer(host, port);
				} catch (Exception e) {
					LogUtils.ERROR_LOG.error("根据host=>{},port=>{}查询server服务异常 ",host,port);
				}
			}
			if (null != server) {
				break;
			}
		}
		return server;
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
				LogUtils.CLEAR_INVALID.info(
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
				LogUtils.CLEAR_INVALID.info("@@@@@@currentDateTime=>{},delete_mapping_ids=>{},serviceMappings=>{}@@@@@@@",
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

	public CacheServiceConfigDAO getCacheServiceConfigDAO() {
		return cacheServiceConfigDAO;
	}

	public void setCacheServiceConfigDAO(CacheServiceConfigDAO cacheServiceConfigDAO) {
		this.cacheServiceConfigDAO = cacheServiceConfigDAO;
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
	
	public int getSampleMod() {
		return sampleMod;
	}

	public void setSampleMod(int sampleMod) {
		this.sampleMod = sampleMod;
	}

	public CacheApplicationDAO getCacheApplicationDAO() {
		return this.cacheApplicationDAO;
	}

	public void setCacheApplicationDAO(CacheApplicationDAO cacheApplicationDAO) {
		this.cacheApplicationDAO = cacheApplicationDAO;
	}

	public String getEnableLocalIp() {
		return enableLocalIp;
	}

	public void setEnableLocalIp(String enableLocalIp) {
		this.enableLocalIp = enableLocalIp;
	}

	public String getEnableFilterIp() {
		return enableFilterIp;
	}

	public void setEnableFilterIp(String enableFilterIp) {
		this.enableFilterIp = enableFilterIp;
	}
	
	public String getEnvIpRange() {
		return envIpRange;
	}

	public void setEnvIpRange(String envIpRange) {
		this.envIpRange = envIpRange;
	}

	public void updateServiceAppIds() {
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
								venusApplicationDO.setNewApp(false);
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
		for (Map.Entry<String, Set<URL>> ent : maps.entrySet()) {
			heartbeatRegister(ent.getValue(), ent.getKey());
		}
		long end = System.currentTimeMillis() - start;
		LogUtils.logSlow(end, "heartbeat maps=> " + JSON.toJSONString(maps));
	}

	private class UpdateHeartbeatTimeRunnable implements Runnable {

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
			while (needRun) {
				try {
					int startSize = HEARTBEAT_QUEUE.size();
					UpdateHeartBeatTimeDTO heartbeatDto = HEARTBEAT_QUEUE.poll();
					if (null != heartbeatDto) {
						int endSize = HEARTBEAT_QUEUE.size();
						long start = System.currentTimeMillis();
						boolean update = venusServiceMappingDAO.updateHeartBeatTime(heartbeatDto.getServerId(),
								heartbeatDto.getServiceIds(), heartbeatDto.getRole());
						long consumerTime = System.currentTimeMillis() - start;
						LogUtils.logSlow(consumerTime, "UpdateHeartbeatTimeRunnable run() ");
						if(!update){
							LogUtils.HEARTBEAT_LOG.info("UpdateHeartbeatTimeRunnable.poll startSize=>{},endSize=>{},update=>{},heartbeatDto=>{}", startSize, endSize,update,JSON.toJSONString(heartbeatDto));
						}
						if(start % sampleMod ==1){
							LogUtils.HEARTBEAT_LOG.info("UpdateHeartbeatTimeRunnable.sampling startSize=>{},endSize=>{},update=>{},consumerTime=>{},heartbeatDto=>{}", startSize, endSize,update,consumerTime,JSON.toJSONString(heartbeatDto));
						}
					}
				} catch (Throwable e) {
					LogUtils.ERROR_LOG.error("UpdateHeartbeatTimeRunnable consumer thread is error" + e.getMessage(), e);
				}
			}
		}
		/* heartbeat loaddef 只打外部请求的数据，错误和大于200毫秒的全打 */
		/* 正常的采样打 10个打一个  清理程序 和同步数据(按5000毫秒，别的按200毫秒) slow时间需更改 */


	}

	@Override
	public void destroy() throws Exception {
		needRun = false;
		es.shutdown();
	}

}
