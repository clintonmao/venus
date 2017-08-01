package com.meidusa.venus.registry.dao;

import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.domain.VenusServerDO;

public interface VenusServerDAO {

	int addServer(VenusServerDO venusServerDO) throws DAOException;

	boolean updateServer(VenusServerDO venusServerDO) throws DAOException;

	VenusServerDO getServer(String host, Integer port) throws DAOException;

	VenusServerDO getServer(Integer id) throws DAOException;

}
