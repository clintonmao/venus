package com.meidusa.venus.monitor;

import com.athena.service.api.AthenaDataService;
import com.meidusa.venus.Application;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.ServiceFactoryBean;
import com.meidusa.venus.ServiceFactoryExtra;
import com.meidusa.venus.exception.VenusConfigException;
import com.meidusa.venus.monitor.config.ClientConfigManagerIniter;
import com.meidusa.venus.monitor.support.CustomScanAndRegisteUtil;
import com.meidusa.venus.monitor.support.ApplicationContextHolder;
import com.meidusa.venus.util.ReftorUtil;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * venus监控工厂类
 * Created by Zhangzhihua on 2017/9/11.
 */
public class VenusMonitorFactory implements InitializingBean, ApplicationContextAware,BeanFactoryPostProcessor{

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    /**
     * 注册中心地址，多个地址以;分隔
     */
    private String address;

    private Application application;

    private ApplicationContext applicationContext;

    /**
     * athena配置信息初始化
     */
    private ClientConfigManagerIniter clientConfigManagerIniter;

    /**
     * 直连服务实例化工厂
     */
    private ServiceFactoryExtra serviceFactoryExtra;

    /**
     * 是否开启athena上报
     */
    private boolean enableAthenaReport = true;

    /**
     * 是否开启venus上报
     */
    private boolean enableVenusReport = true;

    /**
     * 判断是否有必须的依赖，如configManager、athena-client.jar、athenaDataService等
     */
    private boolean hasNeededDependences = true;

    private static VenusMonitorFactory venusMonitorFactory = null;

    /**
     * athena配置管理
     */
    private Object clientConfigManager;

    /**
     * athena上报服务
     */
    private AthenaDataService athenaDataService;

    private VenusMonitorFactory(){
        venusMonitorFactory = this;
    }

    /**
     * 获取实例，若spring未定义则为空
     * @return
     */
    public static VenusMonitorFactory getInstance(){
        return venusMonitorFactory;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        valid();

        init();
    }

    /**
     * 校验
     */
    void valid(){
        if(StringUtils.isEmpty(address)){
            throw new VenusConfigException("address not allow empty.");
        }
    }

    void init(){
        try {
            //手动扫描athena以注解定义的包
            scanAndRegisteAthenaPackage();

            //初始化athenaConfigManager
            initClientConfigManager();

            //初始化athenaDataService
            initAthenaDataService(address);

            //注册spring beans
            /*
            AutowireCapableBeanFactory beanFactory = this.applicationContext.getAutowireCapableBeanFactory();
            if(beanFactory != null && beanFactory instanceof ConfigurableListableBeanFactory){
                ConfigurableListableBeanFactory configurableListableBeanFactory = (ConfigurableListableBeanFactory)beanFactory;
            }else{
                hasNeededDependences = false;
                throw new VenusConfigException("get ConfigurableListableBeanFactory failed,cannot registe beans.");
            }
            */
        } catch (Throwable e) {
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error("init monitor factory failed.",e);
            }
            hasNeededDependences = false;
        }
    }


    /**
     * 手动扫描athena client包并注册到spring上下文
     */
    void scanAndRegisteAthenaPackage(){
        ApplicationContextHolder contextHolder = new ApplicationContextHolder();
        contextHolder.setApplicationContext(applicationContext);

        CustomScanAndRegisteUtil scanner = new CustomScanAndRegisteUtil();
        String[] confPkgs = {"/com/saic/framework/athena"};
        Class[] annotationTags = {Component.class,Service.class};
        Set<Class<?>> classSet = scanner.scan(confPkgs,annotationTags);
        if(CollectionUtils.isEmpty(classSet)){
            throw new VenusConfigException("scan com.saic.framework package failed.");
        }else{
            for(Class cls:classSet){
                if(logger.isDebugEnabled()){
                    logger.debug("cls:{}.",cls);
                }
            }
            scanner.regist(classSet);
        }
    }

    /**
     * 初始化athena配置信息
     */
    void initClientConfigManager(){
        //创建配置信息代理实例
        String className = "com.meidusa.venus.monitor.athena.config.impl.DefaultClientConfigManagerIniter";
        ClientConfigManagerIniter clientConfigManagerRegister = ReftorUtil.newInstance(className);
        if(clientConfigManagerRegister == null){
            throw new VenusConfigException("instance clientConfigManagerIniter failed.");
        }
        this.clientConfigManagerIniter = clientConfigManagerRegister;

        //初始化配置信息实例
        String appName = application.getName();
        if(StringUtils.isEmpty(appName)){
            throw new VenusConfigException("application not config.");
        }
        Object clientConfigManager = clientConfigManagerRegister.initConfigManager(appName,true);
        if(clientConfigManager == null){
            throw new VenusConfigException("init clientConfigManager failed.");
        }
        this.clientConfigManager = clientConfigManager;
    }

    /**
     * 初始化athenaDataService
     * @param url
     */
    void initAthenaDataService(String url){
        //通过ref实例化serviceFactory，避免client、monitor互相引用
        String className = "com.meidusa.venus.client.factory.simple.SimpleServiceFactory";
        Object obj = ReftorUtil.newInstance(className);
        if(obj == null){
            throw new VenusConfigException("init simpleServiceFactory failed.");
        }
        serviceFactoryExtra = (ServiceFactoryExtra)obj;
        String[] addressArr = {url};
        serviceFactoryExtra.setAddressList(addressArr);

        AthenaDataService athenaDataService = serviceFactoryExtra.getService(AthenaDataService.class);
        if(athenaDataService == null){
            throw new RpcException("init athenaDataService failed.");
        }
        this.athenaDataService = athenaDataService;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        try {
            registeBeans(beanFactory);
        } catch (Exception e) {
            if(exceptionLogger.isErrorEnabled()){
                exceptionLogger.error("postProcessBeanFactory failed.",e);
            }
            hasNeededDependences = false;
        }
    }

    /**
     * 注册spring beans
     * @param beanFactory
     */
    void registeBeans(ConfigurableListableBeanFactory beanFactory){
        if(hasNeededDependences){
            try {
                //注册configManager到spring上下文
                registeClientConfigManager(beanFactory);

                //注册athenaDataService到spring上下文
                registeAthenaDataService(beanFactory);
            } catch (Exception e) {
                if(exceptionLogger.isErrorEnabled()){
                    exceptionLogger.error("registe clientConfigManager and athenaDataService failed.",e);
                }
                hasNeededDependences = false;
            }
        }
    }

    /**
     * 注册configManager
     * @param beanFactory
     */
    void registeClientConfigManager(ConfigurableListableBeanFactory beanFactory){
        if(clientConfigManagerIniter != null){
            clientConfigManagerIniter.registeConfigManager(beanFactory,this.clientConfigManager);
        }
    }

    /**
     * 注册athenaDataService
     * @param beanFactory
     */
    void registeAthenaDataService(ConfigurableListableBeanFactory beanFactory){
        String beanName = athenaDataService.getClass().getSimpleName();
        BeanDefinitionRegistry reg = (BeanDefinitionRegistry) beanFactory;
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(ServiceFactoryBean.class);
        beanDefinition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, beanName));
        beanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
        ConstructorArgumentValues args = new ConstructorArgumentValues();
        args.addIndexedArgumentValue(0, this.athenaDataService);
        args.addIndexedArgumentValue(1, AthenaDataService.class);
        beanDefinition.setConstructorArgumentValues(args);
        beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_NAME);
        reg.registerBeanDefinition(beanName, beanDefinition);
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public AthenaDataService getAthenaDataService() {
        return athenaDataService;
    }

    public void setAthenaDataService(AthenaDataService athenaDataService) {
        this.athenaDataService = athenaDataService;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public boolean isEnableAthenaReport() {
        return enableAthenaReport;
    }

    public void setEnableAthenaReport(boolean enableAthenaReport) {
        this.enableAthenaReport = enableAthenaReport;
    }

    public boolean isEnableVenusReport() {
        return enableVenusReport;
    }

    public void setEnableVenusReport(boolean enableVenusReport) {
        this.enableVenusReport = enableVenusReport;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public boolean isHasNeededDependences() {
        return hasNeededDependences;
    }

    public void setHasNeededDependences(boolean hasNeededDependences) {
        this.hasNeededDependences = hasNeededDependences;
    }
}
