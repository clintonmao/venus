package com.meidusa.venus.registry.dao;

import java.util.List;

import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.domain.VenusServiceDO;

public interface VenusServiceDAO {

	int addService(VenusServiceDO venusServiceDO) throws DAOException;

	boolean updateService(String methods, boolean isDelete, int id) throws DAOException;
	
	boolean updateService(int id, boolean isDelete) throws DAOException;

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
	
	List<VenusServiceDO> getServices(List<Integer> ids) throws DAOException;

}
