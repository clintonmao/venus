package com.meidusa.venus.registry.dao;

import java.util.List;

import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.data.move.OldServiceMappingDO;

public interface OldServiceMappingDAO {
	
	List<OldServiceMappingDO> queryOldServiceMappings(Integer pageSize,Integer mappId)throws DAOException;
	
	Integer getOldServiceMappingCount()throws DAOException;

}
