package com.meidusa.venus.registry.data.move;

import java.io.Serializable;
import java.util.List;

import com.meidusa.venus.URL;
import com.meidusa.venus.registry.domain.VenusServerDO;

public class UpdateHeartBeatTimeDTO implements Serializable {

	private static final long serialVersionUID = -4957292074836153116L;

	private int serverId;

	private List<Integer> serviceIds;

	private String role;
	
	private VenusServerDO serverDO;
	
	public int getServerId() {
		return serverId;
	}

	public void setServerId(int serverId) {
		this.serverId = serverId;
	}

	public List<Integer> getServiceIds() {
		return serviceIds;
	}

	public void setServiceIds(List<Integer> serviceIds) {
		this.serviceIds = serviceIds;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public VenusServerDO getServerDO() {
		return serverDO;
	}

	public void setServerDO(VenusServerDO serverDO) {
		this.serverDO = serverDO;
	}
	
}