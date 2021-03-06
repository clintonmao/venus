package com.meidusa.venus;

import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.exception.VenusConfigException;
import com.meidusa.venus.exception.XmlVenusExceptionFactory;
import com.meidusa.venus.io.serializer.SerializerFactory;
import com.meidusa.venus.support.MonitorResourceFacade;
import com.meidusa.venus.support.VenusContext;
import com.meidusa.venus.util.VenusLoggerFactory;
import com.saike.commons.Application;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import sun.misc.URLClassPath;

import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Venus应用定义
 * Created by Zhangzhihua on 2017/9/15.
 */
public class VenusApplication implements InitializingBean,DisposableBean {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    //应用名称
    private String name;

    private Application application;

    //是否开启filter，默认开启
    private boolean enableFilter = true;

    private static VenusApplication venusApplication;

    //是否已释放
    private static boolean isDestroyed = false;

    //---------------以下为要管理的资源列表 -----------------------

    //服务工厂列表[client]
    private static List<ServiceFactory> serviceFactoryList = new ArrayList<ServiceFactory>();

    //invoker列表[client]
    private static List<Invoker> invokerList = new ArrayList<Invoker>();

    //协议列表[backend]
    private static List<Protocol> protocolList = new ArrayList<Protocol>();

    //服务管理列表[backend]
    private static List<Destroyier> serviceManagerList = new ArrayList<Destroyier>();

    private VenusApplication(){
        venusApplication = this;
        Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownListener()));
    }

    public static VenusApplication getInstance(){
        return venusApplication;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        //校验
        valid();

        //初始化
        init();
    }

    /**
     * 验证名称有效性
     */
    void valid(){
        //校验名称
        if(StringUtils.isEmpty(name)){
            if(application == null || StringUtils.isEmpty(application.getAppName())){
                throw new VenusConfigException("venusApplication name and application not allow empty.");
            }
        }

        //校验是否包含不兼容的jar包
        validFilteJars();

        //检查venus模块是否包含必须的校验文件
        validModulesVersion();
    }

    /**
     * 检查是否包含不兼容的jar包
     * */
    void validFilteJars(){
        //List<String> clsPaths = getJarsFromSystemVal();
        List<String> clsPaths = getJarsFromClassLoader();
        logger.info("#########class path list begin#####");
        for(String item:clsPaths){
            logger.info(item);
        }
        logger.info("#########class path list end#####");

        //校验不兼容、过期的venus jar包(通过正则表达式匹配)
        String[] filteJars = {
                //venus annotation
                "venus-annotations-3(.*?).jar",
                //venus common
                "venus-common-base-3(.*?).jar",
                "venus-common-exception-3(.*?).jar",
                "venus-common-io-3(.*?).jar",
                "venus-common-service-3(.*?).jar",
                "venus-common-validator-3(.*?).jar",
                //venus client
                "venus-client-3(.*?).jar",
                //venus backend
                "venus-backend-3(.*?).jar",
                //venus athena相关
                "venus-extension-athena-3(.*?).jar",
                "venus-athena-impl-3(.*?).jar"
        };
        List<String> filteJarList = Arrays.asList(filteJars);
        if(CollectionUtils.isEmpty(filteJarList)){
            return;
        }

        String wsepr = "\\";//windows
        String lsepr = "/";//linux
        for(String item:clsPaths){
            int indexof = -1;
            if(item.lastIndexOf(wsepr) != -1){
                indexof = item.lastIndexOf(wsepr);
            }else if(item.lastIndexOf(lsepr) != -1){
                indexof = item.lastIndexOf(lsepr);
            }
            //格式不符
            if(indexof == -1){
                continue;
            }
            //后续不符
            String clsPathName = item.substring(indexof+1,item.length());
            if(!clsPathName.endsWith(".jar")){
                continue;
            }
            String jarName = clsPathName;
            for(String filteJarName:filteJarList){
                //判断是否要过滤
                Pattern r = Pattern.compile(filteJarName);
                Matcher m = r.matcher(jarName);
                if (m.find( )) {
                   throw new VenusConfigException("found incompatible jar:" + jarName + ",please exclude.more detail info to see http://cf.dds.com/pages/viewpage.action?pageId=12456812 2.3.1 section.");
                }
            }
        }
    }

    /**
     * 获取clazzloader加载jars
     * @return
     */
    List<String> getJarsFromClassLoader(){
        List<String> jars = new ArrayList<>();
        List<String> clazzLoaderJars = new ArrayList<>();
        ClassLoader clazzLoader = Thread.currentThread().getContextClassLoader();
        logger.info("#######clazzLoader:{}",clazzLoader);

        try {
            if(clazzLoader != null && clazzLoader instanceof URLClassLoader){
                URLClassLoader urlClassLoader = (URLClassLoader)clazzLoader;
                //getURLS方式获取jars
                java.net.URL[] urls = urlClassLoader.getURLs();
                for(java.net.URL url:urls){
                    jars.add(url.getPath());
                }

                //修改clazzLoader属性作用域，获取获取jars
                Field field = urlClassLoader.getClass().getDeclaredField("ucp");
                field.setAccessible(true);
                Object ucpValue = field.get(urlClassLoader);
                if(ucpValue != null && ucpValue instanceof URLClassPath){
                    URLClassPath urlClassPath = (URLClassPath)ucpValue;
                    //loaders lmap
                    Field loadersField = urlClassPath.getClass().getDeclaredField("lmap");
                    loadersField.setAccessible(true);
                    Object lmapValue = loadersField.get(urlClassPath);
                    if(lmapValue != null && lmapValue instanceof Map){
                        Map<?,?> mm = (Map)lmapValue;
                        for(Map.Entry<?,?> entry:mm.entrySet()){
                            String clsPath = entry.getKey().toString();
                            clazzLoaderJars.add(clsPath);
                        }
                    }
                }

            }
        }catch (Exception ex){
            //若clazzLoader方式获取异常，则返回普通方式-getURLS获取的jars，在不同环境下不准
            return jars;
        }
        return clazzLoaderJars;
    }

    /**
     * 获取classloader加载Jars
     * @return
     */
    List<String> getJarsFromSystemVal(){
        List<String> jars = new ArrayList<>();
        //get properties
        /*
        Properties properties = System.getProperties();
        Enumeration enums = properties.keys();
        while(enums.hasMoreElements()){
            String item = enums.nextElement().toString();
            if(item.contains("path") || item.contains("url") || item.contains("PATH") || item.contains("URL")){
                logger.info("{}:{}",item,properties.get(item));
            }
        }
        */
        //获取class path jars
        String clsPath = System.getProperty("java.class.path");
        if(StringUtils.isEmpty(clsPath)){
            return jars;
        }
        String[] clsPaths = clsPath.split(";");
        if(clsPaths == null || clsPaths.length == 0){
            return jars;
        }
        for(int i=0;i<clsPaths.length;i++){
            jars.add(clsPaths[i]);
        }
        return jars;
    }

    /**
     * 检查venus模块是否包含必要的校验文件
     */
    void validModulesVersion(){
        String[] packages = {
                //common-base
                "com.meidusa.venus.CommonBasePackageValid",
                //common-exception
                "com.meidusa.venus.exception.CommonExceptionPackageValid",
                //remote
                "com.meidusa.venus.io.RemotePackageValid",
                //client
                "com.meidusa.venus.client.ClientPackageValid"
        };
        List<String> packegeList = Arrays.asList(packages);
        if(CollectionUtils.isEmpty(packegeList)){
            return;
        }
        for(String pkgName:packegeList){
            try {
                Class<?> pkgClz = Class.forName(pkgName);
                PackageValid packageValid = (PackageValid)pkgClz.newInstance();
                packageValid.valid();
            } catch (ClassNotFoundException e) {
                String errorMsg = String.format("class %s not found,please check jar config.",pkgName);
                throw new VenusConfigException(errorMsg);
            }catch (InstantiationException e) {
                String errorMsg = String.format("class %s instance failed,please check jar config.",pkgName);
                throw new VenusConfigException(errorMsg);
            } catch (IllegalAccessException e) {
                String errorMsg = String.format("class %s access failed,please check jar config.",pkgName);
                throw new VenusConfigException(errorMsg);
            }catch (Exception e){
                String errorMsg = String.format("class %s valid failed,please check jar config.",pkgName);
                throw new VenusConfigException(errorMsg);
            }
        }
    }



    /**
     * 初始化
     */
    void init(){
        if(StringUtils.isEmpty(name)){
            name = application.getAppName();
        }
        logger.info("#######application name:{}",name);
        VenusContext.getInstance().setApplication(name);

        //初始化序列化配置
        SerializerFactory.init();

        //初始化异常配置
        XmlVenusExceptionFactory.getInstance().init();

        MonitorResourceFacade.getInstance().init();
    }

    @Override
    public void destroy() throws Exception {
        if(logger.isWarnEnabled()){
            logger.warn("spring container destroy,release resource.");
        }
        synchronized (VenusApplication.class){
            if(!isDestroyed){
                doDestroy();
                isDestroyed = true;
            }else{
                logger.info("spring container already released.");
            }
        }
    }

    /**
     * 应用关闭监听事件
     */
    class ShutdownListener implements Runnable{

        @Override
        public void run() {
            if(logger.isWarnEnabled()){
                logger.warn("venusApplication exit,release resource.");
            }
            synchronized (VenusApplication.class){
                if(!isDestroyed){
                    doDestroy();
                    isDestroyed = true;
                }else{
                    logger.info("venusApplication already released.");
                }
            }
        }
    }

    /**
     * 释放应用资源，如释放连接，反订阅、反注册等
     */
    void doDestroy(){
        try {
            releaseInvoker();
            releaseProtocol();
            releaseServiceManager();
            releaseServiceFactory();
        } catch (Throwable e) {
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error("release resource failed.",e);
            }
        }
    }

    /**
     * 释放invoker资源
     */
    void releaseInvoker(){
        //释放invoker资源
        if(CollectionUtils.isEmpty(invokerList)){
            return;
        }
        for(Invoker invoker:invokerList){
            if(invoker != null){
                try {
                    invoker.destroy();
                } catch (RpcException e) {
                    if(exceptionLogger.isErrorEnabled()){
                        exceptionLogger.error("release invoker failed.",e);
                    }
                }
            }
        }
    }

    /**
     * 释放serviceFactory资源
     */
    void releaseServiceFactory(){
        if(CollectionUtils.isEmpty(serviceFactoryList)){
            return;
        }
        for(ServiceFactory serviceFactory:serviceFactoryList){
            if(serviceFactory != null){
                try {
                    serviceFactory.destroy();
                } catch (Exception e) {
                    if(exceptionLogger.isErrorEnabled()){
                        exceptionLogger.error("release serviceFactory failed.",e);
                    }
                }
            }
        }
    }

    /**
     * 资源serviceManager资源
     */
    void releaseServiceManager(){
        if(CollectionUtils.isEmpty(serviceManagerList)){
            return;
        }
        for(Destroyier serviceManager:serviceManagerList){
            if(serviceManager != null){
                try {
                    serviceManager.destroy();
                } catch (Exception e) {
                    if(exceptionLogger.isErrorEnabled()){
                        exceptionLogger.error("release serviceManager failed.",e);
                    }
                }
            }
        }
    }

    /**
     * 释放protocol资源
     */
    void releaseProtocol(){
        if(CollectionUtils.isEmpty(protocolList)){
            return;
        }
        for(Protocol protocol:protocolList){
            if(protocol != null){
                try {
                    protocol.destroy();
                } catch (Exception e) {
                    if(exceptionLogger.isErrorEnabled()){
                        exceptionLogger.error("release protocol failed.",e);
                    }
                }
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnableFilter() {
        return enableFilter;
    }

    public void setEnableFilter(boolean enableFilter) {
        this.enableFilter = enableFilter;
    }

    public static List<Invoker> getInvokerList() {
        return invokerList;
    }

    public static List<ServiceFactory> getServiceFactoryList() {
        return serviceFactoryList;
    }

    public static List<Destroyier> getServiceManagerList() {
        return serviceManagerList;
    }

    public static List<Protocol> getProtocolList() {
        return protocolList;
    }

    public static void addInvoker(Invoker invoker){
        invokerList.add(invoker);
    }

    public static void addServiceFactory(ServiceFactory serviceFactory){
        serviceFactoryList.add(serviceFactory);
    }

    public static void addServiceManager(Destroyier serviceManager){
        serviceManagerList.add(serviceManager);
    }

    public static void addProtocol(Protocol protocol){
        protocolList.add(protocol);
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

}
