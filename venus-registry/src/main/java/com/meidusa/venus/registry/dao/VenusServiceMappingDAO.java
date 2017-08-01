package com.meidusa.venus.registry.dao;

import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.domain.VenusServiceMappingDO;

public interface VenusServiceMappingDAO {

	boolean addServiceMapping(VenusServiceMappingDO venusServiceMappingDO) throws DAOException;

	boolean updateServiceMapping(VenusServiceMappingDO venusServiceMappingDO) throws DAOException;

	VenusServiceMappingDO getServiceMapping(Integer serverId,Integer serviceId, String role) throws DAOException;
	
	VenusServiceMappingDO getServiceMapping(Integer id) throws DAOException;

}
