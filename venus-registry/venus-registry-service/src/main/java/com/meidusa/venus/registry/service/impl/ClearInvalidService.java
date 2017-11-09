package com.meidusa.venus.registry.service.impl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.meidusa.toolkit.common.runtime.GlobalScheduler;
import com.meidusa.venus.registry.VenusRegisteException;
import com.meidusa.venus.registry.service.RegisterService;
import com.meidusa.venus.support.VenusConstants;

/**
 * 服务注册中心清理无效数据服务
 */
public class ClearInvalidService {

	private static Logger logger = LoggerFactory.getLogger(ClearInvalidService.class);

	private RegisterService registerService;

	public void init() throws Exception {
		logger.info("ClearInvalidService init ");
		clearInvalid();
		GlobalScheduler.getInstance().scheduleAtFixedRate(new ClearInvalidRunnable(), 5, 5, TimeUnit.SECONDS);
	}

	/**
	 * 清理操作
	 * @throws VenusRegisteException
	 */
	public void clearInvalid() throws VenusRegisteException {
		int seconds = VenusConstants.LOGIC_DEL_INVALID_SERVICE_TIME;
		try {
			Date date = getSubSecond(new Date(), seconds);
			DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String currentDateTime = format.format(date);
			registerService.clearInvalidService(currentDateTime, seconds);
		} catch (Exception e) {
			logger.error("ClearInvalidRunnable is error", e);
		}
	}

	public static final Date getSubSecond(Date date, int second) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.SECOND, -second);
		return calendar.getTime();
	}

	public RegisterService getRegisterService() {
		return registerService;
	}

	public void setRegisterService(RegisterService registerService) {
		this.registerService = registerService;
	}

	private class ClearInvalidRunnable implements Runnable {

		@Override
		public void run() {
			clearInvalid();
		}

	}

}
