package com.meidusa.venus.registry.dao.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections.CollectionUtils;

import com.meidusa.toolkit.common.runtime.GlobalScheduler;
import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.LogUtils;
import com.meidusa.venus.registry.dao.CacheServiceConfigDAO;
import com.meidusa.venus.registry.dao.VenusServiceConfigDAO;
import com.meidusa.venus.registry.domain.VenusServiceConfigDO;

public class CacheVenusServiceConfigDaoImpl implements CacheServiceConfigDAO {

	private static final int PAGE_SIZE_1000 = 1000;

	private VenusServiceConfigDAO venusServiceConfigDAO;

	private Map<Integer, List<VenusServiceConfigDO>> cacheVenusServiceConfigMap = new HashMap<Integer, List<VenusServiceConfigDO>>();

	private AtomicInteger cacheTotalCount = new AtomicInteger(0);

	public VenusServiceConfigDAO getVenusServiceConfigDAO() {
		return venusServiceConfigDAO;
	}

	public void setVenusServiceConfigDAO(VenusServiceConfigDAO venusServiceConfigDAO) {
		this.venusServiceConfigDAO = venusServiceConfigDAO;
	}

	public void init() {
		GlobalScheduler.getInstance().scheduleAtFixedRate(new LoadCacheVenusServiceConfigRunnable(), 1, 5,
				TimeUnit.SECONDS);
	}

	@Override
	public List<VenusServiceConfigDO> getVenusServiceConfig(Integer serviceId) throws DAOException {
		return cacheVenusServiceConfigMap.get(serviceId);
	}

	void load() {
		Map<Integer, List<VenusServiceConfigDO>> localCacheVenusServiceConfigMap = new HashMap<Integer, List<VenusServiceConfigDO>>();
		Integer totalCount = venusServiceConfigDAO.getServiceConfigCount();
		if (null != totalCount && totalCount > 0) {
			int mod = totalCount % PAGE_SIZE_1000;
			int count = totalCount / PAGE_SIZE_1000;
			if (mod > 0) {
				count = count + 1;
			}
			int id = 0;
			for (int i = 0; i < count; i++) {
				List<VenusServiceConfigDO> queryVenusServiceConfigs = venusServiceConfigDAO
						.queryServiceConfigs(PAGE_SIZE_1000, id);
				if (CollectionUtils.isNotEmpty(queryVenusServiceConfigs)) {
					id = queryVenusServiceConfigs.get(queryVenusServiceConfigs.size() - 1).getId();
					for (VenusServiceConfigDO serviceConfigDO : queryVenusServiceConfigs) {
						putToMap(serviceConfigDO.getServiceId(), serviceConfigDO,localCacheVenusServiceConfigMap);
					}
				}
			}
		}
		cacheTotalCount.set(totalCount);
		cacheVenusServiceConfigMap=localCacheVenusServiceConfigMap;
	}

	private void putToMap(Integer key, VenusServiceConfigDO vs,Map<Integer, List<VenusServiceConfigDO>> localCacheVenusServiceConfigMap) {
		List<VenusServiceConfigDO> list = localCacheVenusServiceConfigMap.get(key);
		if (null == list) {
			list = new ArrayList<VenusServiceConfigDO>();
			list.add(vs);
			localCacheVenusServiceConfigMap.put(key, list);
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
				LogUtils.logCacheSlow(consumerTime, "LoadCacheVenusServiceConfigRunnable load() ");
				LogUtils.DEFAULT_LOG.info(
						"LoadCacheVenusServiceConfigRunnable consumerTime=>{},cacheVenusServiceConfigMap size=>{}",
						consumerTime, cacheVenusServiceConfigMap.size());
			} catch (Throwable e) {
				LogUtils.ERROR_LOG.error("load serviceConfigs cache data error", e);
			} 
		}

	}

	@Override
	public int getVenusServiceConfigCount() throws DAOException {
		return cacheTotalCount.get();
	}

}
