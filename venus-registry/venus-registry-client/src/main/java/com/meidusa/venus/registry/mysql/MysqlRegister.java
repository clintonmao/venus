package com.meidusa.venus.registry.mysql;

import com.caucho.hessian.HessianException;
import com.caucho.hessian.client.HessianRuntimeException;
import com.caucho.hessian.io.HessianProtocolException;
import com.caucho.hessian.io.HessianServiceException;
import com.meidusa.fastjson.JSON;
import com.meidusa.toolkit.common.runtime.GlobalScheduler;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.URL;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.domain.RouterRule;
import com.meidusa.venus.registry.domain.VenusServiceConfigDO;
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

	private String fileCachePath ;

	//是否开启本地文件缓存
	private static boolean isEnableFileCache = true;

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
			String property = System.getProperty("user.home");
			fileCachePath = property + File.separator + "venus" + File.separator
					+ ".venusCache.txt";
		} else {
			String property = System.getProperty("user.home");
			if (property.startsWith(File.separator)) {
				fileCachePath = property + File.separator + "venus" + File.separator + ".venusCache.txt";
			} else {
				fileCachePath = File.separator + property + File.separator + "venus" + File.separator
						+ ".venusCache.txt";
			}
		}
		if(logger.isInfoEnabled()){
			logger.info("fileCachePath=>{}",fileCachePath);
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
			if(logger.isInfoEnabled()){
				logger.info("registe service:{}.",url);
			}
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
		if(logger.isInfoEnabled()){
			logger.info("unregiste service:{}.",url);
		}
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
	public boolean subscrible(URL url) throws VenusRegisteException {
		if(logger.isInfoEnabled()){
			logger.info("subscrible service:{}.", url);
		}
		boolean success = true;
		try {
			registerService.subscrible(url);
			heartbeat();
		} catch (Exception e) {
			subscribleFailUrls.add(url);
			logger.error("服务{}订阅异常 ,异常原因：{}", url.getServiceName(), e);
			success = false;
		}
		subscribleUrls.add(url);
		load();
		return success;
	}

	@Override
	public void unsubscrible(URL url) throws VenusRegisteException {
		if(logger.isInfoEnabled()){
			logger.info("unsubscrible service:{}.",url);
		}
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
			if(isEnableFileCache()){
				serviceDefinitions = findSrvDefListFromFileCache(key);
			}
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
		StringBuilder buf = new StringBuilder();
		buf.append("/");
		buf.append(url.getInterfaceName());
		buf.append("/");
		buf.append(url.getServiceName());
		if(StringUtils.isNotEmpty(url.getVersion())){
			buf.append("?version=").append(url.getVersion());
		}
		return buf.toString();
	}

	private static String getKey(VenusServiceDefinitionDO url) {
		StringBuilder buf = new StringBuilder();
		buf.append("/");
		buf.append(url.getInterfaceName());
		buf.append("/");
		buf.append(url.getName());
		if(StringUtils.isNotEmpty(url.getVersion())){
			buf.append("?version=").append(url.getVersion());
		}
		return buf.toString();
	}

	@Override
	public void load() throws VenusRegisteException {
		if (CollectionUtils.isNotEmpty(subscribleUrls)) {
			List<String> jsons = new ArrayList<String>();
			boolean hasException=false;
			for (URL url : subscribleUrls) {
				try {
					List<VenusServiceDefinitionDO> serviceDefinitions = registerService.findServiceDefinitions(url);
					String key = getKeyFromUrl(url);
					if (CollectionUtils.isNotEmpty(serviceDefinitions)) {
						subscribleServiceDefinitionMap.put(key, serviceDefinitions);
						jsons.add(JSON.toJSONString(serviceDefinitions));
					}else{
						subscribleServiceDefinitionMap.remove(key);
					}
				} catch (Exception e) {
					if (e instanceof HessianRuntimeException || e instanceof HessianException
							|| e instanceof HessianProtocolException || e instanceof HessianServiceException) {
						hasException = true;
					}
					logger.error("服务{}ServiceDefLoaderRunnable 运行异常 ,异常原因：{}", url.getServiceName(), e);
				}
			}

			if(hasException){//查询接口有异常 不写本地缓存
				return;
			}
			//若开启本地文件缓存，则写文件
			if (isEnableFileCache()) {
				saveSrvDefListToFileCache(jsons);
			}

		}
	}

	/**
	 * 是否开启本地文件缓存
	 * @return
	 */
	boolean isEnableFileCache(){
		return isEnableFileCache;
	}

	/**
	 * 保存服务定义清单到本地文件缓存
	 * @param jsons
	 */
	void saveSrvDefListToFileCache(List<String> jsons){
		writeFile(fileCachePath, jsons);
	}

	/**
	 * 从本地文件缓存查找服务定义信息
	 * @param key
	 * @return
	 */
	List<VenusServiceDefinitionDO> findSrvDefListFromFileCache(String key){
		List<String> readFileJsons = readFile(fileCachePath);
		Map<String, List<VenusServiceDefinitionDO>> map = new HashMap<String, List<VenusServiceDefinitionDO>>();
		if (CollectionUtils.isNotEmpty(readFileJsons)) {
			for (String str : readFileJsons) {
				List<VenusServiceDefinitionDO> parseObject = JSON.parseArray(str, VenusServiceDefinitionDO.class);
				if (CollectionUtils.isNotEmpty(parseObject)) {
					map.put(getKey(parseObject.get(0)), parseObject);
				}
			}
		}
		List<VenusServiceDefinitionDO> serviceDefinitions = map.get(key);
		return serviceDefinitions;
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
						if(logger.isDebugEnabled()){
							logger.debug("report register heatbeat:{}.",url);
						}
						registerService.heartbeatRegister(url);
					} catch (Exception e) {
						logger.error("服务{}registe更新heartBeatTime异常 ,异常原因：{}", url.getServiceName(), e);
					}
					break;
				}
			}
			if (CollectionUtils.isNotEmpty(subscribleUrls)) {
				for (Iterator<URL> iterator = subscribleUrls.iterator(); iterator.hasNext();) {
					URL url = iterator.next();
					try {
						if(logger.isDebugEnabled()){
							logger.debug("report subscrible heatbeat:{}.",url);
						}
						registerService.heartbeatSubcribe(url);
					} catch (Exception e) {
						logger.error("服务{}subscrible更新heartBeatTime异常 ,异常原因：{}", url.getServiceName(), e);
					}
					break;
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
						boolean subscrible = subscrible(url);
						if (subscrible) {
							iterator.remove();
						}
					} catch (Exception e) {
						logger.error("Fail服务{}重新订阅异常 ,异常原因：{}", url.getServiceName(), e);
					}
				}
			}

		}

	}
	
	public static List<String> readFile(String filePath) {
		List<String> fileContents = new ArrayList<String>();
		if (!filePath.endsWith(".txt")) {
			return fileContents;
		}
		File file = new File(filePath);
		if (file.exists()) {
			RandomAccessFile randomAccessFile = null;
			try {
				randomAccessFile = new RandomAccessFile(file, "r");
			} catch (FileNotFoundException e) {
				logger.error("readFile filePath=>" + filePath + " is error", e);
			}
			String str = null;
			try {
				while ((str = randomAccessFile.readLine()) != null) {
					fileContents.add(new String(str.getBytes("ISO-8859-1"),"UTF-8"));
				}
			} catch (IOException e) {
				logger.error("readFile filePath=>" + filePath + " is error", e);
			} finally {
				if (null != randomAccessFile) {
					try {
						randomAccessFile.close();
					} catch (IOException e) {
						// ingore
					}
				}
			}
		}
		return fileContents;
	}

	/**
	 * 读文件
	 * 
	 * @param filePath
	 * @return
	 */
	public static List<String> readFile1(String filePath) {
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
			if (!f.exists()) {
				f.mkdirs();
			}
		} else {
			return;
		}
		if (CollectionUtils.isEmpty(jsons)) {
			File f = new File(filePath);
			try {
				if(f.exists()){
					f.delete();
				}
				if (f.createNewFile()) {
					f.setExecutable(true);
					f.setReadable(true);
					f.setWritable(true);
				}
			} catch (IOException e) {
				//ingore
			}
			return;
		}
		//List<String> readFiles = readFile(filePath);
		//List<String> need_write_list = getWriteList(readFiles, jsons);
		List<String> need_write_list = jsons;
		if (CollectionUtils.isEmpty(need_write_list)) {
			return;
		}
		RandomAccessFile randomAccessFile =null;
		try {
			File file = new File(filePath);
			if (file.createNewFile()) {
				// Runtime.getRuntime().exec("chmod 777 /home/test3.txt");
				file.setExecutable(true);
				file.setReadable(true);
				file.setWritable(true);
			}
			if (file.isFile()) {
				/*fileWriter = new FileWriter(file);
				bufferWriter = new BufferedWriter(fileWriter);
				for (String json : need_write_list) {
					bufferWriter.write(json);
					bufferWriter.newLine();
				}*/
				
				randomAccessFile = new RandomAccessFile(file, "rw");
				for (String json : need_write_list) {
					randomAccessFile.write(json.getBytes("UTF-8"));
					randomAccessFile.write("\n".getBytes("UTF-8"));
				}
				randomAccessFile.close();
			}
		} catch (IOException e) {
			logger.error("writeFile filePath=>" + filePath + " is error", e);
		} catch (NullPointerException e) {
			logger.error("writeFile filePath=>" + filePath + " is error", e);
		} finally {
			if (null != randomAccessFile) {
				try {
					randomAccessFile.close();
				} catch (IOException e) {
					// ingore
				}
			}
			/*
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
			}*/
		}
	}

	private static List<String> getWriteList(List<String> oldList, List<String> newList) {
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

	/*
	public static void main(String args[]) {
		List<String> readFileJsons = readFile("D:\\Users\\longhaisheng\\venus\\.venusCache.txt");
		Map<String, List<VenusServiceDefinitionDO>> map = new HashMap<String, List<VenusServiceDefinitionDO>>();
		if (CollectionUtils.isNotEmpty(readFileJsons)) {
			for (String str : readFileJsons) {
				List<VenusServiceDefinitionDO> parseObject = JSON.parseArray(str, VenusServiceDefinitionDO.class);
				if (CollectionUtils.isNotEmpty(parseObject)) {
					map.put(getKey(parseObject.get(0)), parseObject);
				}
			}
	}
	public static void main(String args[]) {
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
		def1.setDescription("中国北中华天线网");
		def2.setDescription("史可隽");

		List<VenusServiceDefinitionDO> list1=new ArrayList<VenusServiceDefinitionDO>();
		List<VenusServiceDefinitionDO> list2=new ArrayList<VenusServiceDefinitionDO>();
		list1.add(def1);
		list2.add(def2);
		List<String> jsons = new ArrayList<String>();
		jsons.add(JSON.toJSONString(list1));
		jsons.add(JSON.toJSONString(list2));
		String filePath = "D:\\soft\\b\\a.txt";
		//writeFile(filePath,  new ArrayList<String>() );
		writeFile(filePath, jsons);
		List<String> readFile = readFile(filePath);
		for (String str : readFile) {
			System.out.println(str);
		}
	}
	*/

}
