package com.meidusa.venus.registry.service.impl;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import com.meidusa.venus.registry.dao.OldServiceMappingDAO;
import com.meidusa.venus.registry.dao.VenusServerDAO;
import com.meidusa.venus.registry.dao.VenusServiceDAO;
import com.meidusa.venus.registry.dao.VenusServiceMappingDAO;
import com.meidusa.venus.registry.dao.impl.DataSourceUtil;
import com.meidusa.venus.registry.dao.impl.OldServiceMappingDaoImpl;
import com.meidusa.venus.registry.dao.impl.VenusApplicationDaoImpl;
import com.meidusa.venus.registry.dao.impl.VenusServerDaoImpl;
import com.meidusa.venus.registry.dao.impl.VenusServiceConfigDaoImpl;
import com.meidusa.venus.registry.dao.impl.VenusServiceDaoImpl;
import com.meidusa.venus.registry.dao.impl.VenusServiceMappingDaoImpl;
import com.meidusa.venus.registry.data.move.OldServerDO;
import com.meidusa.venus.registry.data.move.OldServiceDO;
import com.meidusa.venus.registry.data.move.OldServiceMappingDO;
import com.meidusa.venus.registry.service.RegisterService;

public class OLdServiceMappingService {

	private static final int PAGE_SIZE_200 = 200;

	private static Logger logger = LoggerFactory.getLogger(OLdServiceMappingService.class);

	private OldServiceMappingDAO oldServiceMappingDAO;

	private JdbcTemplate oldJdbcTemplate;

	/** 原注册中心mysql连接地址 */
	private String oldConnectUrl;
	
	private RegisterService registerService;

	public OLdServiceMappingService() {

	}

	public OLdServiceMappingService(String oldConnectUrl, String connectUrl) {
		this.setOldConnectUrl(oldConnectUrl);
		init();
	}

	public void init() {
		if (StringUtils.isBlank(this.getOldConnectUrl())) {
			this.setOldConnectUrl("mysql://10.32.173.250:3306/registry?username=registry&password=registry");
		}
		String url = this.getOldConnectUrl();
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
		BasicDataSource dataSource = DataSourceUtil.getBasicDataSource(url);
		if (oldJdbcTemplate == null) {
			synchronized (OLdServiceMappingService.class) {
				if (oldJdbcTemplate == null) {
					oldJdbcTemplate = new JdbcTemplate(dataSource);
				}
				this.setOldServiceMappingDAO(new OldServiceMappingDaoImpl(oldJdbcTemplate));
			}
		}
		
	}

	public void moveServiceMappings() {
		Integer totalCount = oldServiceMappingDAO.getOldServiceMappingCount();
		if (null != totalCount && totalCount > 0) {
			int mod = totalCount % PAGE_SIZE_200;
			int count = totalCount / PAGE_SIZE_200;
			if (mod > 0) {
				count = count + 1;
			}
			Integer mapId = null;
			for (int i = 0; i < count; i++) {
				List<OldServiceMappingDO> oldServiceMappings = oldServiceMappingDAO
						.queryOldServiceMappings(PAGE_SIZE_200, mapId);
				if (CollectionUtils.isNotEmpty(oldServiceMappings)) {
					mapId = oldServiceMappings.get(oldServiceMappings.size() - 1).getMapId();
					for (OldServiceMappingDO oldServiceMappingDO : oldServiceMappings) {
						System.out.println("mapId=>" + oldServiceMappingDO.getMapId());
						// TODO 在此更新 新的数据库等操作
					}
				}
			}
		}
	}

	public void moveServices() {
		Integer totalCount = oldServiceMappingDAO.getOldServiceCount();
		if (null != totalCount && totalCount > 0) {
			int mod = totalCount % PAGE_SIZE_200;
			int count = totalCount / PAGE_SIZE_200;
			if (mod > 0) {
				count = count + 1;
			}
			Integer mapId = null;
			for (int i = 0; i < count; i++) {
				List<OldServiceDO> services = oldServiceMappingDAO.queryOldServices(PAGE_SIZE_200, mapId);
				if (CollectionUtils.isNotEmpty(services)) {
					mapId = services.get(services.size() - 1).getId();
					for (OldServiceDO oldServiceDO : services) {
						System.out.println("id=>" + oldServiceDO.getId());
						// TODO 在此更新 新的数据库等操作
						//registerService.addService(oldServiceDO.getServiceName(), oldServiceDO.getDescription());
					}
				}
			}
		}
	}

	public void moveServers() {
		Integer totalCount = oldServiceMappingDAO.getOldServerCount();
		if (null != totalCount && totalCount > 0) {
			int mod = totalCount % PAGE_SIZE_200;
			int count = totalCount / PAGE_SIZE_200;
			if (mod > 0) {
				count = count + 1;
			}
			Integer id = null;
			for (int i = 0; i < count; i++) {
				List<OldServerDO> servers = oldServiceMappingDAO.queryOldServers(PAGE_SIZE_200, id);
				if (CollectionUtils.isNotEmpty(servers)) {
					id = servers.get(servers.size() - 1).getId();
					for (OldServerDO oldServerDO : servers) {
						System.out.println("id=>" + oldServerDO.getId());
						//registerService.addServer(oldServerDO.getHostName(), oldServerDO.getPort());
					}
				}
			}
		}
	}

	public JdbcTemplate getOldJdbcTemplate() {
		return oldJdbcTemplate;
	}

	public void setOldJdbcTemplate(JdbcTemplate oldJdbcTemplate) {
		this.oldJdbcTemplate = oldJdbcTemplate;
	}

	public String getOldConnectUrl() {
		return oldConnectUrl;
	}

	public void setOldConnectUrl(String oldConnectUrl) {
		this.oldConnectUrl = oldConnectUrl;
	}

	public OldServiceMappingDAO getOldServiceMappingDAO() {
		return oldServiceMappingDAO;
	}

	public void setOldServiceMappingDAO(OldServiceMappingDAO oldServiceMappingDAO) {
		this.oldServiceMappingDAO = oldServiceMappingDAO;
	}

	public RegisterService getRegisterService() {
		return registerService;
	}

	public void setRegisterService(RegisterService registerService) {
		this.registerService = registerService;
	}

	public static void main(String args[]) {
		OLdServiceMappingService oms = new OLdServiceMappingService();
		oms.init();
		oms.moveServers();
	}
}
