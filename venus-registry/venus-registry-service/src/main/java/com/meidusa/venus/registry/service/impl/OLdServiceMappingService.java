package com.meidusa.venus.registry.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.meidusa.toolkit.common.runtime.GlobalScheduler;
import com.meidusa.venus.registry.LogUtils;
import com.meidusa.venus.registry.dao.OldServiceMappingDAO;
import com.meidusa.venus.registry.dao.VenusServiceMappingDAO;
import com.meidusa.venus.registry.data.move.OldServerDO;
import com.meidusa.venus.registry.data.move.OldServiceDO;
import com.meidusa.venus.registry.data.move.OldServiceMappingDO;
import com.meidusa.venus.registry.data.move.ServiceMappingDTO;
import com.meidusa.venus.registry.service.RegisterService;

public class OLdServiceMappingService {

	private static final int PAGE_SIZE_200 = 200;
	
	private static final int PAGE_SIZE_30 = 30;
	
	private static final int PAGE_SIZE_50 = 50;

	private OldServiceMappingDAO oldServiceMappingDAO;

	private VenusServiceMappingDAO venusServiceMappingDAO;

	private RegisterService registerService;

	private boolean needDataSync = false;

	public OLdServiceMappingService() {

	}

	public void init() {
		if (this.isNeedDataSync()) {
			LogUtils.MOVE_DATA_LOG.info("Sync Data Thread initialize is need");
			GlobalScheduler.getInstance().scheduleAtFixedRate(new MoveDataRunnable(), 1, 5, TimeUnit.MINUTES);
		} else {
			LogUtils.MOVE_DATA_LOG.info("Sync Data Thread initialize is not need");
		}
	}

	public void moveServiceMappings() {
		Integer totalCount = oldServiceMappingDAO.getOldServiceMappingCount();
		if (null != totalCount && totalCount > 0) {
			int mod = totalCount % PAGE_SIZE_200;
			int count = totalCount / PAGE_SIZE_200;
			if (mod > 0) {
				count = count + 1;
			}
			Integer mapId = null;
			for (int i = 0; i < count; i++) {
				List<OldServiceMappingDO> oldServiceMappings = oldServiceMappingDAO
						.queryOldServiceMappings(PAGE_SIZE_200, mapId);
				if (CollectionUtils.isNotEmpty(oldServiceMappings)) {
					mapId = oldServiceMappings.get(oldServiceMappings.size() - 1).getMapId();
					for (OldServiceMappingDO oldServiceMappingDO : oldServiceMappings) {
						registerService.addNewServiceMapping(oldServiceMappingDO.getHostName(),
								oldServiceMappingDO.getPort(), oldServiceMappingDO.getServiceName(),
								oldServiceMappingDO.getVersion(),oldServiceMappingDO.getDescription());
					}
				}
			}
		}
	}

	public void moveServices() {
		Integer totalCount = oldServiceMappingDAO.getOldServiceCount();
		if (null != totalCount && totalCount > 0) {
			int mod = totalCount % PAGE_SIZE_30;
			int count = totalCount / PAGE_SIZE_30;
			if (mod > 0) {
				count = count + 1;
			}
			Integer mapId = null;
			for (int i = 0; i < count; i++) {
				List<OldServiceDO> services = oldServiceMappingDAO.queryOldServices(PAGE_SIZE_30, mapId);
				if (CollectionUtils.isNotEmpty(services)) {
					mapId = services.get(services.size() - 1).getId();
					delOldMappingIds(services);
				}
			}
		}
	}
	
	private void delOldMappingIds(List<OldServiceDO> services) {
		List<ServiceMappingDTO> oldServiceMappings = oldServiceMappingDAO.queryOldServiceMappings(getServiceNames(services));
		List<ServiceMappingDTO> serviceMappings = venusServiceMappingDAO.queryServiceMappings(getServiceNames(services));
		Map<String, List<ServiceMappingDTO>> maps = getServiceMappingDTOs(serviceMappings);
		Map<String, List<ServiceMappingDTO>> oldMaps = getServiceMappingDTOs(oldServiceMappings);
		
		List<Integer> allDelMapIds=new ArrayList<Integer>();
		for (Map.Entry<String, List<ServiceMappingDTO>> ent : maps.entrySet()) {
			String key = ent.getKey();
			List<Integer> deleteMapIds = getDeleteIds(oldMaps.get(key),ent.getValue());
			allDelMapIds.addAll(deleteMapIds);
		}
		
		segmentDelete(allDelMapIds);
	}

	private void segmentDelete(List<Integer> allDelMapIds) {
		if (allDelMapIds.size() <= 0) {
			return;
		}
		int totalCount = allDelMapIds.size();
		int mod = totalCount % PAGE_SIZE_50;
		int count = totalCount / PAGE_SIZE_50;
		if (mod > 0) {
			count = count + 1;
		}
		for (int i = 0; i < count; i++) {
			int end = (i + 1) * PAGE_SIZE_50;
			if ((i + 1) * PAGE_SIZE_50 >= totalCount) {
				end = totalCount;
			}
			int start = i * PAGE_SIZE_50;
			List<Integer> subList = allDelMapIds.subList(start, end);
			venusServiceMappingDAO.deleteServiceMappings(subList);
		}
	}

	private void get_del_ids(List<OldServiceDO> services) {
		List<Integer> deleteMapIds = new ArrayList<Integer>();
		for (OldServiceDO oldServiceDO : services) {
			String serviceName = oldServiceDO.getServiceName();
			List<ServiceMappingDTO> oldServiceMappings = oldServiceMappingDAO.queryOldServiceMappings(serviceName);
			List<ServiceMappingDTO> serviceMappings = venusServiceMappingDAO.queryServiceMappings(serviceName);
			for (ServiceMappingDTO map : serviceMappings) {
				boolean needDel = true;
				for (ServiceMappingDTO old : oldServiceMappings) {
					if (isNotBlank(map.getHostName()) && isNotBlank(old.getHostName())) {
						if (map.getHostName().equals(old.getHostName())) {
							if (map.getPort() == old.getPort()) {
								needDel = false;
								break;
							}
						}
					}
				}

				if (needDel) {
					LogUtils.MOVE_DATA_LOG.info("mapId=>{},hostName=>{},serviceId=>{},serverId=>{},serverName=>{}", map.getMapId(),
							map.getHostName(), map.getServiceId(), map.getServerId(), map.getServiceName());
					deleteMapIds.add(map.getMapId());
				}
			}
		}
		//venusServiceMappingDAO.deleteServiceMappings(deleteMapIds);
	}

	private List<Integer> getDeleteIds(List<ServiceMappingDTO> oldServiceMappings,
			List<ServiceMappingDTO> serviceMappings) {
		List<Integer> deleteMapIds = new ArrayList<Integer>();
		if (CollectionUtils.isNotEmpty(serviceMappings)) {
			for (ServiceMappingDTO map : serviceMappings) {
				boolean needDel = true;
				if (CollectionUtils.isNotEmpty(oldServiceMappings)) {
					for (ServiceMappingDTO old : oldServiceMappings) {
						if (isNotBlank(map.getHostName()) && isNotBlank(old.getHostName())) {
							if (map.getHostName().equals(old.getHostName())) {
								if (map.getPort() == old.getPort()) {
									needDel = false;
									break;
								}
							}
						}
					}
				}
				if (needDel) {
					LogUtils.MOVE_DATA_LOG.info("@@@@mapId=>{},hostName=>{},port={},serviceId=>{},serverId=>{},serverName=>{}", map.getMapId(),
							map.getHostName(), map.getPort(),map.getServiceId(), map.getServerId(), map.getServiceName());
					deleteMapIds.add(map.getMapId());
				}
			}
		}
		return deleteMapIds;
	}

	private List<String> getServiceNames(List<OldServiceDO> services) {
		List<String> serviceNames = new ArrayList<String>();
		for (OldServiceDO oldServiceDO : services) {
			serviceNames.add(oldServiceDO.getServiceName());
		}
		return serviceNames;
	}

	private Map<String, List<ServiceMappingDTO>> getServiceMappingDTOs(List<ServiceMappingDTO> services) {
		Map<String, List<ServiceMappingDTO>> maps = new HashMap<String, List<ServiceMappingDTO>>();
		for (ServiceMappingDTO s : services) {
			String serviceName = s.getServiceName();
			String versionRange = s.getVersionRange();
			String key = serviceName;
			if (StringUtils.isNotBlank(versionRange) && !"null".equalsIgnoreCase(versionRange)) {
				key = serviceName + ":" + versionRange;
			}
			List<ServiceMappingDTO> list = maps.get(key);
			if (list == null) {
				list = new ArrayList<ServiceMappingDTO>();
				list.add(s);
				maps.put(key, list);
			} else {
				list.add(s);
			}
		}
		return maps;
	}

	public void moveServers() {
		Integer totalCount = oldServiceMappingDAO.getOldServerCount();
		if (null != totalCount && totalCount > 0) {
			int mod = totalCount % PAGE_SIZE_200;
			int count = totalCount / PAGE_SIZE_200;
			if (mod > 0) {
				count = count + 1;
			}
			Integer id = null;
			for (int i = 0; i < count; i++) {
				List<OldServerDO> servers = oldServiceMappingDAO.queryOldServers(PAGE_SIZE_200, id);
				if (CollectionUtils.isNotEmpty(servers)) {
					id = servers.get(servers.size() - 1).getId();
					for (OldServerDO oldServerDO : servers) {
						registerService.addServer(oldServerDO.getHostName(), oldServerDO.getPort());
					}
				}
				/*try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					
				}*/
			}
		}
	}

	public OldServiceMappingDAO getOldServiceMappingDAO() {
		return oldServiceMappingDAO;
	}

	public void setOldServiceMappingDAO(OldServiceMappingDAO oldServiceMappingDAO) {
		this.oldServiceMappingDAO = oldServiceMappingDAO;
	}

	public RegisterService getRegisterService() {
		return registerService;
	}

	public void setRegisterService(RegisterService registerService) {
		this.registerService = registerService;
	}

	public boolean isNeedDataSync() {
		return needDataSync;
	}

	public void setNeedDataSync(boolean needDataSync) {
		this.needDataSync = needDataSync;
	}

	public VenusServiceMappingDAO getVenusServiceMappingDAO() {
		return venusServiceMappingDAO;
	}

	public void setVenusServiceMappingDAO(VenusServiceMappingDAO venusServiceMappingDAO) {
		this.venusServiceMappingDAO = venusServiceMappingDAO;
	}

	public static boolean isNotBlank(String param) {
		return StringUtils.isNotBlank(param) && !"null".equals(param);
	}

	private class MoveDataRunnable implements Runnable {

		@Override
		public void run() {
			try {
				long start = System.currentTimeMillis();
				long firstStart=start;
				LogUtils.MOVE_DATA_LOG.info("*********MoveDataRunnable start at=>{}*********", start);
				moveServers();
				long end = System.currentTimeMillis();
				long consumerTime = end - start;
				LogUtils.logSlow5000(consumerTime, "MoveDataRunnable  moveServers() ");
				LogUtils.MOVE_DATA_LOG.info("moveServers end at=>{},consumerTime=>{}", end, consumerTime);
				
				start = System.currentTimeMillis();
				moveServiceMappings();
				end = System.currentTimeMillis();
				consumerTime = end - start;
				LogUtils.logSlow5000(consumerTime, "MoveDataRunnable  moveServiceMappings() ");
				LogUtils.MOVE_DATA_LOG.info("moveServiceMappings end at=>{},consumerTime=>{}", end, consumerTime);
				
				start = System.currentTimeMillis();
				moveServices();
				end = System.currentTimeMillis();
				consumerTime = end - start;
				LogUtils.MOVE_DATA_LOG.info("moveServices end at=>{},consumerTime=>{}", end, consumerTime);
				LogUtils.logSlow5000(consumerTime, "MoveDataRunnable  moveServices() ");
				
				LogUtils.MOVE_DATA_LOG.info("*********MoveDataRunnable end at=>{},consumerTime=>{}*************", end,(end-firstStart));
			} catch (Exception e) {
				LogUtils.ERROR_LOG.error("moveServers method is error", e);
			}
		}

	}

	/*
	 * public static void main(String args[]) { MysqlRegisterService newDs = new
	 * MysqlRegisterService(); newDs.setConnectUrl(
	 * "mysql://localhost:3306/registry_venus?username=root&password=123456");
	 * newDs.init();
	 * 
	 * OLdServiceMappingService oldDs = new OLdServiceMappingService();
	 * oldDs.setRegisterService(newDs); oldDs.setOldConnectUrl(
	 * "mysql://10.32.173.250:3306/registry?username=registry&password=registry"
	 * ); oldDs.init(); oldDs.moveServers(); }
	 */
	
}
