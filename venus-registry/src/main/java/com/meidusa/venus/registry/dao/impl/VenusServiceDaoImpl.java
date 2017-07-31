package com.meidusa.venus.registry.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.dao.VenusServiceDAO;
import com.meidusa.venus.registry.domain.VenusServiceDO;

@Component
public class VenusServiceDaoImpl implements VenusServiceDAO {

	@Resource
	private JdbcTemplate jdbcTemplate;

	@Override
	public boolean addService(VenusServiceDO venusServiceDO) throws DAOException {
		String sql = "insert into t_venus_service (name,interface_name,version, description, create_time, update_time) values (?, ?,?, ?, now(), now())";
		int update = 0;
		try {
			update = this.jdbcTemplate.update(sql, venusServiceDO.getName(), venusServiceDO.getInterfaceName(),
					venusServiceDO.getVersion(), venusServiceDO.getDescription());
		} catch (Exception e) {
			throw new DAOException("保存venusService异常", e);
		}
		return update > 0 ? true : false;
	}

	@Override
	public boolean updateService(VenusServiceDO venusServiceDO) throws DAOException {
		String sql = "update t_venus_service set name=?,interface_name=?,version=?, description=?, update_time=now() where id=?";
		int update = 0;
		try {
			update = this.jdbcTemplate.update(sql, venusServiceDO.getName(), venusServiceDO.getInterfaceName(),
					venusServiceDO.getVersion(), venusServiceDO.getDescription(), venusServiceDO.getId());
		} catch (Exception e) {
			throw new DAOException("更新venusService异常", e);
		}
		return update > 0 ? true : false;
	}

	@Override
	public VenusServiceDO getService(Integer id) throws DAOException {
		String sql = "select id, name,interface_name,version, description, create_time, update_time from t_venus_service where id = ?";
		try {
			return this.jdbcTemplate.query(sql, new Object[] { id }, new ResultSetExtractor<VenusServiceDO>() {
				@Override
				public VenusServiceDO extractData(ResultSet rs) throws SQLException, DataAccessException {
					if (rs.next()) {
						return ResultUtils.resultToVenusServiceDO(rs);
					}
					return null;
				}

			});
		} catch (Exception e) {
			throw new DAOException("获取venusService异常", e);
		}
	}

	@Override
	public VenusServiceDO getService(String serviceName, String interfaceName) throws DAOException {
		String sql = "select id, name,interface_name,version, description, create_time, update_time from t_venus_service where ";
		Object[] params = new Object[] { serviceName };
		if (StringUtils.isNotBlank(serviceName)) {
			sql = sql + " name=? ";
		}
		if (StringUtils.isNotBlank(interfaceName)) {
			sql = sql + " and interface_name=?";
			params = new Object[] { serviceName, interfaceName };
		}

		return this.jdbcTemplate.query(sql, params, new ResultSetExtractor<VenusServiceDO>() {
			@Override
			public VenusServiceDO extractData(ResultSet rs) throws SQLException, DataAccessException {
				if (rs.next()) {
					return ResultUtils.resultToVenusServiceDO(rs);
				}
				return null;
			}
		});
	}

}
