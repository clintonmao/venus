package com.meidusa.venus.registry.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.dao.VenusServiceMappingDAO;
import com.meidusa.venus.registry.data.move.ServiceMappingDTO;
import com.meidusa.venus.registry.domain.RegisteConstant;
import com.meidusa.venus.registry.domain.VenusServiceDO;
import com.meidusa.venus.registry.domain.VenusServiceMappingDO;

public class VenusServiceMappingDaoImpl implements VenusServiceMappingDAO {

	private static final String SELECT_FIELDS_TABLE = "select id, server_id, service_id, version, active, sync,role,provider_app_id,consumer_app_id,is_delete,create_time, update_time,registe_time,heartbeat_time from t_venus_service_mapping ";

	private JdbcTemplate jdbcTemplate;

	public VenusServiceMappingDaoImpl(JdbcTemplate jdbcTemplate) {
		super();
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public boolean addServiceMapping(VenusServiceMappingDO mapping) throws DAOException {
		String sql = "insert into t_venus_service_mapping (server_id,service_id,provider_app_id,consumer_app_id,version, active, sync,role,is_delete,create_time, update_time,registe_time,heartbeat_time) values (?, ?, ?, ?, ?, ?, ?, ?, ?,now(), now(),now(),now())";
		int update = 0;
		try {
			update = this.jdbcTemplate.update(sql, mapping.getServerId(), mapping.getServiceId(),
					mapping.getProviderAppId(), mapping.getConsumerAppId(), mapping.getVersion(), mapping.isActive(),
					mapping.isSync(), mapping.getRole(), mapping.getIsDelete());
		} catch (Exception e) {
			throw new DAOException("保存服务映射关系异常", e);
		}
		return update > 0;
	}

	@Override
	public boolean updateSubcribeServiceMapping(int id,int consumerAppId, boolean active, boolean isDelete) throws DAOException {
		String sql = "update t_venus_service_mapping set active = ?,is_delete=?,update_time=now(),registe_time=now(),heartbeat_time=now()";
		if (consumerAppId > 0) {
			sql = sql + ",consumer_app_id=? ";
		}
		sql = sql + " where id = ? ";
		int update = 0;
		try {
			if (consumerAppId > 0) {
				update = this.jdbcTemplate.update(sql, active, isDelete,consumerAppId,id);
			}else{
				update = this.jdbcTemplate.update(sql, active, isDelete, id);
			}
		} catch (Exception e) {
			throw new DAOException("更新映射关系异常", e);
		}
		return update > 0;
	}
	
	public boolean updateProviderServiceMapping(int id, boolean active, boolean isDelete,int providerAppId) throws DAOException {
		String sql = "update t_venus_service_mapping set active = ?,is_delete=?,update_time=now(),registe_time=now(),heartbeat_time=now()"
				;
		if (providerAppId > 0) {
			sql = sql + ",provider_app_id=? ";
		}
		sql = sql + " where id = ?";
		int update = 0;
		try {
			if (providerAppId > 0) {
				update = this.jdbcTemplate.update(sql, active, isDelete, providerAppId,id);
			}else{
				update = this.jdbcTemplate.update(sql, active, isDelete, id);
			}
		} catch (Exception e) {
			throw new DAOException("更新映射关系异常", e);
		}
		return update > 0;
	}

	@Override
	public boolean updateHeartBeatTime(int serverId, String role)
			throws DAOException {
		String sql = "update t_venus_service_mapping set is_delete=0,heartbeat_time = now() where server_id = ? and role=? ";
		int update = 0;
		try {
			update = this.jdbcTemplate.update(sql, serverId, role);
		} catch (Exception e) {
			throw new DAOException("更新映射关系heartbeat_time时间异常", e);
		}
		return update > 0;
	}

	@Override
	public boolean deleteServiceMapping(int id) throws DAOException {
		String sql = "delete from t_venus_service_mapping where id = ? ";
		int update = 0;
		try {
			update = this.jdbcTemplate.update(sql, id);
		} catch (Exception e) {
			throw new DAOException("更新映射关系异常", e);
		}
		return update > 0;
	}

	@Override
	public VenusServiceMappingDO getServiceMapping(Integer serverId, Integer serviceId, String role)
			throws DAOException {
		String sql = SELECT_FIELDS_TABLE + " where server_id = ? and service_id = ? and role=?";

		try {
			return this.jdbcTemplate.query(sql, new Object[] { serverId, serviceId, role },
					new ResultSetExtractor<VenusServiceMappingDO>() {
						@Override
						public VenusServiceMappingDO extractData(ResultSet rs)
								throws SQLException, DataAccessException {
							if (rs.next()) {
								return ResultUtils.resultToVenusServiceMappingDO(rs);
							}
							return null;
						}
					});
		} catch (Exception e) {
			throw new DAOException("根据serverId=>" + serverId + ",serviceId=>" + serviceId + "获取服务映射关系异常", e);
		}
	}

	@Override
	public List<VenusServiceMappingDO> getServiceMapping(Integer serviceId, String role, boolean isDelete)
			throws DAOException {
		String sql = SELECT_FIELDS_TABLE + " where service_id = ? and role=? and is_delete=?";

		try {
			return this.jdbcTemplate.query(sql, new Object[] { serviceId, role, isDelete },
					new ResultSetExtractor<List<VenusServiceMappingDO>>() {
						@Override
						public List<VenusServiceMappingDO> extractData(ResultSet rs)
								throws SQLException, DataAccessException {
							List<VenusServiceMappingDO> returnList = new ArrayList<VenusServiceMappingDO>();
							while (rs.next()) {
								returnList.add(ResultUtils.resultToVenusServiceMappingDO(rs));
							}
							return returnList;
						}
					});
		} catch (Exception e) {
			throw new DAOException("根据serviceId=>" + serviceId + "获取服务映射关系异常", e);
		}
	}

	@Override
	public VenusServiceMappingDO getServiceMapping(Integer id) throws DAOException {
		String sql = SELECT_FIELDS_TABLE + " where id = ?";

		try {
			return this.jdbcTemplate.query(sql, new Object[] { id }, new ResultSetExtractor<VenusServiceMappingDO>() {
				@Override
				public VenusServiceMappingDO extractData(ResultSet rs) throws SQLException, DataAccessException {
					if (rs.next()) {
						return ResultUtils.resultToVenusServiceMappingDO(rs);
					}
					return null;
				}
			});
		} catch (Exception e) {
			throw new DAOException("根据ID＝>" + id + "获取服务映射关系异常", e);
		}
	}

	@Override
	public List<VenusServiceMappingDO> getServiceMappings(Integer serverId) throws DAOException {
		String sql = SELECT_FIELDS_TABLE + " where server_id = ?";

		try {
			return this.jdbcTemplate.query(sql, new Object[] { serverId },
					new ResultSetExtractor<List<VenusServiceMappingDO>>() {
						@Override
						public List<VenusServiceMappingDO> extractData(ResultSet rs)
								throws SQLException, DataAccessException {
							List<VenusServiceMappingDO> returnList = new ArrayList<VenusServiceMappingDO>();
							while (rs.next()) {
								VenusServiceMappingDO mapping = ResultUtils.resultToVenusServiceMappingDO(rs);
								returnList.add(mapping);
							}
							return returnList;
						}
					});
		} catch (Exception e) {
			throw new DAOException("根据serverID＝>" + serverId + "获取服务映射关系异常", e);
		}
	}

	public List<VenusServiceMappingDO> getServiceMappings(String dateStr,int second) throws DAOException {
		String sql="select m.id, m.server_id, m.service_id, m.version, m.active, m.sync,m.role,m.provider_app_id,m.consumer_app_id,m.is_delete,m.create_time, m.update_time,m.registe_time,m.heartbeat_time from t_venus_service_mapping as m "
				+ "left join t_venus_service as v on m.service_id=v.id where m.has_heartbeat=1 and m.is_delete=0 and m.heartbeat_time <= subdate(now(),interval "+second+" second) ";
		
		try {
			return this.jdbcTemplate.query(sql, new Object[] {},
					new ResultSetExtractor<List<VenusServiceMappingDO>>() {
				@Override
				public List<VenusServiceMappingDO> extractData(ResultSet rs)
						throws SQLException, DataAccessException {
					List<VenusServiceMappingDO> returnList = new ArrayList<VenusServiceMappingDO>();
					while (rs.next()) {
						VenusServiceMappingDO mapping = ResultUtils.resultToVenusServiceMappingDO(rs);
						returnList.add(mapping);
					}
					return returnList;
				}
			});
		} catch (Exception e) {
			throw new DAOException("根据heartbeat_time小于等于当前时间减去" + second + "秒获取服务映射关系列表异常", e);
		}
	}
	
	public List<VenusServiceMappingDO> queryServiceMappings(int hour) throws DAOException {
		String sql="select m.id, m.server_id, m.service_id, m.version, m.active, m.sync,m.role,m.provider_app_id,m.consumer_app_id,m.is_delete,m.create_time, m.update_time,m.registe_time,m.heartbeat_time from t_venus_service_mapping as m "
				+ "left join t_venus_service as v on m.service_id=v.id where m.has_heartbeat=1 and m.is_delete=1 and m.heartbeat_time <= subdate(now(),interval "+hour+" hour) ";

		try {
			return this.jdbcTemplate.query(sql, new Object[] {},
					new ResultSetExtractor<List<VenusServiceMappingDO>>() {
						@Override
						public List<VenusServiceMappingDO> extractData(ResultSet rs)
								throws SQLException, DataAccessException {
							List<VenusServiceMappingDO> returnList = new ArrayList<VenusServiceMappingDO>();
							while (rs.next()) {
								VenusServiceMappingDO mapping = ResultUtils.resultToVenusServiceMappingDO(rs);
								returnList.add(mapping);
							}
							return returnList;
						}
					});
		} catch (Exception e) {
			throw new DAOException("根据heartbeat_time小于等于当前时间减去" + hour + "小时获取服务映射关系列表异常", e);
		}
	}

	public List<VenusServiceMappingDO> getDeleteServiceMappings(String updateTime, String role, boolean isDelete)
			throws DAOException {
		String sql = SELECT_FIELDS_TABLE + " where update_time >= ? and role=? and is_delete=?";

		try {
			return this.jdbcTemplate.query(sql, new Object[] { updateTime, role, isDelete },
					new ResultSetExtractor<List<VenusServiceMappingDO>>() {
						@Override
						public List<VenusServiceMappingDO> extractData(ResultSet rs)
								throws SQLException, DataAccessException {
							List<VenusServiceMappingDO> returnList = new ArrayList<VenusServiceMappingDO>();
							while (rs.next()) {
								VenusServiceMappingDO mapping = ResultUtils.resultToVenusServiceMappingDO(rs);
								returnList.add(mapping);
							}
							return returnList;
						}
					});
		} catch (Exception e) {
			throw new DAOException("根据大于等于update_time＝>" + updateTime + "获取服务映射关系列表异常", e);
		}
	}

	public List<VenusServiceMappingDO> getServiceMappings(int serviceId) throws DAOException {
		String sql = SELECT_FIELDS_TABLE + " where service_id = ?";

		try {
			return this.jdbcTemplate.query(sql, new Object[] { serviceId },
					new ResultSetExtractor<List<VenusServiceMappingDO>>() {
						@Override
						public List<VenusServiceMappingDO> extractData(ResultSet rs)
								throws SQLException, DataAccessException {
							List<VenusServiceMappingDO> returnList = new ArrayList<VenusServiceMappingDO>();
							while (rs.next()) {
								VenusServiceMappingDO mapping = ResultUtils.resultToVenusServiceMappingDO(rs);
								returnList.add(mapping);
							}
							return returnList;
						}
					});
		} catch (Exception e) {
			throw new DAOException("根据等于serviceId＝>" + serviceId + "获取服务映射关系列表异常", e);
		}
	}

	public boolean updateServiceMappings(List<Integer> ids) throws DAOException {
		if (ids.isEmpty()) {
			return false;
		}
		StringBuilder sb = new StringBuilder();
		for (Integer id : ids) {
			sb.append(id);
			sb.append(",");
		}
		String str = sb.substring(0, sb.length() - 1);
		String sql = "update t_venus_service_mapping set is_delete=?,update_time=now() where id in(" + str + ")";
		int update = 0;
		try {
			update = this.jdbcTemplate.update(sql, true);
		} catch (Exception e) {
			throw new DAOException("逻辑删除更新映射关系异常", e);
		}
		return update > 0;
	}
	
	public boolean deleteServiceMappings(List<Integer> ids) throws DAOException {
		if (ids.isEmpty()) {
			return false;
		}
		StringBuilder sb = new StringBuilder();
		for (Integer id : ids) {
			sb.append(id);
			sb.append(",");
		}
		String str = sb.substring(0, sb.length() - 1);
		String sql = "delete from t_venus_service_mapping where id in(" + str + ")";
		int update = 0;
		try {
			update = this.jdbcTemplate.update(sql);
		} catch (Exception e) {
			throw new DAOException("逻辑删除更新映射关系异常,sql=>"+sql, e);
		}
		return update > 0;
	}
	
	public boolean logicDeleteServiceMappings(List<Integer> ids) throws DAOException {
		if (ids.isEmpty()) {
			return false;
		}
		StringBuilder sb = new StringBuilder();
		for (Integer id : ids) {
			sb.append(id);
			sb.append(",");
		}
		String str = sb.substring(0, sb.length() - 1);
		String sql = "update t_venus_service_mapping set is_delete=1 where id in(" + str + ")";
		int update = 0;
		try {
			update = this.jdbcTemplate.update(sql);
		} catch (Exception e) {
			throw new DAOException("逻辑删除更新映射关系异常", e);
		}
		return update > 0;
	}
	
	public boolean existServiceMapping(String hostName, int port, String serviceName, String version)
			throws DAOException {
		String sql = "SELECT count(map.id) as records FROM t_venus_service_mapping as map "
				+ "left join t_venus_server as s "
				+ "on map.server_id=s.id left join t_venus_service as v on v.id=map.service_id " + " where s.hostname='"
				+ hostName + "' and s.port=" + port + " and v.name='" + serviceName + "' and map.role='provider'";
		if (StringUtils.isBlank(version) || "null".equalsIgnoreCase(version)) {
			sql = sql + " and map.version is null ";
		}else{
			sql = sql + " and map.version='" + version + "' ";
		}
		try {
			return this.jdbcTemplate.queryForObject(sql, Integer.class) > 0;
		} catch (Exception e) {
			throw new DAOException(
					"根据等于serviceName＝>" + serviceName + ",hostName=>" + hostName + ",port=>" + port + "获取服务映射关系个数异常",
					e);
		}
	}
	
	public int getMappingCountByServerId(int serverId) throws DAOException {
		String sql = "SELECT count(map.id) as records FROM t_venus_service_mapping as map where map.server_id="
				+ serverId;
		try {
			return this.jdbcTemplate.queryForObject(sql, Integer.class);
		} catch (Exception e) {
			throw new DAOException("根据serverId＝>" + serverId + ",获取服务映射关系个数异常", e);
		}
	}

	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public boolean updateHeartBeatTime(int serverId, List<Integer> serviceIds, String role) throws DAOException {
		if (serviceIds.isEmpty()) {
			return false;
		}
		StringBuilder sb = new StringBuilder();
		for (Integer id : serviceIds) {
			sb.append(id);
			sb.append(",");
		}
		String str = sb.substring(0, sb.length() - 1);
		String sql = "update t_venus_service_mapping set is_delete=0,has_heartbeat=1,heartbeat_time = now() where server_id = ? and role=? and service_id in(" + str + ")";
		int update = 0;
		try {
			update = this.jdbcTemplate.update(sql,serverId, role);
		} catch (Exception e) {
			throw new DAOException("更新映射关系心跳时间异常", e);
		}
		return update > 0;
	}
	
	public List<ServiceMappingDTO> queryServiceMappings(List<String> serviceNames) throws DAOException {
		StringBuilder sb = new StringBuilder();
		for (String name : serviceNames) {
			sb.append("'");
			sb.append(name);
			sb.append("'");
			sb.append(",");
		}
		String nameStr = sb.substring(0, sb.length() - 1);
		String sql = "SELECT map.id as map_id,map.server_id,map.version,s.hostname as host_name,s.port,v.name as service_name,map.service_id FROM t_venus_service_mapping as map left join t_venus_server as s on map.server_id=s.id left join t_venus_service as v on v.id=map.service_id where v.name in("+nameStr+") "
				+ "and map.role=? and v.registe_type=? ";
		try {
			return this.jdbcTemplate.query(sql, new Object[] {RegisteConstant.PROVIDER,RegisteConstant.OPERATOR_REGISTE}, new ResultSetExtractor<List<ServiceMappingDTO>>() {
				@Override
				public List<ServiceMappingDTO> extractData(ResultSet rs) throws SQLException, DataAccessException {
					List<ServiceMappingDTO> returnList = new ArrayList<ServiceMappingDTO>();
					while (rs.next()) {
						returnList.add(ResultUtils.rsTransServiceMappingDTO(rs));
					}
					return returnList;
				}
			});
		} catch (Exception e) {
			throw new DAOException("根据sql=>" + sql + ",serviceName=>"+serviceNames+";获取服务映射关系异常", e);
		}
	}
	
	public List<ServiceMappingDTO> queryServiceMappings(String serviceName) throws DAOException {
		String sql = "SELECT map.id as map_id,map.server_id,map.version,s.hostname as host_name,s.port,v.name as service_name,map.service_id FROM t_venus_service_mapping as map left join t_venus_server as s on map.server_id=s.id left join t_venus_service as v on v.id=map.service_id where v.name =? "
				+ "and role=? and v.registe_type=? ";
		try {
			return this.jdbcTemplate.query(sql, new Object[] {serviceName,RegisteConstant.PROVIDER,RegisteConstant.OPERATOR_REGISTE}, new ResultSetExtractor<List<ServiceMappingDTO>>() {
				@Override
				public List<ServiceMappingDTO> extractData(ResultSet rs) throws SQLException, DataAccessException {
					List<ServiceMappingDTO> returnList = new ArrayList<ServiceMappingDTO>();
					while (rs.next()) {
						returnList.add(ResultUtils.rsTransServiceMappingDTO(rs));
					}
					return returnList;
				}
			});
		} catch (Exception e) {
			throw new DAOException("根据sql=>" + sql + ",serviceName=>"+serviceName+";获取服务映射关系异常", e);
		}
	}
	
	public int getMappingCount() throws DAOException {
		String sql = "SELECT count(id) as records FROM t_venus_service_mapping as map where role='"+RegisteConstant.PROVIDER+"' and is_delete=0";
		try {
			return this.jdbcTemplate.queryForObject(sql, Integer.class);
		} catch (Exception e) {
			throw new DAOException("查询mappint count出错,获取服务映射关系个数异常", e);
		}
	}
	
	public List<VenusServiceMappingDO> queryServiceMappings(Integer pageSize, Integer id) throws DAOException {
		String sql = SELECT_FIELDS_TABLE + " where id>" + id + " and role='" + RegisteConstant.PROVIDER
				+ "' and is_delete=0" + " order by id asc limit " + pageSize;
		try {
			return this.jdbcTemplate.query(sql,new ResultSetExtractor<List<VenusServiceMappingDO>>() {
						@Override
						public List<VenusServiceMappingDO> extractData(ResultSet rs)
								throws SQLException, DataAccessException {
							List<VenusServiceMappingDO> returnList = new ArrayList<VenusServiceMappingDO>();
							while (rs.next()) {
								returnList.add(ResultUtils.resultToVenusServiceMappingDO(rs));
							}
							return returnList;
						}
					});
		} catch (Exception e) {
			throw new DAOException("根据id=>" + id + "获取服务映射关系异常", e);
		}
	}
	
	
	
}
