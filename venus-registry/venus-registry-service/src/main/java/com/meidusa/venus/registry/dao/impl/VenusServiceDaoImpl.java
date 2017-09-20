package com.meidusa.venus.registry.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.dao.VenusServiceDAO;
import com.meidusa.venus.registry.domain.VenusServiceDO;

public class VenusServiceDaoImpl implements VenusServiceDAO {

	private static final String SELECT_FIELDS = "select id, name,interface_name,version, description, app_id,registe_type,methods,is_delete,create_time, update_time ";
	
	private JdbcTemplate jdbcTemplate;

	public VenusServiceDaoImpl(JdbcTemplate jdbcTemplate) {
		super();
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public int addService(VenusServiceDO venusServiceDO) throws DAOException {
		final String sql = "insert into t_venus_service (name,interface_name,version, description,app_id,registe_type,methods,is_delete, create_time, update_time) values ('"
				+ venusServiceDO.getName() + "', '" + venusServiceDO.getInterfaceName() + "', '"
				+ venusServiceDO.getVersion() + "', '" + venusServiceDO.getDescription() + "', "
				+ venusServiceDO.getAppId() + "," + venusServiceDO.getRegisteType() + ",'" + venusServiceDO.getMethods()
				+ "'," + venusServiceDO.getIsDelete() + ", now(), now())";
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
	public boolean updateService(String methods, boolean isDelete, int id) throws DAOException {
		String sql = "update t_venus_service set methods=?,is_delete=?,update_time=now() where id=?";
		int update = 0;
		try {
			update = this.jdbcTemplate.update(sql, methods, isDelete, id);
		} catch (Exception e) {
			throw new DAOException("更新venusService异常", e);
		}
		return update > 0;
	}
	
	@Override
	public boolean updateService(int id, boolean isDelete) throws DAOException {
		String sql = "update t_venus_service set is_delete=?,update_time=now() where id=?";
		int update = 0;
		try {
			update = this.jdbcTemplate.update(sql, isDelete, id);
		} catch (Exception e) {
			throw new DAOException("更新venusService异常", e);
		}
		return update > 0;
	}

	@Override
	public VenusServiceDO getService(Integer id) throws DAOException {
		String sql = SELECT_FIELDS + " from t_venus_service where id = ?";
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
	public VenusServiceDO getService(String interfaceName, String serviceName, String version) throws DAOException {
		String sql = SELECT_FIELDS + " from t_venus_service where ";
		Object[] params = new Object[] { serviceName, version };
		if (StringUtils.isNotBlank(serviceName)) {
			sql = sql + " name=? and version=?";
		}
		if (StringUtils.isNotBlank(interfaceName)) {
			sql = sql + " and interface_name=?";
			params = new Object[] { serviceName, version, interfaceName };
		}
		try {
			return this.jdbcTemplate.query(sql, params, new ResultSetExtractor<VenusServiceDO>() {
				@Override
				public VenusServiceDO extractData(ResultSet rs) throws SQLException, DataAccessException {
					if (rs.next()) {
						return ResultUtils.resultToVenusServiceDO(rs);
					}
					return null;
				}
			});
		} catch (Exception e) {
			throw new DAOException("根据serviceName:" + serviceName + ",获取venusService异常", e);
		}
	}
	
	public static Object[] listToArray(List<Object> list) {
		if (null == list) {
			return new Object[0];
		}
		int size = list.size();
		return list.toArray(new Object[size]);
	}
	
	@Override
	public List<VenusServiceDO> queryServices(String interfaceName, String serviceName, String version)
			throws DAOException {
		if(StringUtils.isBlank(interfaceName) && StringUtils.isBlank(serviceName)){
			throw new DAOException("serviceName与interfaceName不能同时为空");
		}
		
		String sql = SELECT_FIELDS + " from t_venus_service where ";
		StringBuilder whereSql = new StringBuilder();
		List<Object> params = new ArrayList<Object>();
		if (StringUtils.isNotBlank(serviceName)) {
			whereSql.append(" and name=? ");
			params.add(serviceName);
		}
		if (StringUtils.isNotBlank(interfaceName)) {
			whereSql.append(" and interface_name=? ");
			params.add(interfaceName);
		}

		if (StringUtils.isNotBlank(version)) {
			whereSql.append(" and version=? ");
			params.add(version);
		}
		
		String wsql = whereSql.toString().trim().substring(whereSql.toString().trim().indexOf("and") + 3);
		sql = sql + " " + wsql;
		
		try {
			return this.jdbcTemplate.query(sql, listToArray(params), new ResultSetExtractor<List<VenusServiceDO>>() {
				@Override
				public List<VenusServiceDO> extractData(ResultSet rs) throws SQLException, DataAccessException {
					List<VenusServiceDO> returnList = new ArrayList<VenusServiceDO>();
					while (rs.next()) {
						VenusServiceDO resultToVenusServiceDO = ResultUtils.resultToVenusServiceDO(rs);
						if (resultToVenusServiceDO.getIsDelete()) {
							continue;
						}
						returnList.add(resultToVenusServiceDO);
					}
					return returnList;
				}
			});
		} catch (Exception e) {
			throw new DAOException("根据serviceName:" + serviceName + ",获取venusService异常", e);
		}
	}
	
	public List<VenusServiceDO> getServices(String interfaceName, String serviceName) throws DAOException {
		String sql = SELECT_FIELDS + " from t_venus_service where ";
		Object[] params = new Object[] { serviceName};
		if (StringUtils.isNotBlank(serviceName)) {
			sql = sql + " name=? ";
		}
		if (StringUtils.isNotBlank(interfaceName)) {
			sql = sql + " and interface_name=?";
			params = new Object[] { serviceName, interfaceName };
		}
		try {
			return this.jdbcTemplate.query(sql, params, new ResultSetExtractor<List<VenusServiceDO>>() {
				@Override
				public List<VenusServiceDO> extractData(ResultSet rs) throws SQLException, DataAccessException {
					List<VenusServiceDO> returnList = new ArrayList<VenusServiceDO>();
					while (rs.next()) {
						VenusServiceDO resultToVenusServiceDO = ResultUtils.resultToVenusServiceDO(rs);
						returnList.add(resultToVenusServiceDO);
					}
					return returnList;
				}
			});
		} catch (Exception e) {
			throw new DAOException("根据serviceName:" + serviceName + ",获取venusService异常", e);
		}
	}

	@Override
	public List<VenusServiceDO> getServices(Collection<Integer> ids) throws DAOException {
		if (ids.isEmpty()) {
			return new ArrayList<VenusServiceDO>();
		}
		StringBuilder sb = new StringBuilder();
		for (Integer id : ids) {
			sb.append(id);
			sb.append(",");
		}
		String str = sb.substring(0, sb.length() - 1);
		String sql = SELECT_FIELDS + " from t_venus_service where id in(" + str + ")";
		try {
			return this.jdbcTemplate.query(sql, new ResultSetExtractor<List<VenusServiceDO>>() {
				@Override
				public List<VenusServiceDO> extractData(ResultSet rs) throws SQLException, DataAccessException {
					List<VenusServiceDO> returnList = new ArrayList<VenusServiceDO>();
					while (rs.next()) {
						VenusServiceDO resultToVenusServiceDO = ResultUtils.resultToVenusServiceDO(rs);
						returnList.add(resultToVenusServiceDO);
					}
					return returnList;
				}

			});
		} catch (Exception e) {
			throw new DAOException("获取venusService异常", e);
		}
	}

}
