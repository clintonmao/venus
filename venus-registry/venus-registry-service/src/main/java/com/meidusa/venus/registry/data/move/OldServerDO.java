package com.meidusa.venus.registry.data.move;

import java.io.Serializable;
import java.util.Date;

public class OldServerDO implements Serializable {

	private static final long serialVersionUID = 5855659524793557821L;

	private int id;

	/** 主机地址如192.168.2.10 */
	private String hostName;

	/** 主机端口 */
	private Integer port;

	private Date createTime;

	private Date updateTime;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	@Override
	public String toString() {
		return "OldServerDO [id=" + id + ", hostName=" + hostName + ", port=" + port + ", createTime=" + createTime
				+ ", updateTime=" + updateTime + "]";
	}

}
