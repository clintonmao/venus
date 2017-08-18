package com.meidusa.venus.registry.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.dao.VenusServiceMappingDAO;
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
	public boolean updateServiceMapping(int id, boolean active, boolean isDelete) throws DAOException {
		String sql = "update t_venus_service_mapping set active = ?,is_delete=?,update_time=now(),registe_time=now(),heartbeat_time=now() where id = ?";
		int update = 0;
		try {
			update = this.jdbcTemplate.update(sql, active, isDelete, id);
		} catch (Exception e) {
			throw new DAOException("更新映射关系异常", e);
		}
		return update > 0;
	}

	@Override
	public boolean updateServiceMappingHeartBeatTime(int serverId, int serviceId, String version, String role)
			throws DAOException {
		String sql = "update t_venus_service_mapping set heartbeat_time = now() where server_id = ? and service_id = ? and version=? and role=? ";
		int update = 0;
		try {
			update = this.jdbcTemplate.update(sql, serverId, serviceId, version, role);
		} catch (Exception e) {
			throw new DAOException("更新映射关系heartbeat_time时间异常", e);
		}
		return update > 0;
	}

	@Override
	public boolean deleteServiceMapping(int serverId, int serviceId, String version, String role) throws DAOException {
		String sql = "update t_venus_service_mapping set is_delete = 1 where server_id = ? and service_id = ? and version=? and role=?";
		int update = 0;
		try {
			update = this.jdbcTemplate.update(sql, serverId, serviceId, version, role);
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

	public List<VenusServiceMappingDO> getServiceMappings(String dateStr) throws DAOException {
		String sql = SELECT_FIELDS_TABLE + " where heartbeat_time <= ? ";

		try {
			return this.jdbcTemplate.query(sql, new Object[] { dateStr},
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
			throw new DAOException("根据大于等于heartbeat_time＝>" + dateStr + "获取服务映射关系列表异常", e);
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
			throw new DAOException("逻辑删除更新映射关系异常", e);
		}
		return update > 0;
	}
}
