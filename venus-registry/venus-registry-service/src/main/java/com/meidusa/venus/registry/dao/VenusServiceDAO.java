package com.meidusa.venus.registry.dao;

import java.util.Collection;
import java.util.List;

import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.domain.VenusServiceDO;

public interface VenusServiceDAO {

	int addService(VenusServiceDO venusServiceDO) throws DAOException;

	boolean updateService(String methods, boolean isDelete, int id) throws DAOException;
	
	boolean updateService(int id, boolean isDelete) throws DAOException;

	/**
	 * 
	 * @param interfaceName 可为空
	 * @param serviceName 不能为空
	 * @param version TODO
	 * @return
	 * @throws DAOException
	 */
	VenusServiceDO getService(String interfaceName, String serviceName, String version) throws DAOException;
	
	List<VenusServiceDO> queryServices(String interfaceName, String serviceName, String version) throws DAOException;
	
	List<VenusServiceDO> getServices(String interfaceName, String serviceName) throws DAOException;
	
	VenusServiceDO getService(Integer id) throws DAOException;
	
	List<VenusServiceDO> getServices(Collection<Integer> ids) throws DAOException;

}
