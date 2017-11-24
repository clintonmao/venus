package com.meidusa.venus.registry.dao;

import java.util.List;

import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.data.move.OldServerDO;
import com.meidusa.venus.registry.domain.VenusServerDO;

public interface VenusServerDAO {

	int addServer(VenusServerDO venusServerDO) throws DAOException;

	boolean updateServer(VenusServerDO venusServerDO) throws DAOException;

	VenusServerDO getServer(String host, Integer port) throws DAOException;
	
	List<VenusServerDO> getServer(String host) throws DAOException;

	VenusServerDO getServer(Integer id) throws DAOException;
	
	List<VenusServerDO> getServers(List<Integer> ids) throws DAOException;
	
	boolean deleteServer(int id) throws DAOException;
	
	Integer getServerCount() throws DAOException;
	
	List<VenusServerDO> queryServers(Integer pageSize, Integer id) throws DAOException;

}
