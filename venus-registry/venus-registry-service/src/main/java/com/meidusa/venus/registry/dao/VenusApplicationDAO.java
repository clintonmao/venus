package com.meidusa.venus.registry.dao;

import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.domain.VenusApplicationDO;

public interface VenusApplicationDAO {

	int addApplication(VenusApplicationDO venusApplicationDO) throws DAOException;

	boolean updateApplication(VenusApplicationDO venusApplicationDO) throws DAOException;

	VenusApplicationDO getApplication(String appCode) throws DAOException;

	VenusApplicationDO getApplication(Integer id) throws DAOException;

}
