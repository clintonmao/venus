package com.meidusa.venus.registry.dao;

import java.util.Collection;
import java.util.List;

import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.domain.VenusServiceDO;

public interface VenusServiceDAO {

	int addService(VenusServiceDO venusServiceDO) throws DAOException;

	boolean updateService(String methods, boolean isDelete, int id, Integer appId,String supportVersion,String endpoints) throws DAOException;
	
	@Deprecated
	boolean updateService(int id, boolean isDelete) throws DAOException;
	
	boolean updateServiceAppId(int id, int appId) throws DAOException;

	/**
	 * 
	 * @param interfaceName 可为空
	 * @param serviceName 不能为空
	 * @param version
	 * @return
	 * @throws DAOException
	 */
	VenusServiceDO getService(String interfaceName, String serviceName, String version) throws DAOException;
	
	int getCountByServiceNameAndAppId(String serviceName, int appId) throws DAOException;
	
	List<VenusServiceDO> queryServices(String interfaceName, String serviceName, String version) throws DAOException;
	
	List<VenusServiceDO> queryServicesByName(String interfaceName, String serviceName, String version) throws DAOException;
	
	List<VenusServiceDO> getServices(String interfaceName, String serviceName) throws DAOException;
	
	VenusServiceDO getService(Integer id) throws DAOException;
	
	VenusServiceDO getService(String serviceName,String version) throws DAOException;
	
	VenusServiceDO getService(String serviceName,int registeType,String versionRange) throws DAOException;
	
	List<VenusServiceDO> getServices(Collection<Integer> ids) throws DAOException;
	
	Integer getServiceCount() throws DAOException;
	
	List<VenusServiceDO> queryServices(Integer pageSize, Integer id) throws DAOException;
	
	boolean updateServiceVersionRange(int id, String versionRange) throws DAOException;
	
	List<VenusServiceDO> queryPageServices(int start, int size) throws DAOException;
	
	List<VenusServiceDO> queryServicesByKeyWord(String keyword,String version, int size) throws DAOException;

}
