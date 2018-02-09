package com.athena.venus.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * kafka venus上报对象
 * @author longhaisheng
 *
 */
public class VenusReportDO implements Serializable{

	private static final long serialVersionUID = -3639068585764080577L;
	
	List<VenusMethodStaticDO> methodStaticDOs=new ArrayList<VenusMethodStaticDO>();
	
	List<VenusMethodCallDetailDO> methodCallDetailDOs=new ArrayList<VenusMethodCallDetailDO>();

	public List<VenusMethodStaticDO> getMethodStaticDOs() {
		return methodStaticDOs;
	}

	public void setMethodStaticDOs(List<VenusMethodStaticDO> methodStaticDOs) {
		this.methodStaticDOs = methodStaticDOs;
	}

	public List<VenusMethodCallDetailDO> getMethodCallDetailDOs() {
		return methodCallDetailDOs;
	}

	public void setMethodCallDetailDOs(List<VenusMethodCallDetailDO> methodCallDetailDOs) {
		this.methodCallDetailDOs = methodCallDetailDOs;
	}
	
	

}
