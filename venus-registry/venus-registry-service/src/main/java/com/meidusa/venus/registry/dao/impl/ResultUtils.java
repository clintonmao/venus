package com.meidusa.venus.registry.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import com.meidusa.fastjson.JSON;

import org.apache.commons.collections.CollectionUtils;

import com.meidusa.venus.registry.data.move.OldServerDO;
import com.meidusa.venus.registry.data.move.OldServiceDO;
import com.meidusa.venus.registry.data.move.OldServiceMappingDO;
import com.meidusa.venus.registry.data.move.ServiceMappingDTO;
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
		VenusServerDO venusServer = new VenusServerDO();
		venusServer.setId(rs.getInt("id"));
		venusServer.setHostname(rs.getString("hostname"));
		venusServer.setPort(rs.getInt("port"));

		venusServer.setCreateTime(rs.getTimestamp("create_time"));
		venusServer.setUpdateTime(rs.getTimestamp("update_time"));
		return venusServer;
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
		venusService.setVersionRange(rs.getString("version_range"));
		
		venusService.setDescription(rs.getString("description"));
		venusService.setAppId(rs.getInt("app_id"));
		venusService.setRegisteType(rs.getInt("registe_type"));

		venusService.setMethods(rs.getString("methods"));
		venusService.setDelete(rs.getBoolean("is_delete"));

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
		venusServiceMapping.setHeartbeatTime(rs.getTimestamp("heartbeat_time"));
		return venusServiceMapping;
	}

	public final static VenusApplicationDO resultToVenusApplicationDO(ResultSet rs) throws SQLException {
		VenusApplicationDO application = new VenusApplicationDO();
		application.setId(rs.getInt("id"));
		application.setAppCode(rs.getString("app_code"));
		application.setProvider(rs.getBoolean("provider"));

		application.setConsumer(rs.getBoolean("consumer"));
		application.setCreateName(rs.getString("create_name"));
		application.setUpdateName(rs.getString("update_name"));

		application.setCreateTime(rs.getTimestamp("create_time"));
		application.setUpdateTime(rs.getTimestamp("update_time"));
		return application;
	}

	public final static void setServiceConfigs(List<VenusServiceConfigDO> serviceConfigs) {
		if (CollectionUtils.isNotEmpty(serviceConfigs)) {
			for (Iterator<VenusServiceConfigDO> iterator = serviceConfigs.iterator(); iterator.hasNext();) {
				VenusServiceConfigDO serviceConfig = iterator.next();
				int type = serviceConfig.getType();
				// 1- 路由规则，2-流控配置，3-降级配置
				if (type == 1) {
					RouterRule routerRule = JSON.parseObject(serviceConfig.getConfig(), RouterRule.class);
					serviceConfig.setRouterRule(routerRule);
				}
				if (type == 2) {
					FlowControl flowControl = JSON.parseObject(serviceConfig.getConfig(), FlowControl.class);
					serviceConfig.setFlowControl(flowControl);
				}
				if (type == 3) {
					MockConfig mockConfig = JSON.parseObject(serviceConfig.getConfig(), MockConfig.class);
					serviceConfig.setMockConfig(mockConfig);
				}
			}
		}
	}

	public final static OldServiceMappingDO rsToOldServiceMappingDO(ResultSet rs) throws SQLException {
		OldServiceMappingDO oldServiceMapping = new OldServiceMappingDO();
		oldServiceMapping.setActive(rs.getBoolean("active"));
		oldServiceMapping.setCreateTime(rs.getTimestamp("create_time"));
		oldServiceMapping.setDescription(rs.getString("description"));
		
		oldServiceMapping.setHostName(rs.getString("host_name"));
		oldServiceMapping.setMapId(rs.getInt("map_id"));
		oldServiceMapping.setPort(rs.getInt("port"));
		
		oldServiceMapping.setServerId(rs.getInt("server_id"));
		oldServiceMapping.setServiceId(rs.getInt("service_id"));
		oldServiceMapping.setServiceName(rs.getString("service_name"));
		
		oldServiceMapping.setSync(rs.getBoolean("sync"));
		oldServiceMapping.setUpdateTime(rs.getTimestamp("update_time"));
		oldServiceMapping.setVersion(rs.getString("version"));
		
		return oldServiceMapping;
	}
	
	public final static ServiceMappingDTO rsTransServiceMappingDTO(ResultSet rs) throws SQLException {
		ServiceMappingDTO oldServiceMapping = new ServiceMappingDTO();

		oldServiceMapping.setMapId(rs.getInt("map_id"));
		
		oldServiceMapping.setVersionRange(rs.getString("version"));
		oldServiceMapping.setServerId(rs.getInt("server_id"));
		oldServiceMapping.setHostName(rs.getString("host_name"));
		oldServiceMapping.setPort(rs.getInt("port"));
		
		oldServiceMapping.setServiceId(rs.getInt("service_id"));
		oldServiceMapping.setServiceName(rs.getString("service_name"));
		return oldServiceMapping;
	}

	public final static OldServiceDO rsToOldServiceDO(ResultSet rs) throws SQLException {
		OldServiceDO oldService = new OldServiceDO();
		oldService.setId(rs.getInt("id"));
		oldService.setServiceName(rs.getString("service_name"));
		oldService.setDescription(rs.getString("description"));
		return oldService;
	}

	public final static OldServerDO rsToOldServerDO(ResultSet rs) throws SQLException {
		OldServerDO oldServer = new OldServerDO();
		oldServer.setId(rs.getInt("id"));
		oldServer.setHostName(rs.getString("hostname"));
		oldServer.setPort(rs.getInt("port"));
		return oldServer;
	}

}
