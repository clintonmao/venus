package com.meidusa.venus.registry.dao.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;

import com.meidusa.toolkit.common.runtime.GlobalScheduler;
import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.LogUtils;
import com.meidusa.venus.registry.dao.CacheApplicationDAO;
import com.meidusa.venus.registry.dao.VenusApplicationDAO;
import com.meidusa.venus.registry.domain.VenusApplicationDO;

public class CacheVenusApplicationDaoImpl implements CacheApplicationDAO {

	private static final int PAGE_SIZE_200 = 200;

	private VenusApplicationDAO venusApplicationDAO;

	private Map<Integer, VenusApplicationDO> cacheIdApplicationMap = new HashMap<Integer, VenusApplicationDO>();

	private Map<String, VenusApplicationDO> cacheCodeApplicationMap = new HashMap<String, VenusApplicationDO>();

	private volatile boolean loacCacheRunning = false;

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
		if (loacCacheRunning) {// 缓存加载过程中，直接返回空，让从数据库中查询;
			return null;
		}
		return cacheIdApplicationMap.get(id);
	}

	@Override
	public VenusApplicationDO getApplication(String appCode) throws DAOException {
		if (loacCacheRunning) {// 缓存加载过程中，直接返回空，让从数据库中查询;
			return null;
		}
		return cacheCodeApplicationMap.get(appCode);
	}

	void load() {
		/*loacCacheRunning = true;
		if (loacCacheRunning) {
			cacheCodeApplicationMap.clear();
			cacheIdApplicationMap.clear();
		}*/
		
		List<VenusApplicationDO> allQueryApplications = new ArrayList<VenusApplicationDO>();
		
		Integer totalCount = venusApplicationDAO.getApplicationCount();
		if (null != totalCount && totalCount > 0) {
			int mod = totalCount % PAGE_SIZE_200;
			int count = totalCount / PAGE_SIZE_200;
			if (mod > 0) {
				count = count + 1;
			}
			int id = 0;
			for (int i = 0; i < count; i++) {
				List<VenusApplicationDO> queryApplications = venusApplicationDAO.queryApplications(PAGE_SIZE_200, id);
				if (CollectionUtils.isNotEmpty(queryApplications)) {
					id = queryApplications.get(queryApplications.size() - 1).getId();
					allQueryApplications.addAll(queryApplications);
				}
			}
		}
		if (CollectionUtils.isNotEmpty(allQueryApplications)) {
			loacCacheRunning = true;
			cacheIdApplicationMap.clear();
			cacheCodeApplicationMap.clear();

			for (VenusApplicationDO applicationDO : allQueryApplications) {
				cacheIdApplicationMap.put(applicationDO.getId(), applicationDO);
				cacheCodeApplicationMap.put(applicationDO.getAppCode(), applicationDO);
			}

			loacCacheRunning = false;
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
			} finally {
				loacCacheRunning = false;
			}
		}

	}

}
