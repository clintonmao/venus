package com.meidusa.venus;

import com.meidusa.venus.backend.services.ServiceManager;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.exception.XmlVenusExceptionFactory;
import com.meidusa.venus.support.VenusContext;
import com.meidusa.venus.exception.VenusConfigException;
import com.meidusa.venus.io.serializer.SerializerFactory;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Venus应用定义
 * Created by Zhangzhihua on 2017/9/15.
 */
public class Application implements InitializingBean,DisposableBean {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    //应用名称
    private String name;

    //是否开启filter，默认开启
    private boolean enableFilter = true;

    private static Application application;

    //是否已释放
    private static boolean isDestroyed = false;

    //---------------以下为要管理的资源列表 -----------------------
    //invoker列表
    private static List<Invoker> invokerList = new ArrayList<Invoker>();

    //服务工厂列表
    private static List<ServiceFactory> serviceFactoryList = new ArrayList<ServiceFactory>();

    //服务管理列表
    private static List<ServiceManager> serviceManagerList = new ArrayList<ServiceManager>();

    //协议列表
    private static List<Protocol> protocolList = new ArrayList<Protocol>();

    private Application(){
        application = this;
        Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownListener()));
    }

    public static Application getInstance(){
        if(application == null){
            throw new VenusConfigException("application not inited.");
        }
        return application;
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
        if(StringUtils.isEmpty(name)){
            throw new VenusConfigException("application name not allow empty.");
        }
    }

    /**
     * 初始化
     */
    void init(){
        VenusContext.getInstance().setApplication(name);

        //初始化序列化配置
        SerializerFactory.init();

        //初始化异常配置
        XmlVenusExceptionFactory.getInstance().init();
    }

    @Override
    public void destroy() throws Exception {
        if(logger.isWarnEnabled()){
            logger.warn("spring container destroy,release resource.");
        }
        synchronized (Application.class){
            if(!isDestroyed){
                doDestroy();
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
                logger.warn("application exit,release resource.");
            }
            synchronized (Application.class){
                if(!isDestroyed){
                    doDestroy();
                }
            }
        }
    }

    /**
     * 释放应用释放，如释放连接，反订阅、反注册等
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
        for(ServiceManager serviceManager:serviceManagerList){
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

    public static List<ServiceManager> getServiceManagerList() {
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

    public static void addServiceManager(ServiceManager serviceManager){
        serviceManagerList.add(serviceManager);
    }

    public static void addProtocol(Protocol protocol){
        protocolList.add(protocol);
    }
}
