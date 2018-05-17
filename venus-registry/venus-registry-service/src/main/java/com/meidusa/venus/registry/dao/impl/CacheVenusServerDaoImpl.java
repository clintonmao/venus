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
import com.meidusa.venus.registry.dao.CacheVenusServerDAO;
import com.meidusa.venus.registry.dao.VenusServerDAO;
import com.meidusa.venus.registry.domain.VenusServerDO;

public class CacheVenusServerDaoImpl implements CacheVenusServerDAO {

	private static final int PAGE_SIZE_1000 = 1000;

	private VenusServerDAO venusServerDAO;

	private Map<String, VenusServerDO> cacheServerMap = new HashMap<String, VenusServerDO>();
	
	private Map<Integer, VenusServerDO> cacheIdServerMap = new HashMap<Integer, VenusServerDO>();

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
		return cacheServerMap.get(getKey(host, port));
	}

//	private VenusServerDO getOneServer(String host, int port) {
//		for (Iterator<VenusServerDO> iterator = cacheServers.iterator(); iterator.hasNext();) {
//			VenusServerDO server = iterator.next();
//			if (server.getHostname().equals(host)) {
//				if (server.getPort().intValue() == port) {
//					return server;
//				}
//			}
//		}
//		return null;
//	}

	private String getKey(String host, int port) {
		return host + ":" + port;
	}

	void load() {
		List<VenusServerDO> allQueryServers=new ArrayList<VenusServerDO>();
		Integer totalCount = venusServerDAO.getServerCount();
		if (null != totalCount && totalCount > 0) {
			int mod = totalCount % PAGE_SIZE_1000;
			int count = totalCount / PAGE_SIZE_1000;
			if (mod > 0) {
				count = count + 1;
			}
			int id = 0;
			for (int i = 0; i < count; i++) {
				List<VenusServerDO> queryServers = venusServerDAO.queryServers(PAGE_SIZE_1000, id);
				if (CollectionUtils.isNotEmpty(queryServers)) {
					id = queryServers.get(queryServers.size() - 1).getId();
					allQueryServers.addAll(queryServers);
				}
			}
		}
		if (CollectionUtils.isNotEmpty(allQueryServers)) {
			Map<String, VenusServerDO> localCacheServerMap = new HashMap<String, VenusServerDO>();
			Map<Integer, VenusServerDO> localCacheIdServerMap = new HashMap<Integer, VenusServerDO>();
			for (VenusServerDO serverDO : allQueryServers) {
				String key = getKey(serverDO.getHostname(), serverDO.getPort());
				localCacheServerMap.put(key, serverDO);
				localCacheIdServerMap.put(serverDO.getId(), serverDO);
			}
			
			cacheServerMap=localCacheServerMap;
			cacheIdServerMap=localCacheIdServerMap;
		}
	}

//	private boolean contains(VenusServerDO serverDO) {
//		if (CollectionUtils.isNotEmpty(cacheServers)) {
//			for (Iterator<VenusServerDO> iterator = cacheServers.iterator(); iterator.hasNext();) {
//				VenusServerDO ser = iterator.next();
//				if (ser.getHostname().equals(serverDO.getHostname())) {
//					if (null != serverDO.getPort() && (serverDO.getPort().intValue() == ser.getPort().intValue())) {
//						if (ser.getId() == serverDO.getId()) {
//							return true;
//						} else {
//							iterator.remove();// host port 相同，但id不同，删除旧的数据
//						}
//					}
//				}
//			}
//		}
//		return false;
//	}

	private class LoadCacheServersRunnable implements Runnable {

		@Override
		public void run() {
			try {
				long start = System.currentTimeMillis();
				load();
				long consumerTime = System.currentTimeMillis() - start;
				LogUtils.logCacheSlow(consumerTime, "LoadCacheServersRunnable load() ");
				LogUtils.DEFAULT_LOG.info("LoadCacheServersRunnable start=>{}, end=>{},consumerTime=>{},cacheServers size=>{}",
						start, System.currentTimeMillis(), consumerTime, cacheServerMap.size());
			} catch (Exception e) {
				LogUtils.ERROR_LOG.error("load server cache data error", e);
			}
		}

	}

	@Override
	public List<VenusServerDO> getServers(List<Integer> ids) throws DAOException {
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
