package com.meidusa.venus.registry.dao;

import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.domain.VenusServiceDO;

public interface VenusServiceDAO {

	int addService(VenusServiceDO venusServiceDO) throws DAOException;

	boolean updateService(VenusServiceDO venusServiceDO) throws DAOException;

	/**
	 * 
	 * @param serviceName 不能为空
	 * @param version TODO
	 * @param interfaceName 可为空
	 * @return
	 * @throws DAOException
	 */
	VenusServiceDO getService(String serviceName, String version, String interfaceName) throws DAOException;
	
	VenusServiceDO getService(Integer id) throws DAOException;

}
