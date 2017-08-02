package com.meidusa.venus.registry.dao;

import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.domain.VenusServiceConfigDO;

public interface VenusServiceConfigDAO {

	VenusServiceConfigDO getServiceConfig(Integer serviceId) throws DAOException;

}
