package com.meidusa.venus.registry.service.impl;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import com.meidusa.venus.registry.LogUtils;
import com.meidusa.venus.registry.dao.VenusApplicationDAO;
import com.meidusa.venus.registry.dao.VenusServiceDAO;
import com.meidusa.venus.registry.domain.VenusApplicationDO;
import com.meidusa.venus.registry.domain.VenusServiceDO;

public class SyncApplicationisNewService {

	private VenusServiceDAO venusServiceDAO;

	private VenusApplicationDAO venusApplicationDAO;

	private static final int PAGE_SIZE_200 = 200;

	private volatile boolean loacCacheRunning = false;

	public VenusApplicationDAO getVenusApplicationDAO() {
		return venusApplicationDAO;
	}

	public void setVenusApplicationDAO(VenusApplicationDAO venusApplicationDAO) {
		this.venusApplicationDAO = venusApplicationDAO;
	}

	public VenusServiceDAO getVenusServiceDAO() {
		return venusServiceDAO;
	}

	public void setVenusServiceDAO(VenusServiceDAO venusServiceDAO) {
		this.venusServiceDAO = venusServiceDAO;
	}

	public void init() {
		load();
		//GlobalScheduler.getInstance().scheduleAtFixedRate(new LoadCacheServicesRunnable(), 1, 20, TimeUnit.HOURS);
	}

	public void load() {
		loacCacheRunning = true;
		if (loacCacheRunning) {

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
						String serviceName = vs.getName();
						String oldAppCode = serviceName + "_app";
						VenusApplicationDO application = venusApplicationDAO.getApplication(oldAppCode);
						if(null!=application){
							venusApplicationDAO.updateApplication(false, application.getId());
							System.out.println("application.getId()"+application.getId()+"is update");
							LogUtils.DEFAULT_LOG.error("application.getId()"+application.getId()+"is update");
						}
					}
				}
			}
		}
		loacCacheRunning = false;
	}

//	private class LoadCacheServicesRunnable implements Runnable {
//
//		@Override
//		public void run() {
//			try {
//				long start = System.currentTimeMillis();
//				load();
//				long end = System.currentTimeMillis();
//				long consumerTime = end - start;
//			} catch (Exception e) {
//				LogUtils.ERROR_LOG.error("load service cache data error", e);
//			} finally {
//				loacCacheRunning = false;
//			}
//		}
//
//	}

}
