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

public class ClearInvalidService {

	private static Logger logger = LoggerFactory.getLogger(ClearInvalidService.class);

	private int heartBeatSecond = 10;

	private RegisterService registerService;

	public void init() throws Exception {
		clearInvalid();
		GlobalScheduler.getInstance().scheduleAtFixedRate(new ClearInvalidRunnable(), 5, 60, TimeUnit.SECONDS);
	}

	public void clearInvalid() throws VenusRegisteException {
		logger.info("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
		int seconds = 10 * heartBeatSecond;
		int updateSeconds = 12 * heartBeatSecond;
		try {
			Date date = getSubSecond(new Date(), seconds);
			Date updateDate = getSubSecond(new Date(), updateSeconds);
			DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String currentDateTime = format.format(date);
			String updateTime = format.format(updateDate);
			registerService.clearInvalidService(currentDateTime, updateTime);
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

	public int getHeartBeatSecond() {
		return heartBeatSecond;
	}

	public void setHeartBeatSecond(int heartBeatSecond) {
		this.heartBeatSecond = heartBeatSecond;
	}

	private class ClearInvalidRunnable implements Runnable {

		@Override
		public void run() {
			clearInvalid();
		}

	}

}
