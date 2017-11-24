package com.meidusa.venus.registry.dao.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.meidusa.toolkit.common.runtime.GlobalScheduler;
import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.dao.CacheVenusServerDAO;
import com.meidusa.venus.registry.dao.VenusServerDAO;
import com.meidusa.venus.registry.domain.VenusServerDO;

public class CacheVenusServerDaoImpl implements CacheVenusServerDAO {

	private static final int PAGE_SIZE_200 = 200;

	private VenusServerDAO venusServerDAO;

	private static Logger logger = LoggerFactory.getLogger(CacheVenusServerDaoImpl.class);

	private List<VenusServerDO> cacheServers = new ArrayList<VenusServerDO>();

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

	void load() {
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
					for (VenusServerDO serverDO : queryServers) {
						boolean contains = contains(serverDO);
						if (!contains) {
							cacheServers.add(serverDO);
						}
					}
				}
			}
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
				long end = System.currentTimeMillis();
				long consumerTime = end - start;
				logger.error("LoadCacheServersRunnable start=>{}, end=>{},consumerTime=>{},cacheServers size=>{}", start,
						end, consumerTime, cacheServers.size());
			} catch (Exception e) {
				logger.error("load server cache data error", e);
			}
		}

	}

}
