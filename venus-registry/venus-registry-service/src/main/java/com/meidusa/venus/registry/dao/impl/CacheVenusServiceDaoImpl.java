package com.meidusa.venus.registry.dao.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.meidusa.toolkit.common.runtime.GlobalScheduler;
import com.meidusa.venus.URL;
import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.dao.CacheVenusServiceDAO;
import com.meidusa.venus.registry.dao.VenusServiceDAO;
import com.meidusa.venus.registry.domain.VenusServiceDO;
import com.meidusa.venus.registry.util.RegistryUtil;

public class CacheVenusServiceDaoImpl implements CacheVenusServiceDAO {

	private VenusServiceDAO venusServiceDAO;

	private Map<String, List<VenusServiceDO>> cacheServiceMap = new HashMap<String, List<VenusServiceDO>>();

	private List<VenusServiceDO> cacheServices = new ArrayList<VenusServiceDO>();

	private static final int PAGE_SIZE_200 = 200;

	private static Logger logger = LoggerFactory.getLogger(CacheVenusServiceDaoImpl.class);

	public VenusServiceDAO getVenusServiceDAO() {
		return venusServiceDAO;
	}

	public void setVenusServiceDAO(VenusServiceDAO venusServiceDAO) {
		this.venusServiceDAO = venusServiceDAO;
	}

	public void init() {
		GlobalScheduler.getInstance().scheduleAtFixedRate(new LoadCacheServicesRunnable(), 1, 10, TimeUnit.SECONDS);
	}

	public void load() {
		Integer totalCount = venusServiceDAO.getServiceCount();
		if (null != totalCount && totalCount > 0) {
			int mod = totalCount % PAGE_SIZE_200;
			int count = totalCount / PAGE_SIZE_200;
			if (mod > 0) {
				count = count + 1;
			}
			int mapId = 0;
			for (int i = 0; i < count; i++) {
				List<VenusServiceDO> services = venusServiceDAO.queryServices(PAGE_SIZE_200, mapId);
				if (CollectionUtils.isNotEmpty(services)) {
					mapId = services.get(services.size() - 1).getId();
					for (Iterator<VenusServiceDO> iterator = services.iterator(); iterator.hasNext();) {
						VenusServiceDO vs = iterator.next();
						 if (!cacheServices.contains(vs)) {
							 cacheServices.add(vs);
						 }
//						String key = RegistryUtil.getKey(vs);
//						List<VenusServiceDO> list = cacheServiceMap.get(key);
//						if (null == list) {
//							list = new ArrayList<VenusServiceDO>();
//							list.add(vs);
//							cacheServiceMap.put(key, list);
//						} else {
//							if (!list.contains(vs)) {
//								list.add(vs);
//							}
//						}
//						if (list.size() > 1) {
//							logger.error("more than 2 service {}", vs);
//						}
					}
				}
			}
		}

	}

	@Override
	public List<VenusServiceDO> queryServices(String interfaceName, String serviceName, String version)
			throws DAOException {
		if (StringUtils.isBlank(interfaceName) && StringUtils.isBlank(serviceName)) {
			throw new DAOException("serviceName与interfaceName不能同时为空");
		}

		List<VenusServiceDO> returnList = new ArrayList<VenusServiceDO>();

		for (Iterator<VenusServiceDO> iterator = cacheServices.iterator(); iterator.hasNext();) {
			VenusServiceDO vs = iterator.next();
			boolean isFind = false;
			if (isNotBlank(serviceName) && isNotBlank(interfaceName)) {
				if (serviceName.equals(vs.getName()) || interfaceName.equals(vs.getInterfaceName())) {
					isFind = true;
				}
			} else {
				if (isNotBlank(serviceName)) {
					if (serviceName.equals(vs.getName())) {
						isFind = true;
					}
				}
				if (isNotBlank(interfaceName)) {
					if (interfaceName.equals(vs.getInterfaceName())) {
						isFind = true;
					}
				}
			}

			if (isNotBlank(version)) {
				if (version.equals(vs.getVersion())) {
					isFind = true;
				} else {
					isFind = false;
				}
			}

			if (isFind) {
				returnList.add(vs);
			}
		}
		return returnList;

	}

	public static boolean isNotBlank(String param) {
		return StringUtils.isNotBlank(param) && !"null".equals(param);
	}

	private class LoadCacheServicesRunnable implements Runnable {

		@Override
		public void run() {
			try {
				long start = System.currentTimeMillis();
				load();
				long end = System.currentTimeMillis();
				long consumerTime = end - start;
				logger.error("LoadCacheServicesRunnable start=>{}, end=>{},consumerTime=>{},cacheServiceMap size=>{}",
						start, end, consumerTime, cacheServiceMap.size());
			} catch (Exception e) {
				logger.error("load service cache data error", e);

			}
		}

	}

	@Override
	public List<VenusServiceDO> queryServices(URL url) throws DAOException {
		return cacheServiceMap.get(RegistryUtil.getKeyFromUrl(url));
	}
}
