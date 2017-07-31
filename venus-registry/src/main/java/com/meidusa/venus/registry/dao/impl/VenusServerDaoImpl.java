package com.meidusa.venus.registry.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.annotation.Resource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.dao.VenusServerDAO;
import com.meidusa.venus.registry.domain.VenusServerDO;

@Component
public class VenusServerDaoImpl implements VenusServerDAO {

	@Resource
	private JdbcTemplate jdbcTemplate;

	@Override
	public boolean addServer(VenusServerDO venusServerDO) throws DAOException {
		String sql = "insert into t_venus_server (hostname,port,create_time, update_time) values (?, ?, now(), now())";
		int update = 0;
		try {
			update = this.jdbcTemplate.update(sql, venusServerDO.getHostname(), venusServerDO.getPort());
		} catch (Exception e) {
			throw new DAOException("保存venusServer异常", e);
		}
		return update > 0 ? true : false;
	}

	@Override
	public boolean updateServer(VenusServerDO venusServerDO) throws DAOException {
		String sql = "update t_venus_server set hostname=?,port=?, update_time=now() where id=?";
		int update = 0;
		try {
			update = this.jdbcTemplate.update(sql, venusServerDO.getHostname(), venusServerDO.getPort(),
					venusServerDO.getId());
		} catch (Exception e) {
			throw new DAOException("更新venusServer异常", e);
		}
		return update > 0 ? true : false;
	}

	@Override
	public VenusServerDO getServer(Integer id) throws DAOException {
		String sql = "select id, hostname,port,create_time, update_time from t_venus_server where id = ?";
		try {
			return this.jdbcTemplate.query(sql, new Object[] { id }, new ResultSetExtractor<VenusServerDO>() {
				@Override
				public VenusServerDO extractData(ResultSet rs) throws SQLException, DataAccessException {
					if (rs.next()) {
						return ResultUtils.resultToVenusServerDO(rs);
					}
					return null;
				}

			});
		} catch (Exception e) {
			throw new DAOException("获取venusService异常", e);
		}
	}

	@Override
	public VenusServerDO getServer(String host, String port) throws DAOException {
		String sql = "select id, hostname,port,create_time, update_time from t_venus_server where hostname = ? and port = ? ";
		Object[] params = new Object[] { host, port };
		return this.jdbcTemplate.query(sql, params, new ResultSetExtractor<VenusServerDO>() {
			@Override
			public VenusServerDO extractData(ResultSet rs) throws SQLException, DataAccessException {
				if (rs.next()) {
					return ResultUtils.resultToVenusServerDO(rs);
				}
				return null;
			}
		});
	}

}
