package com.meidusa.venus.registry.dao;

import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.domain.VenusApplicationDO;

public interface CacheApplicationDAO {

	VenusApplicationDO getApplication(String appCode) throws DAOException;

	VenusApplicationDO getApplication(Integer id) throws DAOException;

}
