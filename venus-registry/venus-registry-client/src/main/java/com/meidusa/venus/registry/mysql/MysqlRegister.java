package com.meidusa.venus.registry.mysql;

import com.meidusa.fastjson.JSON;
import com.meidusa.toolkit.common.runtime.GlobalScheduler;
import com.meidusa.venus.RpcException;
import com.meidusa.venus.URL;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.domain.VenusServiceDefinitionDO;
import com.meidusa.venus.registry.service.RegisterService;
import com.meidusa.venus.registry.VenusRegisteException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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
	private ConcurrentMap<String, List<VenusServiceDefinitionDO>> subscribleServiceDefinitionMap = new ConcurrentHashMap<String, List<VenusServiceDefinitionDO>>();

	private boolean loadRunning = false;

	private boolean heartbeatRunning = false;

	//心跳间隔时间,单位m
	private int heartBeatInterval = 5;

	//服务定义加载间隔时间
	private int srvDefLoaderInterval = 10;

	//失败重试间隔时间
	private int failRetryInterval = 30;

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
			GlobalScheduler.getInstance().scheduleAtFixedRate(new UrlFailRunnable(), 10, failRetryInterval, TimeUnit.SECONDS);
			GlobalScheduler.getInstance().scheduleAtFixedRate(new ServiceDefLoaderRunnable(), 10, srvDefLoaderInterval, TimeUnit.SECONDS);
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
			logger.error("服务{}订阅异常 ,异常原因：{}", url.getServiceName(), e);
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
			GlobalScheduler.getInstance().scheduleAtFixedRate(new HeartBeatRunnable(), 5, heartBeatInterval,
					TimeUnit.SECONDS);
			heartbeatRunning = true;
		}
	}

	@Override
	public List<VenusServiceDefinitionDO> lookup(URL url) throws VenusRegisteException {
		// 接口名 服务名 版本号 加载服务的server信息及serviceConfig信息
		// 根据本地 VenusServiceDefinitionDO 列表去查找
		String key = getKeyFromUrl(url);
		List<VenusServiceDefinitionDO> serviceDefinitions = subscribleServiceDefinitionMap.get(key);
		if (CollectionUtils.isEmpty(serviceDefinitions)) {
			List<String> readFileJsons = readFile(subcribePath);
			Map<String, List<VenusServiceDefinitionDO>> map = new HashMap<String, List<VenusServiceDefinitionDO>>();
			if (CollectionUtils.isNotEmpty(readFileJsons)) {
				for (String str : readFileJsons) {
					List<VenusServiceDefinitionDO> parseObject = JSON.parseArray(str, VenusServiceDefinitionDO.class);
					if (CollectionUtils.isNotEmpty(parseObject)) {
						map.put(getKey(parseObject.get(0)), parseObject);
					}
				}
			}
			serviceDefinitions = map.get(key);
		}
		return serviceDefinitions;
	}

	@Override
	public List<VenusServiceDefinitionDO> lookup(URL url, boolean isQueryFromRegister) throws VenusRegisteException {
		if (isQueryFromRegister) {
			return registerService.findServiceDefinitions(url);
		} else {
			return lookup(url);
		}
	}

	private static String getKeyFromUrl(URL url) {
		String interfaceName = url.getInterfaceName();
		String serviceName = url.getServiceName();
		String version = url.getVersion();
		return interfaceName + "/" + serviceName + "/?version=" + version;
	}

	private static String getKey(VenusServiceDefinitionDO url) {
		String serviceName = url.getName();
		String interfaceName = url.getInterfaceName();
		String version = url.getVersionRange();
		return interfaceName + "/" + serviceName + "/?version=" + version;
	}

	@Override
	public void load() throws VenusRegisteException {
		if (CollectionUtils.isNotEmpty(subscribleUrls)) {
			List<String> jsons = new ArrayList<String>();
			for (URL url : subscribleUrls) {
				String key = getKeyFromUrl(url);
				try {
					List<VenusServiceDefinitionDO> serviceDefinitions = registerService.findServiceDefinitions(url);
					if (CollectionUtils.isNotEmpty(serviceDefinitions)) {
						subscribleServiceDefinitionMap.put(key, serviceDefinitions);
						jsons.add(JSON.toJSONString(serviceDefinitions));
					}
				} catch (Exception e) {
					logger.error("服务{}ServiceDefLoaderRunnable 运行异常 ,异常原因：{}", url.getServiceName(), e);
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

	private class ServiceDefLoaderRunnable implements Runnable {
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
			List<VenusServiceDefinitionDO> oldObject = JSON.parseArray(json, VenusServiceDefinitionDO.class);
			if (CollectionUtils.isNotEmpty(newList)) {
				for (String str : newList) {
					List<VenusServiceDefinitionDO> newObject = JSON.parseArray(str, VenusServiceDefinitionDO.class);
					if (getKey(oldObject.get(0)).equals(getKey(newObject.get(0)))) {
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

	/*public static void main(String args[]) {

		VenusServiceDefinitionDO def1 = new VenusServiceDefinitionDO();
		VenusServiceDefinitionDO def2 = new VenusServiceDefinitionDO();

		RouterRule rr = new RouterRule();
		VenusServiceConfigDO conf = new VenusServiceConfigDO();
		conf.setRouterRule(rr);

		List<VenusServiceConfigDO> serviceConfigs = new ArrayList<VenusServiceConfigDO>();
		serviceConfigs.add(conf);
		def1.setServiceConfigs(serviceConfigs);
		def2.setServiceConfigs(serviceConfigs);
		def1.setName("orderService");
		def2.setName("userService");
		def1.setVersionRange("1.0.0");
		def2.setVersionRange("1.0.0");
		def1.setInterfaceName("com.chexiang.Orderservice");
		def2.setInterfaceName("com.chexiang.Userservice");

		List<VenusServiceDefinitionDO> list1=new ArrayList<VenusServiceDefinitionDO>();
		List<VenusServiceDefinitionDO> list2=new ArrayList<VenusServiceDefinitionDO>();
		list1.add(def1);
		list2.add(def2);
		List<String> jsons = new ArrayList<String>();
		jsons.add(JSON.toJSONString(list1));
		jsons.add(JSON.toJSONString(list2));
		String filePath = "D:\\soft\\b\\a.txt";
		writeFile(filePath, jsons);
		List<String> readFile = readFile(filePath);
		for (String str : readFile) {
			System.out.println(str);
		}
	}*/
	 

}
