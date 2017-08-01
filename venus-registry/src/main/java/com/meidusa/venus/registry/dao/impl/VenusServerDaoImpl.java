package com.meidusa.venus.registry.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.annotation.Resource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.dao.VenusServerDAO;
import com.meidusa.venus.registry.domain.VenusServerDO;

@Component
public class VenusServerDaoImpl implements VenusServerDAO {

	@Resource
	private JdbcTemplate jdbcTemplate;

	@Override
	public int addServer(VenusServerDO venusServerDO) throws DAOException {
		/*
		 * String sql =
		 * "insert into t_venus_server (hostname,port,create_time, update_time) values (?, ?, now(), now())"
		 * ; int update = 0; try { update = this.jdbcTemplate.update(sql,
		 * venusServerDO.getHostname(), venusServerDO.getPort()); } catch
		 * (Exception e) { throw new DAOException("保存venusServer异常", e); }
		 * return update > 0 ? true : false;
		 */

		final String sql = "insert into t_venus_server (hostname,port,create_time, update_time) values ('"
				+ venusServerDO.getHostname() + "', " + venusServerDO.getPort() + ", now(), now())";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		int autoIncId = 0;
		jdbcTemplate.update(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
				return ps;
			}
		}, keyHolder);
		autoIncId = keyHolder.getKey().intValue();
		return autoIncId;
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
			throw new DAOException("根据ID获取venusServer异常", e);
		}
	}

	@Override
	public VenusServerDO getServer(String host, Integer port) throws DAOException {
		String sql = "select id, hostname,port,create_time, update_time from t_venus_server where hostname = ? and port = ? ";
		Object[] params = new Object[] { host, port };
		try {
			return this.jdbcTemplate.query(sql, params, new ResultSetExtractor<VenusServerDO>() {
				@Override
				public VenusServerDO extractData(ResultSet rs) throws SQLException, DataAccessException {
					if (rs.next()) {
						return ResultUtils.resultToVenusServerDO(rs);
					}
					return null;
				}
			});
		} catch (Exception e) {
			throw new DAOException("根据host 和 port 获取venusServer异常", e);
		}
	}

}
