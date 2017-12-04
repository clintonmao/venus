package com.meidusa.venus.registry.dao.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.meidusa.toolkit.common.runtime.GlobalScheduler;
import com.meidusa.venus.URL;
import com.meidusa.venus.registry.DAOException;
import com.meidusa.venus.registry.LogUtils;
import com.meidusa.venus.registry.dao.CacheVenusServiceDAO;
import com.meidusa.venus.registry.dao.VenusServiceDAO;
import com.meidusa.venus.registry.domain.VenusServerDO;
import com.meidusa.venus.registry.domain.VenusServiceDO;
import com.meidusa.venus.registry.util.RegistryUtil;

public class CacheVenusServiceDaoImpl implements CacheVenusServiceDAO {

	private VenusServiceDAO venusServiceDAO;

	private Map<String, List<VenusServiceDO>> cacheServiceMap = new HashMap<String, List<VenusServiceDO>>();

	private List<VenusServiceDO> cacheServices = new ArrayList<VenusServiceDO>();

	private static final int PAGE_SIZE_200 = 200;

	private volatile boolean loacCacheRunning = false;

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
		loacCacheRunning = true;
		if (loacCacheRunning) {
			cacheServices.clear();
			cacheServiceMap.clear();
		}
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
//						if (!cacheServices.contains(vs)) {
//							cacheServices.add(vs);
//						}
						if (isNotBlank(vs.getInterfaceName())) {
							String interfaceNamekey = RegistryUtil.getCacheKey(vs.getInterfaceName(), vs.getVersion());
							putToMap(vs, interfaceNamekey);
						}
						if (isNotBlank(vs.getName())) {
							String namekey = RegistryUtil.getCacheKey(vs.getName(), vs.getVersion());
							putToMap(vs, namekey);
						}
						String key = RegistryUtil.getCacheKey(vs);
						putToMap(vs,key);
					}
				}
			}
		}
		loacCacheRunning = false;
	}

	private void putToMap(VenusServiceDO vs,String key) {
		List<VenusServiceDO> list = cacheServiceMap.get(key);
		if (null == list) {
			list = new ArrayList<VenusServiceDO>();
			list.add(vs);
			cacheServiceMap.put(key, list);
		} else {
			if (!list.contains(vs)) {
				list.add(vs);
			}
		}
	}

	@Override
	public List<VenusServiceDO> queryServices(String interfaceName, String serviceName, String version)
			throws DAOException {
		if (StringUtils.isBlank(interfaceName) && StringUtils.isBlank(serviceName)) {
			throw new DAOException("serviceName与interfaceName不能同时为空");
		}
		if (loacCacheRunning) {// 缓存加载过程中，直接返回空，让从数据库中查询;
			return new ArrayList<VenusServiceDO>();
		}
		
		if (isNotBlank(serviceName) && isNotBlank(interfaceName)) {
			String key1=RegistryUtil.getCacheKey(interfaceName, version);
			String key2=RegistryUtil.getCacheKey(serviceName, version);
			List<VenusServiceDO> list = cacheServiceMap.get(key1);
			List<VenusServiceDO> list2 = cacheServiceMap.get(key2);
			List<VenusServiceDO> returnList=new ArrayList<VenusServiceDO>();
			if(CollectionUtils.isNotEmpty(list)){
				returnList.addAll(list);
			}
			if(CollectionUtils.isNotEmpty(list2)){
				returnList.addAll(list2);
			}
			return returnList;
		}else{
			String key=RegistryUtil.getCacheKey(interfaceName,serviceName,version);
			return cacheServiceMap.get(key);
		}
		
		
/*		List<VenusServiceDO> returnList = new ArrayList<VenusServiceDO>();

		for (Iterator<VenusServiceDO> iterator = cacheServices.iterator(); iterator.hasNext();) {
			VenusServiceDO vs = iterator.next();
			boolean nameFind = false;
			if (isNotBlank(serviceName) && isNotBlank(interfaceName)) {
				if (serviceName.equals(vs.getName()) || interfaceName.equals(vs.getInterfaceName())) {
					nameFind = true;
				}
			} else {
				if (isNotBlank(serviceName)) {
					if (serviceName.equals(vs.getName())) {
						nameFind = true;
					}
				}
				if (isNotBlank(interfaceName)) {
					if (interfaceName.equals(vs.getInterfaceName())) {
						nameFind = true;
					}
				}
			}

			if (nameFind) {// 名称匹配再看版本匹配
				if (isNotBlank(version)) {
					if (version.equals(vs.getVersion())) {
						nameFind = true;
					} else {
						nameFind = false;
					}
				}
			}

			if (nameFind) {
				returnList.add(vs);
			}
		}
		return returnList;*/

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
				LogUtils.logSlow(consumerTime, "LoadCacheServicesRunnable load() ");
				LogUtils.DEFAULT_LOG.info(
						"LoadCacheServicesRunnable start=>{}, end=>{},consumerTime=>{},cacheServices size=>{},cacheServiceMap size=>{}",
						start, end, consumerTime, cacheServices.size(), cacheServiceMap.size());
			} catch (Exception e) {
				LogUtils.ERROR_LOG.error("load service cache data error", e);
			} finally {
				loacCacheRunning = false;
			}
		}

	}

	@Override
	public List<VenusServiceDO> queryServices(URL url) throws DAOException {
		return cacheServiceMap.get(RegistryUtil.getKeyFromUrl(url));
	}
}
