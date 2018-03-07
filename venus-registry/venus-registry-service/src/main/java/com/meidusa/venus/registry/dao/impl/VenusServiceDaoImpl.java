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

import com.meidusa.venus.URL;
import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.dao.VenusServiceDAO;
import com.meidusa.venus.registry.domain.RegisteConstant;
import com.meidusa.venus.registry.domain.VenusServiceDO;
import com.meidusa.venus.registry.util.RegistryUtil;

public class VenusServiceDaoImpl implements VenusServiceDAO {

	private static final String SELECT_FIELDS = "select id, name,interface_name,version,version_range, description, app_id,registe_type,methods,is_delete,create_time, update_time ";
	
	private JdbcTemplate jdbcTemplate;

	public VenusServiceDaoImpl(JdbcTemplate jdbcTemplate) {
		super();
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public int addService(VenusServiceDO venusServiceDO) throws DAOException {
		final String insertSql=genInsertSql(venusServiceDO);
		KeyHolder keyHolder = new GeneratedKeyHolder();
		int autoIncId = 0;
		jdbcTemplate.update(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS);
				return ps;
			}
		}, keyHolder);
		autoIncId = keyHolder.getKey().intValue();
		return autoIncId;
	}
	
	private static String genInsertSql(VenusServiceDO vs) {
		StringBuilder columns = new StringBuilder();
		StringBuilder values = new StringBuilder();
		
		if (RegistryUtil.isNotBlank(vs.getName())) {
			columns.append("name,");
			values.append("'" + vs.getName() + "',");
		}
		
		if (RegistryUtil.isNotBlank(vs.getInterfaceName())) {
			columns.append("interface_name,");
			values.append("'" + vs.getInterfaceName() + "',");
		}
		
		if (RegistryUtil.isNotBlank(vs.getVersion())) {
			columns.append("version,");
			values.append("'" + vs.getVersion() + "',");
		}
		if (RegistryUtil.isNotBlank(vs.getVersionRange())) {
			columns.append("version_range,");
			values.append("'" + vs.getVersionRange() + "',");
		}
		
		if (RegistryUtil.isNotBlank(vs.getDescription())) {
			columns.append("description,");
			values.append("'" + vs.getDescription() + "',");
		}else{
			columns.append("description,");
			values.append("'',");
		}
		
		if (null != vs.getAppId()) {
			columns.append("app_id,");
			values.append(vs.getAppId() + ",");
		}

		columns.append("registe_type,");
		values.append(vs.getRegisteType() + ",");

		if (RegistryUtil.isNotBlank(vs.getMethods())) {
			columns.append("methods,");
			values.append("'" + vs.getMethods() + "',");
		}
		
		columns.append("is_delete,");
		values.append(vs.getIsDelete() + ",");

		columns.append("create_time,");
		values.append("now(),");

		columns.append("update_time,");
		values.append("now(),");

		String columnsStr = columns.toString();
		String valuesStr = values.toString();
		return "insert into t_venus_service (" + columnsStr.substring(0, columnsStr.length() - 1) + ") values ("
				+ values.substring(0, valuesStr.length() - 1) + ")";

	}

	public static void main(String args[]){
		URL u = new URL();
		u.setServiceName("orderService");
		u.setInterfaceName("com.chexiang.order.OrderService");
		u.setVersion("1.0.0");

		u.setPath("com.chexiang.order.OrderService/orderService");
		u.setApplication("test-order-domain");
		u.setHost("192.168.0.1");

		u.setPort(16800);
		u.setProtocol("venus");
		u.setLoadbanlance("random");
		u.setMethods("getOrderById[java.lang.String],selectAllOrder[java.lang.String]");
		
		VenusServiceDO vs = new VenusServiceDO();
		vs.setInterfaceName(u.getInterfaceName());
		vs.setName(u.getServiceName());
		vs.setAppId(0);
		vs.setVersion(u.getVersion());
		vs.setRegisteType(RegisteConstant.AUTO_REGISTE);
		vs.setMethods(u.getMethods());
		vs.setDescription("desc");
		vs.setDelete(false);
		System.out.println(genInsertSql(vs));
	}

	@Override
	public boolean updateService(String methods, boolean isDelete, int id, Integer appId,String supportVersion) throws DAOException {
		String sql = "update t_venus_service set methods=?,is_delete=?,version_range=?,update_time=now()";
		if (null != appId && appId > 0) {
			sql = sql + ",app_id=? ";
		}
		sql = sql + " where id=?";
		int update = 0;
		try {
			if (null != appId && appId > 0) {
				update = this.jdbcTemplate.update(sql, methods, isDelete,supportVersion, appId, id);
			} else {
				update = this.jdbcTemplate.update(sql, methods, isDelete,supportVersion, id);
			}
		} catch (Exception e) {
			throw new DAOException("更新venusService异常", e);
		}
		return update > 0;
	}
	
	@Override
	@Deprecated
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
	
	public boolean updateServiceAppId(int id, int appId) throws DAOException {
		String sql = "update t_venus_service set app_id=?,update_time=now() where id=?";
		int update = 0;
		try {
			update = this.jdbcTemplate.update(sql, appId, id);
		} catch (Exception e) {
			throw new DAOException("更新venusService appId异常", e);
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
		String sql = SELECT_FIELDS + " from t_venus_service where version=? ";
		List<Object> params = new ArrayList<Object>();
		params.add(version);
		if (RegistryUtil.isNotBlank(serviceName)) {
			sql = sql + " and name=? ";
			params.add(serviceName);
		}
		if (RegistryUtil.isNotBlank(interfaceName)) {
			sql = sql + " and interface_name=?";
			params.add(interfaceName);
		}
		try {
			return this.jdbcTemplate.query(sql, listToArray(params), new ResultSetExtractor<VenusServiceDO>() {
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
	
	@Override
	public VenusServiceDO getService(String serviceName,String version) throws DAOException {
		String sql = SELECT_FIELDS + " from t_venus_service where name=? and version=?";
		
		Object[] params = new Object[] {serviceName,version};
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
	
	@Override
	public VenusServiceDO getService(String serviceName,int registeType,String versionRange) throws DAOException {
		String sql = SELECT_FIELDS + " from t_venus_service where name=? and registe_type=?";
		Object[] params = new Object[] {serviceName,registeType};
		if (StringUtils.isNotBlank(versionRange)) {
			sql = sql + " and version_range=?";
			params = new Object[] { serviceName, registeType, versionRange };
		} else {
			sql = sql + " and version_range is null";
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
		
		if(RegistryUtil.isNotBlank(serviceName) && RegistryUtil.isNotBlank(interfaceName)){
			whereSql.append(" (name=? or interface_name=?) ");
			params.add(serviceName);
			params.add(interfaceName);
		}else{
			if (RegistryUtil.isNotBlank(serviceName)) {
				whereSql.append(" and name=? ");
				params.add(serviceName);
			}
			if (RegistryUtil.isNotBlank(interfaceName)) {
				whereSql.append(" and interface_name=? ");
				params.add(interfaceName);
			}
		}
		
		if (RegistryUtil.isNotBlank(version)) {
			whereSql.append(" and version=? ");
			params.add(version);
		}
		
		String trimWhere = whereSql.toString().trim();
		if (trimWhere.startsWith("and")) {
			trimWhere = trimWhere.substring(trimWhere.indexOf("and") + 3);
		}
		sql = sql + " " + trimWhere +" order by version desc ";
		
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
	
	@Override
	public List<VenusServiceDO> queryServicesByName(String interfaceName, String serviceName, String version)
			throws DAOException {
		if(StringUtils.isBlank(interfaceName) && StringUtils.isBlank(serviceName)){
			throw new DAOException("serviceName与interfaceName不能同时为空");
		}
		
		String sql = SELECT_FIELDS + " from t_venus_service where ";
		StringBuilder whereSql = new StringBuilder();
		List<Object> params = new ArrayList<Object>();
		
		if (RegistryUtil.isNotBlank(serviceName)) {
			whereSql.append(" and name=? ");
			params.add(serviceName);
		}

		if (RegistryUtil.isNotBlank(version)) {
			whereSql.append(" and version=? ");
			params.add(version);
		}
		
		String trimWhere = whereSql.toString().trim();
		if (trimWhere.startsWith("and")) {
			trimWhere = trimWhere.substring(trimWhere.indexOf("and") + 3);
		}
		sql = sql + " " + trimWhere +" order by version desc ";
		
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
	
	@Deprecated
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

	@Deprecated
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
	
	@Override
	public Integer getServiceCount() throws DAOException {
		String sql = "SELECT count(id) as records FROM t_venus_service ";
		try {
			return this.jdbcTemplate.queryForObject(sql, Integer.class);
		} catch (Exception e) {
			throw new DAOException("根据sql=>" + sql + ";获取服务记录数异常", e);
		}
	}
	
	@Override
	public List<VenusServiceDO> queryServices(Integer pageSize, Integer id) throws DAOException {
		String sql = SELECT_FIELDS + " from t_venus_service ";

		if (null != id) {
			sql = sql + " where id>" + id;
		}
		sql = sql + " order by id asc limit " + pageSize;

		try {
			return this.jdbcTemplate.query(sql, new Object[] {}, new ResultSetExtractor<List<VenusServiceDO>>() {
				@Override
				public List<VenusServiceDO> extractData(ResultSet rs) throws SQLException, DataAccessException {
					List<VenusServiceDO> returnList = new ArrayList<VenusServiceDO>();
					while (rs.next()) {
						returnList.add(ResultUtils.resultToVenusServiceDO(rs));
					}
					return returnList;
				}
			});
		} catch (Exception e) {
			throw new DAOException("根据sql=>" + sql + ";获取服务列表异常", e);
		}
	}
	
	@Override
	public List<VenusServiceDO> queryPageServices(int start, int size) throws DAOException {
		String sql = SELECT_FIELDS + " from t_venus_service ";

		sql = sql + " order by id asc limit " + start+","+size;

		try {
			return this.jdbcTemplate.query(sql, new Object[] {}, new ResultSetExtractor<List<VenusServiceDO>>() {
				@Override
				public List<VenusServiceDO> extractData(ResultSet rs) throws SQLException, DataAccessException {
					List<VenusServiceDO> returnList = new ArrayList<VenusServiceDO>();
					while (rs.next()) {
						returnList.add(ResultUtils.resultToVenusServiceDO(rs));
					}
					return returnList;
				}
			});
		} catch (Exception e) {
			throw new DAOException("根据sql=>" + sql + ";获取服务列表异常", e);
		}
	}
	
	@Deprecated
	public boolean updateServiceVersionRange(int id, String versionRange) throws DAOException {
		String sql = "update t_venus_service set version_range=?,update_time=now() where id=?";
		int update = 0;
		try {
			update = this.jdbcTemplate.update(sql, versionRange, id);
		} catch (Exception e) {
			throw new DAOException("更新venusService versionRange异常", e);
		}
		return update > 0;
	}

	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

}
