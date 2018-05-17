package com.meidusa.venus.registry.dao.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;

import com.meidusa.toolkit.common.runtime.GlobalScheduler;
import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.LogUtils;
import com.meidusa.venus.registry.dao.CacheApplicationDAO;
import com.meidusa.venus.registry.dao.VenusApplicationDAO;
import com.meidusa.venus.registry.domain.VenusApplicationDO;

public class CacheVenusApplicationDaoImpl implements CacheApplicationDAO {

	private static final int PAGE_SIZE_1000 = 1000;

	private VenusApplicationDAO venusApplicationDAO;

	private Map<Integer, VenusApplicationDO> cacheIdApplicationMap = new HashMap<Integer, VenusApplicationDO>();

	private Map<String, VenusApplicationDO> cacheCodeApplicationMap = new HashMap<String, VenusApplicationDO>();

	public VenusApplicationDAO getVenusApplicationDAO() {
		return venusApplicationDAO;
	}

	public void setVenusApplicationDAO(VenusApplicationDAO venusApplicationDAO) {
		this.venusApplicationDAO = venusApplicationDAO;
	}

	public void init() {
		GlobalScheduler.getInstance().scheduleAtFixedRate(new LoadCacheApplicationsRunnable(), 1, 10, TimeUnit.SECONDS);
	}

	@Override
	public VenusApplicationDO getApplication(Integer id) throws DAOException {
		return cacheIdApplicationMap.get(id);
	}

	@Override
	public VenusApplicationDO getApplication(String appCode) throws DAOException {
		return cacheCodeApplicationMap.get(appCode);
	}

	void load() {
		List<VenusApplicationDO> allQueryApplications = new ArrayList<VenusApplicationDO>();
		
		Integer totalCount = venusApplicationDAO.getApplicationCount();
		if (null != totalCount && totalCount > 0) {
			int mod = totalCount % PAGE_SIZE_1000;
			int count = totalCount / PAGE_SIZE_1000;
			if (mod > 0) {
				count = count + 1;
			}
			int id = 0;
			for (int i = 0; i < count; i++) {
				List<VenusApplicationDO> queryApplications = venusApplicationDAO.queryApplications(PAGE_SIZE_1000, id);
				if (CollectionUtils.isNotEmpty(queryApplications)) {
					id = queryApplications.get(queryApplications.size() - 1).getId();
					allQueryApplications.addAll(queryApplications);
				}
			}
		}
		if (CollectionUtils.isNotEmpty(allQueryApplications)) {
			Map<Integer, VenusApplicationDO> localCacheIdApplicationMap = new HashMap<Integer, VenusApplicationDO>();
			Map<String, VenusApplicationDO> localCacheCodeApplicationMap = new HashMap<String, VenusApplicationDO>();

			for (VenusApplicationDO applicationDO : allQueryApplications) {
				localCacheIdApplicationMap.put(applicationDO.getId(), applicationDO);
				localCacheCodeApplicationMap.put(applicationDO.getAppCode(), applicationDO);
			}

			cacheIdApplicationMap = localCacheIdApplicationMap;
			cacheCodeApplicationMap = localCacheCodeApplicationMap;
		}
	}

	private class LoadCacheApplicationsRunnable implements Runnable {

		@Override
		public void run() {
			try {
				long start = System.currentTimeMillis();
				load();
				long consumerTime = System.currentTimeMillis() - start;
				LogUtils.logCacheSlow(consumerTime, "LoadCacheApplicationsRunnable load() ");
				LogUtils.DEFAULT_LOG.info(
						"LoadCacheApplicationsRunnable start=>{}, end=>{},consumerTime=>{},cacheCodeApplicationMap size=>{}",
						start, System.currentTimeMillis(), consumerTime, cacheCodeApplicationMap.size());
			} catch (Throwable e) {
				LogUtils.ERROR_LOG.error("load application cache data error", e);
			} 
		}

	}

}
