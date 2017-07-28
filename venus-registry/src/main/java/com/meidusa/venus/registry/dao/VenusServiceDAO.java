package com.meidusa.venus.registry.dao;

import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.domain.VenusServiceDO;

public interface VenusServiceDAO {

	boolean addService(VenusServiceDO venusServiceDO) throws DAOException;

	boolean updateService(VenusServiceDO venusServiceDO) throws DAOException;

	VenusServiceDO getService(String interfaceName, String serviceName) throws DAOException;

	VenusServiceDO getService(Integer id) throws DAOException;

}
