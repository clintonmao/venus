package com.meidusa.venus.registry.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.dao.OldServiceMappingDAO;
import com.meidusa.venus.registry.data.move.OldServerDO;
import com.meidusa.venus.registry.data.move.OldServiceDO;
import com.meidusa.venus.registry.data.move.OldServiceMappingDO;
import com.meidusa.venus.registry.data.move.ServiceMappingDTO;

public class OldServiceMappingDaoImpl implements OldServiceMappingDAO {

	private JdbcTemplate jdbcTemplate;

	public OldServiceMappingDaoImpl(JdbcTemplate jdbcTemplate) {
		super();
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public List<OldServiceMappingDO> queryOldServiceMappings(Integer pageSize, Integer mappId) throws DAOException {
		String sql = "SELECT map.id as map_id,map.server_id,s.hostname as host_name,s.port,map.service_id,v.name as service_name,v.description,map.version,map.active,map.sync,map.create_time,map.update_time FROM t_venus_service_mapping as map left join t_venus_server as s on map.server_id=s.id left join t_venus_service as v on v.id=map.service_id ";
		
		if (null != mappId) {
			sql = sql + " where map.id>" + mappId;
		}
		sql = sql + " order by map.id asc limit " + pageSize;
		
		try {
			return this.jdbcTemplate.query(sql, new Object[] {}, new ResultSetExtractor<List<OldServiceMappingDO>>() {
				@Override
				public List<OldServiceMappingDO> extractData(ResultSet rs) throws SQLException, DataAccessException {
					List<OldServiceMappingDO> returnList = new ArrayList<OldServiceMappingDO>();
					while (rs.next()) {
						returnList.add(ResultUtils.rsToOldServiceMappingDO(rs));
					}
					return returnList;
				}
			});
		} catch (Exception e) {
			throw new DAOException("根据sql=>" + sql + ";获取服务映射关系异常", e);
		}
	}
	
	@Override
	public List<ServiceMappingDTO> queryOldServiceMappings(List<String> serviceNames) throws DAOException {
		StringBuilder sb = new StringBuilder();
		for (String name : serviceNames) {
			sb.append("'");
			sb.append(name);
			sb.append("'");
			sb.append(",");
		}
		String nameStr = sb.substring(0, sb.length() - 1);
		String sql = "SELECT map.id as map_id,map.server_id,map.version,s.hostname as host_name,s.port,v.name as service_name,map.service_id FROM t_venus_service_mapping as map left join t_venus_server as s on map.server_id=s.id left join t_venus_service as v on v.id=map.service_id where v.name in("+nameStr+")";
		
		try {
			return this.jdbcTemplate.query(sql, new Object[] {}, new ResultSetExtractor<List<ServiceMappingDTO>>() {
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
	
	@Override
	public List<ServiceMappingDTO> queryOldServiceMappings(String serviceName) throws DAOException {
		StringBuilder sb = new StringBuilder();
		String sql = "SELECT map.id as map_id,map.server_id,map.version,s.hostname as host_name,s.port,v.name as service_name,map.service_id FROM t_venus_service_mapping as map left join t_venus_server as s on map.server_id=s.id left join t_venus_service as v on v.id=map.service_id where v.name =?";
		
		try {
			return this.jdbcTemplate.query(sql, new Object[] {serviceName}, new ResultSetExtractor<List<ServiceMappingDTO>>() {
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
	
	
	

	@Override
	public Integer getOldServiceMappingCount() throws DAOException {
		String sql = "SELECT count(map.id) as records FROM t_venus_service_mapping as map left join t_venus_server as s on map.server_id=s.id left join t_venus_service as v on v.id=map.service_id ";
		try {
			return this.jdbcTemplate.queryForObject(sql, Integer.class);
		} catch (Exception e) {
			throw new DAOException("根据sql=>" + sql + ";获取服务映射关系记录数异常", e);
		}
	}

	@Override
	public List<OldServiceDO> queryOldServices(Integer pageSize, Integer id) throws DAOException {
		String sql = "SELECT id,name as service_name,description from t_venus_service ";

		if (null != id) {
			sql = sql + " where id>" + id;
		}
		sql = sql + " order by id asc limit " + pageSize;

		try {
			return this.jdbcTemplate.query(sql, new Object[] {}, new ResultSetExtractor<List<OldServiceDO>>() {
				@Override
				public List<OldServiceDO> extractData(ResultSet rs) throws SQLException, DataAccessException {
					List<OldServiceDO> returnList = new ArrayList<OldServiceDO>();
					while (rs.next()) {
						returnList.add(ResultUtils.rsToOldServiceDO(rs));
					}
					return returnList;
				}
			});
		} catch (Exception e) {
			throw new DAOException("根据sql=>" + sql + ";获取服务列表异常", e);
		}
	}
	
	
	
	public List<String> queryOldServiceVersions(String serviceName) throws DAOException {
		String sql = "SELECT distinct map.version FROM t_venus_service_mapping as map left join t_venus_service as v on v.id=map.service_id "
				+ "where v.name=? order by map.id desc ";

		try {
			return this.jdbcTemplate.query(sql, new Object[] {serviceName}, new ResultSetExtractor<List<String>>() {
				@Override
				public List<String> extractData(ResultSet rs) throws SQLException, DataAccessException {
					List<String> returnList = new ArrayList<String>();
					while (rs.next()) {
						returnList.add(rs.getString("version"));
					}
					return returnList;
				}
			});
		} catch (Exception e) {
			throw new DAOException("根据sql=>" + sql + ";获取服务version列表异常", e);
		}
	}
	
	
	@Override
	public Integer getOldServiceCount() throws DAOException {
		String sql = "SELECT count(id) as records FROM t_venus_service ";
		try {
			return this.jdbcTemplate.queryForObject(sql, Integer.class);
		} catch (Exception e) {
			throw new DAOException("根据sql=>" + sql + ";获取服务记录数异常", e);
		}
	}

	@Override
	public List<OldServerDO> queryOldServers(Integer pageSize, Integer id) throws DAOException {
		String sql = "SELECT id,hostname,port FROM t_venus_server ";

		if (null != id) {
			sql = sql + " where id>" + id;
		}
		sql = sql + " order by id asc limit " + pageSize;

		try {
			return this.jdbcTemplate.query(sql, new Object[] {}, new ResultSetExtractor<List<OldServerDO>>() {
				@Override
				public List<OldServerDO> extractData(ResultSet rs) throws SQLException, DataAccessException {
					List<OldServerDO> returnList = new ArrayList<OldServerDO>();
					while (rs.next()) {
						returnList.add(ResultUtils.rsToOldServerDO(rs));
					}
					return returnList;
				}
			});
		} catch (Exception e) {
			throw new DAOException("根据sql=>" + sql + ";获取主机列表异常", e);
		}
	}

	@Override
	public Integer getOldServerCount() throws DAOException {
		String sql = "SELECT count(id) as records FROM t_venus_server ";
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
