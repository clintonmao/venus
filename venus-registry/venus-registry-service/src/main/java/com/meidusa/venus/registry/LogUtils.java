package com.meidusa.venus.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogUtils {
	
	public static final Logger HEARTBEAT_LOG = LoggerFactory.getLogger("registry.heartbeat");
	
	public static final Logger SLOW_LOG = LoggerFactory.getLogger("registry.slow");
	
	public static final Logger DEFAULT_LOG = LoggerFactory.getLogger("registry.default");
	
	public static final Logger ERROR_LOG = LoggerFactory.getLogger("registry.error");
	
	public static final Logger CLEAR_INVALID = LoggerFactory.getLogger("registry.clearinvalid");
	
	public static final Logger MOVE_DATA_LOG = LoggerFactory.getLogger("registry.movedata");
	
	public static final Logger LOAD_SERVICE_DEF_LOG = LoggerFactory.getLogger("registry.loadservicedef");
	
	public static void logSlow(long consumerTime, String msg) {
		if (consumerTime > 200) {
			SLOW_LOG.info(msg + " consumerTime=>{}", consumerTime);
		}
	}
	
	public static void logSlow5000(long consumerTime, String msg) {
		if (consumerTime > 5000) {
			SLOW_LOG.info(msg + " consumerTime=>{}", consumerTime);
		}
	}
	
	
	
}
