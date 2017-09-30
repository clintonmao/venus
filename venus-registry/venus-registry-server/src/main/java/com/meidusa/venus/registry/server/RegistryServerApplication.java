package com.meidusa.venus.registry.server;

import com.meidusa.toolkit.common.runtime.Application;
import com.meidusa.toolkit.common.runtime.ApplicationConfig;

public class RegistryServerApplication extends Application<ApplicationConfig> {

	@Override
	public void doRun() {

	}

	@Override
	public ApplicationConfig getApplicationConfig() {
		return null;
	}

	@Override
	protected String[] getConfigLocations() {
		return new String[] { "classpath:/conf/application-venus-server.xml" };
	}

	public static void main(String[] args) {
		System.setProperty(ApplicationConfig.PROJECT_MAINCLASS, RegistryServerApplication.class.getName());
		Application.main(args);
	}
}