package com.meidusa.venus.registry.dao;


import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.domain.VenusServerDO;

public interface CacheVenusServerDAO {

	VenusServerDO getServer(String host, int port) throws DAOException;
	
}
