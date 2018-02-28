package com.meidusa.venus.registry.dao;

import java.util.List;

import com.meidusa.venus.URL;
import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.domain.VenusServiceDO;

public interface CacheVenusServiceDAO {

	List<VenusServiceDO> queryServices(String interfaceName, String serviceName, String version) throws DAOException;
	
	List<VenusServiceDO> queryServices(URL url) throws DAOException;
	
	List<String> queryAllServiceNames() throws DAOException;

}
