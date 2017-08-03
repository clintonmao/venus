package com.meidusa.venus.registry.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.dao.VenusServiceMappingDAO;
import com.meidusa.venus.registry.domain.VenusServiceMappingDO;

@Component
public class VenusServiceMappingDaoImpl implements VenusServiceMappingDAO {

	private static final String SELECT_FIELDS_TABLE = "select id, server_id, service_id, version, active, sync,role,registe_type,is_delete,create_time, update_time,registe_time,heartbeat_time from t_venus_service_mapping ";
	
	@Resource
	private JdbcTemplate jdbcTemplate;

	@Override
	public boolean addServiceMapping(VenusServiceMappingDO mapping) throws DAOException {
		String sql = "insert into t_venus_service_mapping (server_id,service_id,version, active, sync,role,registe_type,is_delete,create_time, update_time,registe_time) values (?, ?, ?, ?, ?, ?, ?, 0,now(), now(),now())";
		int update = 0;
		try {
			update = this.jdbcTemplate.update(sql, mapping.getServerId(), mapping.getServiceId(), mapping.getVersion(),
					mapping.isActive(), mapping.isSync(), mapping.getRole(), mapping.getRegisteType());
		} catch (Exception e) {
			throw new DAOException("保存服务映射关系异常", e);
		}
		return update > 0 ? true : false;
	}

	@Override
	public boolean updateServiceMapping(VenusServiceMappingDO venusServiceMappingDO) throws DAOException {
		String sql = "update t_venus_service_mapping set active = ? where server_id = ? and service_id = ?";
		try {
			this.jdbcTemplate.update(sql, venusServiceMappingDO.isActive(), venusServiceMappingDO.getServerId(),
					venusServiceMappingDO.getServiceId());
		} catch (Exception e) {
			throw new DAOException("更新映射关系异常", e);
		}
		return false;
	}
	
	@Override
	public boolean updateServiceMappingHeartBeatTime(int serverId,int serviceId,String version, String role) throws DAOException {
		String sql = "update t_venus_service_mapping set heartbeat_time = now() where server_id = ? and service_id = ? and role=?";
		try {
			this.jdbcTemplate.update(sql, serverId, serviceId,
					role);
		} catch (Exception e) {
			throw new DAOException("更新映射关系heartbeat_time时间异常", e);
		}
		return false;
	}

	@Override
	public boolean deleteServiceMapping(int serverId, int serviceId, String version, String role)
			throws DAOException {
		String sql = "update t_venus_service_mapping set is_delete = 1 where server_id = ? and service_id = ? and version=? and registe_type=1";
		try {
			this.jdbcTemplate.update(sql, serverId, serviceId, version);
		} catch (Exception e) {
			throw new DAOException("更新映射关系异常", e);
		}
		return false;
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
	public List<VenusServiceMappingDO> getServiceMapping(Integer serviceId, String role) throws DAOException {
		String sql = SELECT_FIELDS_TABLE + " where service_id = ? and role=?";

		try {
			return this.jdbcTemplate.query(sql, new Object[] { serviceId, role },
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
}
