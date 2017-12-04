package com.meidusa.venus.registry.dao;

import java.util.List;

import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.domain.VenusApplicationDO;

public interface VenusApplicationDAO {

	int addApplication(VenusApplicationDO venusApplicationDO) throws DAOException;

	boolean updateApplication(VenusApplicationDO venusApplicationDO) throws DAOException;

	VenusApplicationDO getApplication(String appCode) throws DAOException;

	VenusApplicationDO getApplication(Integer id) throws DAOException;
	
	int getApplicationCount() throws DAOException;
	
	List<VenusApplicationDO> queryApplications(Integer pageSize, Integer id) throws DAOException;

}
