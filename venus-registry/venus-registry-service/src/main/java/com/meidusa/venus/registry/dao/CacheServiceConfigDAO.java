package com.meidusa.venus.registry.dao;

import java.util.List;

import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.domain.VenusServiceConfigDO;

public interface CacheServiceConfigDAO {

	List<VenusServiceConfigDO> getVenusServiceConfig(Integer serviceId) throws DAOException;

}