package com.meidusa.venus.registry.mysql;

import com.meidusa.fastjson.JSON;
import com.meidusa.toolkit.common.runtime.GlobalScheduler;
import com.meidusa.venus.RpcException;
import com.meidusa.venus.URL;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.service.RegisterService;
import com.meidusa.venus.registry.VenusRegisteException;
import com.meidusa.venus.service.registry.ServiceDefinition;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
	private ConcurrentMap<String, ServiceDefinition> subscribleServiceDefinitionMap = new ConcurrentHashMap<String, ServiceDefinition>();

	private boolean loadRunning = false;

	private boolean heartbeatRunning = false;

	private int heartBeatSecond = 10;

	private RegisterService registerService = null;

	private String subcribePath = "/data/application/venusLocalSubcribe.txt";

	public MysqlRegister(RegisterService registerService) {
		this.registerService = registerService;
		try {
			init();
		} catch (Exception e) {
			throw new RpcException(e);
		}
	}

	/**
	 * 初始化
	 * @throws Exception
	 */
	void init() throws Exception {
		if (isWindows()) {
			subcribePath = "D:\\data\\application\\venusLocalSubcribe.txt";
		}
		if (!loadRunning) {
			clearInvalid();// TODO 注册中心单独跑这个就可以
			GlobalScheduler.getInstance().scheduleAtFixedRate(new UrlFailRunnable(), 5, 10, TimeUnit.SECONDS);
			GlobalScheduler.getInstance().scheduleAtFixedRate(new ClearInvalidRunnable(), 5, 60, TimeUnit.SECONDS); // 清理线程
			GlobalScheduler.getInstance().scheduleAtFixedRate(new ServiceDefineRunnable(), 10, 60, TimeUnit.SECONDS);
			loadRunning = true;
		}
	}

	@Override
	public void registe(URL url) throws VenusRegisteException {
		try {
			registerService.registe(url);
			heartbeat();
		} catch (Exception e) {
			registeFailUrls.add(url);
			throw new VenusRegisteException("服务注册异常" + url.getServiceName(), e);
		}
		registeUrls.add(url);

	}

	@Override
	public void unregiste(URL url) throws VenusRegisteException {
		if (StringUtils.isBlank(url.getVersion())) {
			throw new VenusRegisteException("取消注册异常" + url.getServiceName() + ",version为空");
		}
		try {
			boolean unregiste = registerService.unregiste(url);
			if (unregiste) {
				registeUrls.remove(url);
			}
		} catch (Exception e) {
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
			//FIXME 若抛异常，就不要打印error信息了，否则日志会打印两次error
			logger.error("服务{}订阅异常 ,异常原因：{}", url.getServiceName(), e);
			//FIXME 此处异常要处理，就不要抛出了，否则会中止流程
		}
		subscribleUrls.add(url);
		load();
	}

	@Override
	public void unsubscrible(URL url) throws VenusRegisteException {
		if (StringUtils.isBlank(url.getVersion())) {
			throw new VenusRegisteException("取消订阅异常" + url.getServiceName() + ",version为空");
		}
		try {
			boolean unsubscrible = registerService.unsubscrible(url);
			if (unsubscrible) {
				subscribleUrls.remove(url);
			}
		} catch (Exception e) {
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
//		 ServiceDefineRunnable run = new ServiceDefineRunnable();
//		 run.run();//测试接口时用
		// 接口名 服务名 版本号 加载服务的server信息及serviceConfig信息
		// 根据本地 ServiceDefinition 列表去查找
		String key = getKeyFromUrl(url);
		ServiceDefinition serviceDefinition = subscribleServiceDefinitionMap.get(key);
		if (null == serviceDefinition) {
			List<String> readFileJsons = readFile(subcribePath);
			Map<String, ServiceDefinition> map = new HashMap<String, ServiceDefinition>();
			if (CollectionUtils.isNotEmpty(readFileJsons)) {
				for (String str : readFileJsons) {
					ServiceDefinition parseObject = JSON.parseObject(str, ServiceDefinition.class);
					map.put(getKey(parseObject), parseObject);
				}
			}
			serviceDefinition = map.get(key);
		}
		return serviceDefinition;
	}

	private static String getKeyFromUrl(URL url) {
		String serviceName = url.getServiceName();
		String interfaceName = url.getInterfaceName();
		String version = url.getVersion();
		return serviceName + "_" + version + "_" + interfaceName;
	}

	private static String getKey(ServiceDefinition url) {
		String serviceName = url.getName();
		String interfaceName = url.getInterfaceName();
		String version = url.getVersionRange();
		return serviceName + "_" + version + "_" + interfaceName;
	}

	@Override
	public void load() throws VenusRegisteException {
		if (CollectionUtils.isNotEmpty(subscribleUrls)) {
			List<String> jsons = new ArrayList<String>();
			for (URL url : subscribleUrls) {
				String key = getKeyFromUrl(url);
				ServiceDefinition def = null;
				try {
					def = registerService.urlToServiceDefine(url);
					logger.info("srvDef:{}", def);
					subscribleServiceDefinitionMap.put(key, def);
					jsons.add(JSON.toJSONString(def));
				} catch (Exception e) {
					logger.error("服务{}ServiceDefineRunnable 运行异常 ,异常原因：{}", url.getServiceName(), e);
				}
			}
			if (CollectionUtils.isNotEmpty(jsons)) {
				writeFile(subcribePath, jsons);
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
		subscribleServiceDefinitionMap.clear();
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

	/**
	 * 读文件
	 * 
	 * @param filePath
	 * @return
	 */
	public static List<String> readFile(String filePath) {
		List<String> fileContents = new ArrayList<String>();
		if (!filePath.endsWith(".txt")) {
			return fileContents;
		}
		File file = new File(filePath);
		if (file.exists()) {
			FileReader reader = null;
			try {
				reader = new FileReader(file);
			} catch (FileNotFoundException e) {
				logger.error("readFile filePath=>" + filePath + " is error", e);
			}
			BufferedReader br = new BufferedReader(reader);
			String str = null;
			try {
				while ((str = br.readLine()) != null) {
					fileContents.add(str);
				}
			} catch (IOException e) {
				logger.error("readFile filePath=>" + filePath + " is error", e);
			} finally {
				if (null != br) {
					try {
						br.close();
					} catch (IOException e) {
						// ingore
					}
				}
				if (null != reader) {
					try {
						reader.close();
					} catch (IOException e) {
						// ingore
					}
				}
			}
		}
		return fileContents;
	}

	private static void mkDir(File file) {
		if (file.getParentFile().exists()) {
			file.mkdir();
		} else {
			mkDir(file.getParentFile());
			file.mkdir();
		}
	}

	/**
	 * 写文件(文件目录必须存在)
	 * 
	 * @param filePath
	 */
	public static void writeFile(String filePath, List<String> jsons) {
		if (filePath.endsWith(".txt")) {
			String folderPath = filePath.substring(0, filePath.lastIndexOf(File.separator));
			File f = new File(folderPath);
			mkDir(f);
		} else {
			return;
		}
		if (CollectionUtils.isEmpty(jsons)) {
			return;
		}
		List<String> readFiles = readFile(filePath);
		List<String> need_write_list = get_write_list(readFiles, jsons);
		if (CollectionUtils.isEmpty(need_write_list)) {
			return;
		}
		FileWriter fileWriter = null;
		BufferedWriter bufferWriter = null;
		try {
			File file = new File(filePath);
			if (file.createNewFile()) {
				// Runtime.getRuntime().exec("chmod 777 /home/test3.txt");
				file.setExecutable(true);
				file.setReadable(true);
				file.setWritable(true);
			}
			if (file.isFile()) {
				fileWriter = new FileWriter(file);
				bufferWriter = new BufferedWriter(fileWriter);
				for (String json : need_write_list) {
					bufferWriter.write(json);
					bufferWriter.newLine();
				}
			}
		} catch (IOException e) {
			logger.error("writeFile filePath=>" + filePath + " is error", e);
		} catch (NullPointerException e) {
			logger.error("writeFile filePath=>" + filePath + " is error", e);
		} finally {
			if (null != bufferWriter) {
				try {
					bufferWriter.close();
				} catch (IOException e) {
					// ingore
				}
			}
			if (null != fileWriter) {
				try {
					fileWriter.close();
				} catch (IOException e) {
					// ingore
				}
			}
		}
	}

	private static List<String> get_write_list(List<String> oldList, List<String> newList) {
		if (CollectionUtils.isEmpty(oldList)) {
			return newList;
		}
		List<String> returnList = new ArrayList<String>();
		for (Iterator<String> iterator = oldList.iterator(); iterator.hasNext();) {
			String json = iterator.next();
			ServiceDefinition oldObject = JSON.parseObject(json, ServiceDefinition.class);
			if (CollectionUtils.isNotEmpty(newList)) {
				for (String str : newList) {
					ServiceDefinition newObject = JSON.parseObject(str, ServiceDefinition.class);
					if (getKey(oldObject).equals(getKey(newObject))) {
						iterator.remove();
					}
				}
			}
		}
		returnList.addAll(oldList);
		returnList.addAll(newList);
		return returnList;

	}

	public static boolean isWindows() {
		String os = System.getProperty("os.name");
		return os.toLowerCase().startsWith("win");
	}

	@Override
	public List<ServiceDefinition> findServiceList(String interfaceName, String serviceName) throws VenusRegisteException {
		return registerService.getServiceDefines(interfaceName, serviceName);
	}

	/*
	 * public static void main(String args[]) {
	 * 
	 * ServiceDefinition def1 = new ServiceDefinition(); ServiceDefinition def2
	 * = new ServiceDefinition();
	 * 
	 * RouterRule rr = new RouterRule(); VenusServiceConfigDO conf = new
	 * VenusServiceConfigDO(); conf.setRouterRule(rr);
	 * 
	 * List<VenusServiceConfigDO> serviceConfigs = new
	 * ArrayList<VenusServiceConfigDO>(); serviceConfigs.add(conf);
	 * def1.setServiceConfigs(serviceConfigs);
	 * def2.setServiceConfigs(serviceConfigs); def1.setName("orderService");
	 * def2.setName("userService"); def1.setVersionRange("1.0.0");
	 * def2.setVersionRange("1.0.0");
	 * def1.setInterfaceName("com.chexiang.Orderservice");
	 * def2.setInterfaceName("com.chexiang.Userservice");
	 * 
	 * List<String> jsons = new ArrayList<String>();
	 * jsons.add(JSON.toJSONString(def1)); jsons.add(JSON.toJSONString(def2));
	 * String filePath = "D:\\soft\\b\\a.txt"; writeFile(filePath, jsons);
	 * List<String> readFile = readFile(filePath); for (String str : readFile) {
	 * System.out.println(str); } }
	 */

}
