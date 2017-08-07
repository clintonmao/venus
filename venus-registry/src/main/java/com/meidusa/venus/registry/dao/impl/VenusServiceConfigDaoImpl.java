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
import com.meidusa.venus.registry.dao.VenusServiceConfigDAO;
import com.meidusa.venus.registry.domain.VenusServiceConfigDO;

@Component
public class VenusServiceConfigDaoImpl implements VenusServiceConfigDAO {

	@Resource
	private JdbcTemplate jdbcTemplate;

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

}
