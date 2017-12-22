package com.meidusa.venus.registry.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.dao.VenusServiceConfigDAO;
import com.meidusa.venus.registry.domain.VenusServiceConfigDO;

public class VenusServiceConfigDaoImpl implements VenusServiceConfigDAO {

	private JdbcTemplate jdbcTemplate;

	public VenusServiceConfigDaoImpl(JdbcTemplate jdbcTemplate) {
		super();
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public List<VenusServiceConfigDO> getServiceConfigs(Integer serviceId) throws DAOException {
		String sql = "select id, type,config,service_id,create_name,update_name,create_time, update_time from t_venus_service_config where service_id = ?";
		try {
			return this.jdbcTemplate.query(sql, new Object[] { serviceId },
					new ResultSetExtractor<List<VenusServiceConfigDO>>() {
						@Override
						public List<VenusServiceConfigDO> extractData(ResultSet rs)
								throws SQLException, DataAccessException {
							List<VenusServiceConfigDO> returnList = new ArrayList<VenusServiceConfigDO>();
							while (rs.next()) {
								returnList.add(ResultUtils.resultToVenusServiceConfigDO(rs));
							}
							return returnList;
						}

					});
		} catch (Exception e) {
			throw new DAOException("根据serviceId=>" + serviceId + "获取ServiceConfig异常", e);
		}
	}
	
	public List<VenusServiceConfigDO> queryServiceConfigs(int pageSize, Integer id) throws DAOException {
		String sql = "select id, type,config,service_id,create_name,update_name,create_time, update_time from t_venus_service_config ";

		if (null != id) {
			sql = sql + " where id>" + id;
		}
		sql = sql + " order by id asc limit " + pageSize;

		try {
			return this.jdbcTemplate.query(sql, new Object[] {}, new ResultSetExtractor<List<VenusServiceConfigDO>>() {
				@Override
				public List<VenusServiceConfigDO> extractData(ResultSet rs) throws SQLException, DataAccessException {
					List<VenusServiceConfigDO> returnList = new ArrayList<VenusServiceConfigDO>();
					while (rs.next()) {
						returnList.add(ResultUtils.resultToVenusServiceConfigDO(rs));
					}
					return returnList;
				}
			});
		} catch (Exception e) {
			throw new DAOException("根据sql=>" + sql + ";获取主机列表异常", e);
		}
	}
	
	public int getServiceConfigCount() throws DAOException {
		String sql = "SELECT count(id) as records FROM t_venus_service_config ";
		try {
			return this.jdbcTemplate.queryForObject(sql, Integer.class);
		} catch (Exception e) {
			throw new DAOException("根据sql=>" + sql + ";获取主机记录数异常", e);
		}
	}

	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

}
