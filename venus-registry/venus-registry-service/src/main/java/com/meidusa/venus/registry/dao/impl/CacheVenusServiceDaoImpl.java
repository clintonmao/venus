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
import com.meidusa.venus.registry.domain.RegisteConstant;
import com.meidusa.venus.registry.domain.VenusServiceDO;
import com.meidusa.venus.registry.util.RegistryUtil;

public class CacheVenusServiceDaoImpl implements CacheVenusServiceDAO {

	private VenusServiceDAO venusServiceDAO;

	private Map<String, List<VenusServiceDO>> cacheServiceMap = new HashMap<String, List<VenusServiceDO>>();
	
	private Map<String, List<VenusServiceDO>> nameServiceMap = new HashMap<String, List<VenusServiceDO>>();

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
			nameServiceMap.clear();
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
						if (RegistryUtil.isNotBlank(vs.getInterfaceName())) {
							String interfaceNamekey = RegistryUtil.getCacheKey(vs.getInterfaceName(), vs.getVersion());
							putToMap(interfaceNamekey, vs);
							putToNameMap(vs.getName(),vs);
						}
						if (RegistryUtil.isNotBlank(vs.getName())) {
							String namekey = RegistryUtil.getCacheKey(vs.getName(), vs.getVersion());
							putToMap(namekey, vs);
							putToNameMap(vs.getName(),vs);
						}
						String key = RegistryUtil.getCacheKey(vs);
						putToMap(key,vs);
					}
				}
			}
		}
		loacCacheRunning = false;
	}

	private void putToMap(String key,VenusServiceDO vs) {
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
	
	private void putToNameMap(String key,VenusServiceDO vs) {
		List<VenusServiceDO> list = nameServiceMap.get(key);
		if (null == list) {
			list = new ArrayList<VenusServiceDO>();
			list.add(vs);
			nameServiceMap.put(key, list);
		} else {
			if (!list.contains(vs)) {
				list.add(vs);
			}
		}
	}

	@Override
	public List<VenusServiceDO> queryServices(String interfaceName, String serviceName, String version,String role)
			throws DAOException {
		if (StringUtils.isBlank(interfaceName) && StringUtils.isBlank(serviceName)) {
			throw new DAOException("serviceName与interfaceName不能同时为空");
		}
		if (loacCacheRunning) {// 缓存加载过程中，直接返回空，让从数据库中查询;
			return new ArrayList<VenusServiceDO>();
		}
		
		if(role.equals(RegisteConstant.CONSUMER)){
			List<VenusServiceDO> returnList=new ArrayList<VenusServiceDO>();
			if (RegistryUtil.isNotBlank(serviceName)){
				List<VenusServiceDO> list= nameServiceMap.get(serviceName);
				if(CollectionUtils.isNotEmpty(list)){
					returnList.addAll(list);
				}
			}
			if (RegistryUtil.isNotBlank(interfaceName)){
				List<VenusServiceDO> list2= nameServiceMap.get(interfaceName);
				if(CollectionUtils.isNotEmpty(list2)){
					returnList.addAll(list2);
				}
			}
			return returnList;
		}else{
			if (RegistryUtil.isNotBlank(serviceName) && RegistryUtil.isNotBlank(interfaceName)) {
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
		}
	}

	private class LoadCacheServicesRunnable implements Runnable {

		@Override
		public void run() {
			try {
				long start = System.currentTimeMillis();
				load();
				long end = System.currentTimeMillis();
				long consumerTime = end - start;
				LogUtils.logCacheSlow(consumerTime, "LoadCacheServicesRunnable load() ");
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
		if (loacCacheRunning) {
			return null;
		}
		
		String serviceName = url.getServiceName();
		if(RegistryUtil.isNotBlank(serviceName)) {
			String version = url.getVersion();
			if (RegistryUtil.isNotBlank(version)) {
				return cacheServiceMap.get(RegistryUtil.getCacheKey(serviceName, version));
			}else{
				return nameServiceMap.get(serviceName);
			}
		}
		return null;
	}
	
	public List<String> queryAllServiceNames() throws DAOException {
		if (loacCacheRunning) {// 缓存加载过程中，直接返回空，让从数据库中查询;
			return new ArrayList<String>();
		}
		List<String> returnList = new ArrayList<String>();
		for (VenusServiceDO vs : cacheServices) {
			String name = vs.getName();
			if (RegistryUtil.isNotBlank(name) && !returnList.contains(name)) {
				returnList.add(name);
			}
		}
		return returnList;
	}
}
