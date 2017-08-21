package com.meidusa.venus.registry.mysql;

import com.meidusa.toolkit.common.runtime.GlobalScheduler;
import com.meidusa.venus.URL;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.RegisterService;
import com.meidusa.venus.registry.VenusRegisteException;
import com.meidusa.venus.service.registry.ServiceDefinition;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * mysql服务注册中心类 Created by Zhangzhihua on 2017/7/27.
 */
public class MysqlRegister implements Register {

	private static Logger logger = LoggerFactory.getLogger(MysqlRegister.class);

	/** 已注册成功的URL */
	private Set<URL> registeUrls = new HashSet<URL>();

	/** 已订阅成功的URL */
	private Set<URL> subscribleUrls = new HashSet<URL>();

	/** 注册失败的URLS */
	private Set<URL> registeFailUrls = new HashSet<URL>();// 失败的继续跑启线程定时运行

	/** 订阅失败的URLS */
	private Set<URL> subscribleFailUrls = new HashSet<URL>();// 失败的继续跑启线程定时运行

	/** 已订阅成功的 服务定义对象 */
	private Set<ServiceDefinition> subscribleServiceDefinitions = new HashSet<ServiceDefinition>();

	private boolean loadRunning = false;

	private boolean heartbeatRunning = false;

	private int heartBeatSecond = 10;

	private static RegisterService registerService = null;

	private static MysqlRegister mysqlRegister = null;

	private static Random RANDOM = new Random();

	private MysqlRegister() {
		try {
			init();
		} catch (Exception e) {
			logger.error("init mysql register error.", e);
		}
	}

	/**
	 * 获取MysqlRegister
	 * 
	 * @param isInjvm
	 *            是否本地引用
	 * @param remoteRegisterService
	 *            远程引用实例
	 * @return
	 */
	public final static MysqlRegister getInstance(boolean isInjvm, RegisterService remoteRegisterService) {
		if (registerService == null) {
			registerService = initRegisterService(isInjvm, remoteRegisterService);
		}

		if (mysqlRegister == null) {
			mysqlRegister = new MysqlRegister();
		}
		return mysqlRegister;
	}

	void init() throws Exception {
		if (!loadRunning) {
			clearInvalid();// TODO 注册中心单独跑这个就可以
			GlobalScheduler.getInstance().scheduleAtFixedRate(new UrlFailRunnable(), 5, 10, TimeUnit.SECONDS);
			GlobalScheduler.getInstance().scheduleAtFixedRate(new ClearInvalidRunnable(), 5, 60, TimeUnit.SECONDS); // 清理线程
			GlobalScheduler.getInstance().scheduleAtFixedRate(new ServiceDefineRunnable(), 10, 60, TimeUnit.SECONDS);
			loadRunning = true;
		}
	}

	/**
	 * 初始化register service
	 * 
	 * @param isInjvm
	 * @param remoteRegisterService
	 * @return
	 */
	static RegisterService initRegisterService(boolean isInjvm, RegisterService remoteRegisterService) {
		if (!isInjvm && remoteRegisterService == null) {
			throw new IllegalArgumentException("isInjvm and registerService not allow empty.");
		}

		if (isInjvm) {
			RegisterService localRegisterService = null;
			try {
				localRegisterService = (RegisterService) Class
						.forName("com.meidusa.venus.registry.service.MysqlRegisterService").newInstance();
			} catch (Exception e) {
				logger.error("new MysqlRegisterService failed.", e);
				return null;
			}
			return localRegisterService;
		} else {
			return remoteRegisterService;
		}
	}

	@Override
	public void registe(URL url) throws VenusRegisteException {
		try {
			registerService.registe(url);
			heartbeat();
		} catch (Exception e) {
			registeFailUrls.add(url);
			logger.error("服务{}注册异常,异常原因：{} ", url.getServiceName(), e);
			throw new VenusRegisteException("服务注册异常" + url.getServiceName(), e);
		}
		registeUrls.add(url);

	}

	@Override
	public void unregiste(URL url) throws VenusRegisteException {
		if (StringUtils.isBlank(url.getVersion())) {
			logger.error("服务{}取消注册异常,异常原因：{} ", url.getServiceName(), "version为空");
			throw new VenusRegisteException("取消注册异常" + url.getServiceName() + ",version为空");
		}
		try {
			boolean unregiste = registerService.unregiste(url);
			if (unregiste) {
				registeUrls.remove(url);
			}
		} catch (Exception e) {
			logger.error("服务{}取消注册异常,异常原因：{} ", url.getServiceName(), e);
			throw new VenusRegisteException("取消注册异常" + url.getServiceName(), e);
		}
	}

	@Override
	public void subscrible(URL url) throws VenusRegisteException {
		try {
			registerService.subscrible(url);
			heartbeat();
		} catch (Exception e) {
			subscribleFailUrls.add(url);
			logger.error("服务{}订阅异常 ,异常原因：{}", url.getServiceName(), e);
			throw new VenusRegisteException("服务订阅异常" + url.getServiceName(), e);
		}
		subscribleUrls.add(url);
		load();
	}

	@Override
	public void unsubscrible(URL url) throws VenusRegisteException {
		if (StringUtils.isBlank(url.getVersion())) {
			logger.error("服务{}取消订阅异常,异常原因：{} ", url.getServiceName(), "version为空");
			throw new VenusRegisteException("取消订阅异常" + url.getServiceName() + ",version为空");
		}
		try {
			boolean unsubscrible = registerService.unsubscrible(url);
			if (unsubscrible) {
				subscribleUrls.remove(url);
			}
		} catch (Exception e) {
			logger.error("服务{}取消订阅异常 ,异常原因：{}", url.getServiceName(), e);
			throw new VenusRegisteException("取消订阅异常" + url.getServiceName(), e);
		}
		load();
	}

	@Override
	public void heartbeat() throws VenusRegisteException {
		if (!heartbeatRunning) {
			GlobalScheduler.getInstance().scheduleAtFixedRate(new HeartBeatRunnable(), 10, heartBeatSecond,
					TimeUnit.SECONDS);
			heartbeatRunning = true;
		}
	}

	@Override
	public void clearInvalid() throws VenusRegisteException {
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

	@Override
	public ServiceDefinition lookup(URL url) throws VenusRegisteException {
		// ServiceDefineRunnable run = new ServiceDefineRunnable();
		// run.run();//测试接口时用
		// 接口名 服务名 版本号 加载服务的server信息及serviceConfig信息
		// 根据本地 ServiceDefinition 列表去查找
		String serviceName = url.getServiceName();
		String interfaceName = url.getInterfaceName();
		String version = url.getVersion();
		synchronized (MysqlRegister.class) {
			for (Iterator<ServiceDefinition> iterator = subscribleServiceDefinitions.iterator(); iterator.hasNext();) {
				ServiceDefinition define = iterator.next();
				if (null != define && define.getName().equals(serviceName)) {
					if (version.equals(define.getVersionRange())) {// TODO
																	// version
						return define;
					}
				}
			}
		}
		return null;
	}

	@Override
	public void load() throws VenusRegisteException {
		if (CollectionUtils.isNotEmpty(subscribleUrls)) {
			Set<ServiceDefinition> tempSet = new HashSet<ServiceDefinition>();
			for (URL url : subscribleUrls) {
				ServiceDefinition def = null;
				try {
					def = registerService.urlToServiceDefine(url);
					logger.info("srvDef:{}", def);
				} catch (Exception e) {
					logger.error("服务{}ServiceDefineRunnable 运行异常 ,异常原因：{}", url.getServiceName(), e);
				}
				if (null != def) {
					tempSet.add(def);
				}
			}
			synchronized (MysqlRegister.class) {
				subscribleServiceDefinitions.clear();
				subscribleServiceDefinitions.addAll(tempSet);
			}
		}
	}

	@Override
	public void destroy() throws VenusRegisteException {
		if (CollectionUtils.isNotEmpty(registeUrls)) {
			for (URL url : registeUrls) {
				unregiste(url);
			}
		}
		if (CollectionUtils.isNotEmpty(subscribleUrls)) {
			for (URL url : subscribleUrls) {
				unsubscrible(url);
			}
		}
		registeUrls.clear();
		subscribleUrls.clear();
		registeFailUrls.clear();
		subscribleFailUrls.clear();
		synchronized (MysqlRegister.class) {
			subscribleServiceDefinitions.clear();
		}
	}

	private class ServiceDefineRunnable implements Runnable {
		public void run() {
			load();
		}

	}

	private class HeartBeatRunnable implements Runnable {
		@Override
		public void run() {
			if (CollectionUtils.isNotEmpty(registeUrls)) {
				for (Iterator<URL> iterator = registeUrls.iterator(); iterator.hasNext();) {
					URL url = iterator.next();
					try {
						registerService.heartbeatRegister(url);
					} catch (Exception e) {
						logger.error("服务{}registe更新heartBeatTime异常 ,异常原因：{}", url.getServiceName(), e);
					}
				}
			}
			if (CollectionUtils.isNotEmpty(subscribleUrls)) {
				for (Iterator<URL> iterator = subscribleUrls.iterator(); iterator.hasNext();) {
					URL url = iterator.next();
					try {
						registerService.heartbeatSubcribe(url);
					} catch (Exception e) {
						logger.error("服务{}subscrible更新heartBeatTime异常 ,异常原因：{}", url.getServiceName(), e);
					}
				}
			}
		}
	}

	private class UrlFailRunnable implements Runnable {
		@Override
		public void run() {
			if (CollectionUtils.isNotEmpty(registeFailUrls)) {
				for (Iterator<URL> iterator = registeFailUrls.iterator(); iterator.hasNext();) {
					URL url = iterator.next();
					try {
						registe(url);
						iterator.remove();
					} catch (Exception e) {
						logger.error("Fail服务{}重新注册异常 ,异常原因：{}", url.getServiceName(), e);
					}
				}
			}
			if (CollectionUtils.isNotEmpty(subscribleFailUrls)) {
				for (Iterator<URL> iterator = subscribleFailUrls.iterator(); iterator.hasNext();) {
					URL url = iterator.next();
					try {
						subscrible(url);
						iterator.remove();
					} catch (Exception e) {
						logger.error("Fail服务{}重新订阅异常 ,异常原因：{}", url.getServiceName(), e);
					}
				}
			}

		}

	}

	private class ClearInvalidRunnable implements Runnable {

		@Override
		public void run() {
			clearInvalid();
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
		MysqlRegister.registerService = registerService;
	}

}
