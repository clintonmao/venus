package com.meidusa.venus.registry.dao.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;

import com.meidusa.toolkit.common.runtime.GlobalScheduler;
import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.LogUtils;
import com.meidusa.venus.registry.dao.CacheVenusServerDAO;
import com.meidusa.venus.registry.dao.VenusServerDAO;
import com.meidusa.venus.registry.domain.VenusServerDO;

public class CacheVenusServerDaoImpl implements CacheVenusServerDAO {

	private static final int PAGE_SIZE_200 = 200;

	private VenusServerDAO venusServerDAO;

	private List<VenusServerDO> cacheServers = new ArrayList<VenusServerDO>();

	private Map<String, VenusServerDO> cacheServerMap = new HashMap<String, VenusServerDO>();
	
	private Map<Integer, VenusServerDO> cacheIdServerMap = new HashMap<Integer, VenusServerDO>();

	private volatile boolean loacCacheRunning = false;

	public VenusServerDAO getVenusServerDAO() {
		return venusServerDAO;
	}

	public void setVenusServerDAO(VenusServerDAO venusServerDAO) {
		this.venusServerDAO = venusServerDAO;
	}

	public void init() {
		GlobalScheduler.getInstance().scheduleAtFixedRate(new LoadCacheServersRunnable(), 1, 2, TimeUnit.SECONDS);
	}

	@Override
	public VenusServerDO getServer(String host, int port) throws DAOException {
		if (loacCacheRunning) {// 缓存加载过程中，直接返回空，让从数据库中查询;
			return null;
		}
		return cacheServerMap.get(getKey(host, port));
	}

	private VenusServerDO getOneServer(String host, int port) {
		for (Iterator<VenusServerDO> iterator = cacheServers.iterator(); iterator.hasNext();) {
			VenusServerDO server = iterator.next();
			if (server.getHostname().equals(host)) {
				if (server.getPort().intValue() == port) {
					return server;
				}
			}
		}
		return null;
	}

	private String getKey(String host, int port) {
		return host + ":" + port;
	}

	void load() {
/*		loacCacheRunning = true;
		if (loacCacheRunning) {
			cacheServerMap.clear();
			cacheIdServerMap.clear();
		}*/
		
		List<VenusServerDO> allQueryServers=new ArrayList<VenusServerDO>();
		Integer totalCount = venusServerDAO.getServerCount();
		if (null != totalCount && totalCount > 0) {
			int mod = totalCount % PAGE_SIZE_200;
			int count = totalCount / PAGE_SIZE_200;
			if (mod > 0) {
				count = count + 1;
			}
			int id = 0;
			for (int i = 0; i < count; i++) {
				List<VenusServerDO> queryServers = venusServerDAO.queryServers(PAGE_SIZE_200, id);
				if (CollectionUtils.isNotEmpty(queryServers)) {
					id = queryServers.get(queryServers.size() - 1).getId();
					allQueryServers.addAll(queryServers);
				}
			}
		}
		if (CollectionUtils.isNotEmpty(allQueryServers)) {
			loacCacheRunning = true;
			cacheServerMap.clear();
			cacheIdServerMap.clear();
			for (VenusServerDO serverDO : allQueryServers) {
				String key = getKey(serverDO.getHostname(), serverDO.getPort());
				cacheServerMap.put(key, serverDO);
				cacheIdServerMap.put(serverDO.getId(), serverDO);
			}
			loacCacheRunning = false;
		}
	}

	private boolean contains(VenusServerDO serverDO) {
		if (CollectionUtils.isNotEmpty(cacheServers)) {
			for (Iterator<VenusServerDO> iterator = cacheServers.iterator(); iterator.hasNext();) {
				VenusServerDO ser = iterator.next();
				if (ser.getHostname().equals(serverDO.getHostname())) {
					if (null != serverDO.getPort() && (serverDO.getPort().intValue() == ser.getPort().intValue())) {
						if (ser.getId() == serverDO.getId()) {
							return true;
						} else {
							iterator.remove();// host port 相同，但id不同，删除旧的数据
						}
					}
				}
			}
		}
		return false;
	}

	private class LoadCacheServersRunnable implements Runnable {

		@Override
		public void run() {
			try {
				long start = System.currentTimeMillis();
				load();
				long consumerTime = System.currentTimeMillis() - start;
				LogUtils.logCacheSlow(consumerTime, "LoadCacheServersRunnable load() ");
				LogUtils.DEFAULT_LOG.info("LoadCacheServersRunnable start=>{}, end=>{},consumerTime=>{},cacheServers size=>{}",
						start, System.currentTimeMillis(), consumerTime, cacheServers.size());
			} catch (Exception e) {
				LogUtils.ERROR_LOG.error("load server cache data error", e);
			} finally {
				loacCacheRunning = false;
			}
		}

	}

	@Override
	public List<VenusServerDO> getServers(List<Integer> ids) throws DAOException {
		if (loacCacheRunning) {// 缓存加载过程中，直接返回空，让从数据库中查询;
			return null;
		}
		List<VenusServerDO> returnList = new ArrayList<VenusServerDO>();
		for (Integer id : ids) {
			VenusServerDO venusServerDO = cacheIdServerMap.get(id);
			if (null != venusServerDO) {
				returnList.add(venusServerDO);
			}
		}
		return returnList;
	}

}
