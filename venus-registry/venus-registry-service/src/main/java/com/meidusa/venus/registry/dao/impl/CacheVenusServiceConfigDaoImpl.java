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
import com.meidusa.venus.registry.dao.CacheServiceConfigDAO;
import com.meidusa.venus.registry.dao.VenusServiceConfigDAO;
import com.meidusa.venus.registry.domain.VenusServiceConfigDO;

public class CacheVenusServiceConfigDaoImpl implements CacheServiceConfigDAO {

	private static final int PAGE_SIZE_200 = 200;

	private VenusServiceConfigDAO venusServiceConfigDAO;

	private Map<Integer, List<VenusServiceConfigDO>> cacheVenusServiceConfigMap = new HashMap<Integer, List<VenusServiceConfigDO>>();

	private volatile boolean loacCacheRunning = false;

	public VenusServiceConfigDAO getVenusServiceConfigDAO() {
		return venusServiceConfigDAO;
	}

	public void setVenusServiceConfigDAO(VenusServiceConfigDAO venusServiceConfigDAO) {
		this.venusServiceConfigDAO = venusServiceConfigDAO;
	}

	public void init() {
		System.out.println("CacheVenusServiceConfigDaoImpl init()");
		GlobalScheduler.getInstance().scheduleAtFixedRate(new LoadCacheVenusServiceConfigRunnable(), 1, 5,
				TimeUnit.SECONDS);
	}

	@Override
	public List<VenusServiceConfigDO> getVenusServiceConfig(Integer serviceId) throws DAOException {
		if (loacCacheRunning) {// 缓存加载过程中，直接返回空，让从数据库中查询;
			return null;
		}
		return cacheVenusServiceConfigMap.get(serviceId);
	}

	void load() {
		loacCacheRunning = true;
		if (loacCacheRunning) {
			cacheVenusServiceConfigMap.clear();
		}
		Integer totalCount = venusServiceConfigDAO.getServiceConfigCount();
		if (null != totalCount && totalCount > 0) {
			int mod = totalCount % PAGE_SIZE_200;
			int count = totalCount / PAGE_SIZE_200;
			if (mod > 0) {
				count = count + 1;
			}
			int id = 0;
			for (int i = 0; i < count; i++) {
				List<VenusServiceConfigDO> queryVenusServiceConfigs = venusServiceConfigDAO
						.queryServiceConfigs(PAGE_SIZE_200, id);
				if (CollectionUtils.isNotEmpty(queryVenusServiceConfigs)) {
					id = queryVenusServiceConfigs.get(queryVenusServiceConfigs.size() - 1).getId();
					for (VenusServiceConfigDO serviceConfigDO : queryVenusServiceConfigs) {
						putToMap(serviceConfigDO.getServiceId(), serviceConfigDO);
					}
				}
			}
		}
		loacCacheRunning = false;
	}

	private void putToMap(Integer key, VenusServiceConfigDO vs) {
		List<VenusServiceConfigDO> list = cacheVenusServiceConfigMap.get(key);
		if (null == list) {
			list = new ArrayList<VenusServiceConfigDO>();
			list.add(vs);
			cacheVenusServiceConfigMap.put(key, list);
		} else {
			if (!list.contains(vs)) {
				list.add(vs);
			}
		}
	}

	private class LoadCacheVenusServiceConfigRunnable implements Runnable {

		@Override
		public void run() {
			try {
				long start = System.currentTimeMillis();
				load();
				long consumerTime = System.currentTimeMillis() - start;
				LogUtils.logSlow(consumerTime, "LoadCacheVenusServiceConfigRunnable load() ");
				LogUtils.DEFAULT_LOG.info(
						"LoadCacheVenusServiceConfigRunnable start=>{}, end=>{},consumerTime=>{},cacheVenusServiceConfigMap size=>{}",
						start, System.currentTimeMillis(), consumerTime, cacheVenusServiceConfigMap.size());
			} catch (Throwable e) {
				LogUtils.ERROR_LOG.error("load serviceConfigs cache data error", e);
			} finally {
				loacCacheRunning = false;
			}
		}

	}

}
