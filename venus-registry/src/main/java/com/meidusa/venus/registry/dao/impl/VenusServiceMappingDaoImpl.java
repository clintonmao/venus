package com.meidusa.venus.registry.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;

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

	@Resource
	private JdbcTemplate jdbcTemplate;

	@Override
	public boolean addServiceMapping(VenusServiceMappingDO venusServiceMappingDO) throws DAOException {
		String sql = "insert into t_venus_service_mapping (server_id,service_id,version, active, sync,create_time, update_time) values (?, ?,?, ?,?, now(), now())";
		int update = 0;
		try {
			update = this.jdbcTemplate.update(sql, venusServiceMappingDO.getServerId(),
					venusServiceMappingDO.getServiceId(), venusServiceMappingDO.getVersion(),
					venusServiceMappingDO.isActive(), venusServiceMappingDO.isSync());
		} catch (Exception e) {
			throw new DAOException("保存venusService异常", e);
		}
		return update > 0 ? true : false;
	}

	@Override
	public boolean updateServiceMapping(VenusServiceMappingDO venusServiceMappingDO) throws DAOException {
		String sql = "update t_venus_service_mapping set version = ?, active = ? where server_id = ? and service_id = ?";
		try {
			this.jdbcTemplate.update(sql, venusServiceMappingDO.getVersion(), venusServiceMappingDO.isActive(),
					venusServiceMappingDO.getServerId(), venusServiceMappingDO.getServiceId());
		} catch (Exception e) {
			throw new DAOException("更新映射关系异常", e);
		}
		return false;
	}

	@Override
	public VenusServiceMappingDO getServiceMapping(Integer serverId, Integer serviceId) throws DAOException {
		String sql = "select id, server_id, service_id, version, active, sync, create_time, update_time from t_venus_service_mapping where server_id = ? and service_id = ?";

		try {
			return this.jdbcTemplate.query(sql, new Object[] { serverId, serviceId },
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
			throw new DAOException("获取服务映射关系异常", e);
		}
	}

	@Override
	public VenusServiceMappingDO getServiceMapping(Integer id) throws DAOException {
		String sql = "select id, server_id, service_id, version, active, sync, create_time, update_time from t_venus_service_mapping where id = ?";

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
			throw new DAOException("获取服务映射关系异常", e);
		}
	}
}
