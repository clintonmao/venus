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
import com.meidusa.venus.registry.data.move.OldServiceMappingDO;

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
		sql = sql + " order by map.id desc limit " + pageSize;

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
	public Integer getOldServiceMappingCount() throws DAOException {
		String sql = "SELECT count(map.id) as records FROM t_venus_service_mapping as map left join t_venus_server as s on map.server_id=s.id left join t_venus_service as v on v.id=map.service_id ";
		try {
			return this.jdbcTemplate.queryForObject(sql, Integer.class);
		} catch (Exception e) {
			throw new DAOException("根据sql=>" + sql + ";获取服务映射关系异常", e);
		}
	}

}
