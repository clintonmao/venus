package com.meidusa.venus.registry.service.impl;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import com.meidusa.venus.registry.dao.OldServiceMappingDAO;
import com.meidusa.venus.registry.dao.impl.DataSourceUtil;
import com.meidusa.venus.registry.dao.impl.OldServiceMappingDaoImpl;
import com.meidusa.venus.registry.data.move.OldServerDO;
import com.meidusa.venus.registry.data.move.OldServiceDO;
import com.meidusa.venus.registry.data.move.OldServiceMappingDO;

public class OLdServiceMappingService {

	private static final int PAGE_SIZE_200 = 200;

	private static Logger logger = LoggerFactory.getLogger(OLdServiceMappingService.class);

	private OldServiceMappingDAO oldServiceMappingDAO;

	private BasicDataSource dataSource;

	private JdbcTemplate jdbcTemplate;

	private String connectUrl;

	public OLdServiceMappingService() {

	}

	public OLdServiceMappingService(String connectUrl) {
		this.setConnectUrl(connectUrl);
		init();
	}

	public void init() {
		if (StringUtils.isBlank(this.getConnectUrl())) {
			this.setConnectUrl("mysql://10.32.173.250:3306/registry?username=registry&password=registry");
		}
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
				this.setOldServiceMappingDAO(new OldServiceMappingDaoImpl(jdbcTemplate));
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
						// TODO 在此更新 新的数据库等操作
					}
				}
			}
		}
	}

	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public String getConnectUrl() {
		return connectUrl;
	}

	public void setConnectUrl(String connectUrl) {
		this.connectUrl = connectUrl;
	}

	public OldServiceMappingDAO getOldServiceMappingDAO() {
		return oldServiceMappingDAO;
	}

	public void setOldServiceMappingDAO(OldServiceMappingDAO oldServiceMappingDAO) {
		this.oldServiceMappingDAO = oldServiceMappingDAO;
	}

	public static void main(String args[]) {
		OLdServiceMappingService oms = new OLdServiceMappingService();
		oms.init();
		oms.moveServers();
	}
}
