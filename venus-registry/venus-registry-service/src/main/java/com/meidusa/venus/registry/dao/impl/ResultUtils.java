package com.meidusa.venus.registry.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import com.meidusa.fastjson.JSON;

import org.apache.commons.collections.CollectionUtils;

import com.meidusa.venus.registry.domain.FlowControl;
import com.meidusa.venus.registry.domain.MockConfig;
import com.meidusa.venus.registry.domain.RouterRule;
import com.meidusa.venus.registry.domain.VenusApplicationDO;
import com.meidusa.venus.registry.domain.VenusServerDO;
import com.meidusa.venus.registry.domain.VenusServiceConfigDO;
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

	public final static VenusServiceConfigDO resultToVenusServiceConfigDO(ResultSet rs) throws SQLException {
		VenusServiceConfigDO serviceConfig = new VenusServiceConfigDO();
		serviceConfig.setId(rs.getInt("id"));
		serviceConfig.setType(rs.getInt("type"));
		serviceConfig.setConfig(rs.getString("config"));

		serviceConfig.setServiceId(rs.getInt("service_id"));
		serviceConfig.setCreateName(rs.getString("create_name"));
		serviceConfig.setUpdateName(rs.getString("update_name"));

		serviceConfig.setCreateTime(rs.getTimestamp("create_time"));
		serviceConfig.setUpdateTime(rs.getTimestamp("update_time"));
		return serviceConfig;
	}

	public final static VenusServiceDO resultToVenusServiceDO(ResultSet rs) throws SQLException {
		VenusServiceDO venusService = new VenusServiceDO();
		venusService.setId(rs.getInt("id"));
		venusService.setName(rs.getString("name"));
		venusService.setInterfaceName(rs.getString("interface_name"));

		venusService.setVersion(rs.getString("version"));
		venusService.setDescription(rs.getString("description"));
		venusService.setAppId(rs.getInt("app_id"));
		venusService.setRegisteType(rs.getInt("registe_type"));

		venusService.setMethods(rs.getString("methods"));
		venusService.setIsDelete(rs.getBoolean("is_delete"));

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
		venusServiceMapping.setProviderAppId(rs.getInt("provider_app_id"));
		venusServiceMapping.setConsumerAppId(rs.getInt("consumer_app_id"));
		venusServiceMapping.setIsDelete(rs.getBoolean("is_delete"));

		venusServiceMapping.setCreateTime(rs.getTimestamp("create_time"));
		venusServiceMapping.setUpdateTime(rs.getTimestamp("update_time"));
		venusServiceMapping.setRegisteTime(rs.getTimestamp("registe_time"));
		return venusServiceMapping;
	}

	public final static VenusApplicationDO resultToVenusApplicationDO(ResultSet rs) throws SQLException {
		VenusApplicationDO venusServiceMapping = new VenusApplicationDO();
		venusServiceMapping.setId(rs.getInt("id"));
		venusServiceMapping.setAppCode(rs.getString("app_code"));
		venusServiceMapping.setProvider(rs.getBoolean("provider"));

		venusServiceMapping.setConsumer(rs.getBoolean("consumer"));
		venusServiceMapping.setCreateName(rs.getString("create_name"));
		venusServiceMapping.setUpdateName(rs.getString("update_name"));

		venusServiceMapping.setCreateTime(rs.getTimestamp("create_time"));
		venusServiceMapping.setUpdateTime(rs.getTimestamp("update_time"));
		return venusServiceMapping;
	}

	public final static void setServiceConfigs(List<VenusServiceConfigDO> serviceConfigs) {
		if (CollectionUtils.isNotEmpty(serviceConfigs)) {
			for (Iterator<VenusServiceConfigDO> iterator = serviceConfigs.iterator(); iterator.hasNext();) {
				VenusServiceConfigDO c = iterator.next();
				int type = c.getType();
				// 1- 路由规则，2-流控配置，3-降级配置
				if (type == 1) {
					RouterRule routerRule = JSON.parseObject(c.getConfig(), RouterRule.class);
					c.setRouterRule(routerRule);
				}
				if (type == 2) {
					FlowControl flowControl = JSON.parseObject(c.getConfig(), FlowControl.class);
					c.setFlowControl(flowControl);
				}
				if (type == 3) {
					MockConfig mockConfig = JSON.parseObject(c.getConfig(), MockConfig.class);
					c.setMockConfig(mockConfig);
				}
			}
		}
	}

}
