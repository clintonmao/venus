package com.meidusa.venus.bus;

import com.meidusa.toolkit.common.runtime.Application;
import com.meidusa.toolkit.common.runtime.ApplicationConfig;

/**
 * 启动 BUS的 Application
 * 
 * @author structchen
 * 
 */
public class BusApplication extends Application<ApplicationConfig> {

    @Override
    public void doRun() {
    }

    @Override
    protected String[] getConfigLocations() {
        return new String[]{"classpath:conf/applicationContext-bus.xml"};
    }

    @Override
    public ApplicationConfig getApplicationConfig() {
        return null;
    }

    public static void main(String[] args) {
        System.setProperty(ApplicationConfig.PROJECT_MAINCLASS, BusApplication.class.getName());
        Application.main(args);
    }
}
