package com.meidusa.venus.registry.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.meidusa.venus.registry.domain.VenusApplicationDO;
import com.meidusa.venus.registry.domain.VenusServerDO;
import com.meidusa.venus.registry.domain.VenusServiceDO;
import com.meidusa.venus.registry.domain.VenusServiceMappingDO;

public class ResultUtils {

	public final static VenusServerDO resultToVenusServerDO(ResultSet rs) throws SQLException {
		VenusServerDO venusService = new VenusServerDO();
		venusService.setId(rs.getInt("id"));
		venusService.setHostname(rs.getString("hostname"));
		venusService.setPort(rs.getInt("port"));
		venusService.setCreateTime(rs.getTimestamp("create_time"));
		venusService.setUpdateTime(rs.getTimestamp("update_time"));
		return venusService;
	}

	public final static VenusServiceDO resultToVenusServiceDO(ResultSet rs) throws SQLException {
		VenusServiceDO venusService = new VenusServiceDO();
		venusService.setId(rs.getInt("id"));
		venusService.setName(rs.getString("name"));
		venusService.setInterfaceName(rs.getString("interface_name"));
		venusService.setVersion(rs.getString("version"));
		venusService.setDescription(rs.getString("description"));
		venusService.setCreateTime(rs.getTimestamp("create_time"));
		venusService.setUpdateTime(rs.getTimestamp("update_time"));
		return venusService;
	}

	public final static VenusServiceMappingDO resultToVenusServiceMappingDO(ResultSet rs) throws SQLException {
		VenusServiceMappingDO venusServiceMapping = new VenusServiceMappingDO();
		venusServiceMapping.setId(rs.getInt("id"));
		venusServiceMapping.setVersion(rs.getString("version"));
		venusServiceMapping.setActive(rs.getBoolean("active"));
		venusServiceMapping.setSync(rs.getBoolean("sync"));
		
		venusServiceMapping.setServerId(rs.getInt("server_id"));
		venusServiceMapping.setServiceId(rs.getInt("service_id"));
		
		venusServiceMapping.setCreateTime(rs.getTimestamp("create_time"));
		venusServiceMapping.setUpdateTime(rs.getTimestamp("update_time"));
		return venusServiceMapping;
	}
	
	public final static VenusApplicationDO resultToVenusApplicationDO(ResultSet rs) throws SQLException {
		VenusApplicationDO venusServiceMapping = new VenusApplicationDO();
		venusServiceMapping.setId(rs.getInt("id"));
		venusServiceMapping.setAppCode(rs.getString("app_code"));
		venusServiceMapping.setCreateName(rs.getString("create_name"));
		venusServiceMapping.setUpdateName(rs.getString("update_name"));

		venusServiceMapping.setCreateTime(rs.getTimestamp("create_time"));
		venusServiceMapping.setUpdateTime(rs.getTimestamp("update_time"));
		return venusServiceMapping;
	}

}
