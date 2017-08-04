package com.meidusa.venus.registry.dao;

import java.util.List;

import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.domain.VenusServiceMappingDO;

public interface VenusServiceMappingDAO {

	boolean addServiceMapping(VenusServiceMappingDO venusServiceMappingDO) throws DAOException;

	boolean updateServiceMapping(int id,boolean active) throws DAOException;

	boolean updateServiceMappingHeartBeatTime(int serverId, int serviceId, String version, String role) throws DAOException;

	boolean deleteServiceMapping(int serverId, int serviceId, String version, String role) throws DAOException;

	VenusServiceMappingDO getServiceMapping(Integer serverId, Integer serviceId, String role) throws DAOException;

	List<VenusServiceMappingDO> getServiceMapping(Integer serviceId, String role) throws DAOException;

	VenusServiceMappingDO getServiceMapping(Integer id) throws DAOException;

	List<VenusServiceMappingDO> getServiceMappings(Integer serverId) throws DAOException;

}
