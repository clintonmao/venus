package com.meidusa.venus.support;

import com.meidusa.venus.util.VenusLoggerFactory;
import org.slf4j.Logger;

import java.util.Map;

/**
 * 资源监控facade，屏蔽实现
 * Created by Zhangzhihua on 2017/11/19.
 */
public class MonitorResourceFacade implements MonitorResource {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    private static MonitorResourceFacade monitorResourceFacade;

    private MonitorResource defaultMonitorResource = null;

    private MonitorResourceFacade(){
        initDefaultMonitorResource();
    }

    public static MonitorResourceFacade getInstance(){
        if(monitorResourceFacade == null){
            monitorResourceFacade = new MonitorResourceFacade();
        }
        return monitorResourceFacade;
    }

    @Override
    public void init() {
        try {
            if(defaultMonitorResource != null){
                defaultMonitorResource.init();
            }
        } catch (Exception e) {
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error("init failed.",e);
            }
        }
    }

    @Override
    public void addProperty(String key, Object object) {
        try {
            if(defaultMonitorResource != null){
                defaultMonitorResource.addProperty(key, object);
            }
        } catch (Exception e) {
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error("addProperty failed.",e);
            }
        }
    }

    @Override
    public Object getProperty(String key) {
        try {
            if(defaultMonitorResource != null){
                return defaultMonitorResource.getProperty(key);
            }
            return null;
        } catch (Exception e) {
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error("getProperty failed.",e);
            }
            return null;
        }
    }

    @Override
    public Map<String, Object> getAllProperties() {
        try {
            if(defaultMonitorResource != null){
                return defaultMonitorResource.getAllProperties();
            }
            return null;
        } catch (Exception e) {
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error("getAllProperties failed.",e);
            }
            return null;
        }
    }

    /**
     * 初始化监控resource
     */
    void initDefaultMonitorResource(){
        try {
            Class clz = Class.forName("com.meidusa.venus.manager.service.DefaultMonitorResource");
            Object obj  = clz.newInstance();
            if(obj != null){
                this.defaultMonitorResource = (MonitorResource)obj;
            }
        } catch (Exception e) {
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error("load resource failed.",e);
            }
        }
    }
}
