package com.meidusa.venus.registry.data.move;

import java.io.Serializable;
import java.util.List;

public class UpdateHeartBeatTimeDTO implements Serializable {

	private static final long serialVersionUID = -4957292074836153116L;

	public int serverId;

	public List<Integer> serviceIds;

	public String role;

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

}