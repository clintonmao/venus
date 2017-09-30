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

/**
 * 服务注册中心清理无效数据服务
 */
public class ClearInvalidService {

	private static Logger logger = LoggerFactory.getLogger(ClearInvalidService.class);

	//服务提供方、消费方心跳上报间隔时间
	private int heartBeatInterval = 5;

	//清理定时器间隔时间
	private int clearTimerInterval = 5;

	private RegisterService registerService;

	public void init() throws Exception {
		logger.info("ClearInvalidService init ");
		clearInvalid();
		GlobalScheduler.getInstance().scheduleAtFixedRate(new ClearInvalidRunnable(), 5, clearTimerInterval, TimeUnit.SECONDS);
	}

	/**
	 * 清理操作
	 * @throws VenusRegisteException
	 */
	public void clearInvalid() throws VenusRegisteException {
		int seconds = 10 * heartBeatInterval;
		int updateSeconds = 12 * heartBeatInterval;
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

	public int getHeartBeatInterval() {
		return heartBeatInterval;
	}

	public void setHeartBeatInterval(int heartBeatInterval) {
		this.heartBeatInterval = heartBeatInterval;
	}

	private class ClearInvalidRunnable implements Runnable {

		@Override
		public void run() {
			clearInvalid();
		}

	}

}
