package com.meidusa.venus.registry.dao;

import java.util.List;

import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.data.move.OldServerDO;
import com.meidusa.venus.registry.data.move.OldServiceDO;
import com.meidusa.venus.registry.data.move.OldServiceMappingDO;

public interface OldServiceMappingDAO {

	List<OldServiceMappingDO> queryOldServiceMappings(Integer pageSize, Integer mappId) throws DAOException;

	Integer getOldServiceMappingCount() throws DAOException;

	List<OldServiceDO> queryOldServices(Integer pageSize, Integer id) throws DAOException;

	Integer getOldServiceCount() throws DAOException;

	List<OldServerDO> queryOldServers(Integer pageSize, Integer id) throws DAOException;

	Integer getOldServerCount() throws DAOException;
	
	List<String> queryOldServiceVersions(String serviceName) throws DAOException;

}
