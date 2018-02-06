package com.meidusa.venus.monitor;

import com.athena.service.api.AthenaDataService;
import com.meidusa.venus.VenusApplication;
import com.meidusa.venus.exception.RpcException;
import com.meidusa.venus.ServiceFactoryBean;
import com.meidusa.venus.ServiceFactoryExtra;
import com.meidusa.venus.exception.VenusConfigException;
import com.meidusa.venus.monitor.support.CustomScanAndRegisteUtil;
import com.meidusa.venus.monitor.support.ApplicationContextHolder;
import com.meidusa.venus.registry.VenusRegistryFactory;
import com.meidusa.venus.util.ReftorUtil;
import com.meidusa.venus.util.VenusLoggerFactory;
import com.saic.framework.athena.configuration.ClientConfigManager;
import com.saic.framework.athena.configuration.DefaultClientConfigManager;
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

    //监控心地址，多个地址以;分隔
    private String address;

    //是否开启athena上报
    private boolean enableAthenaReport = true;

    //是否开启venus上报
    private boolean enableVenusReport = true;

    private VenusApplication venusApplication;

    //注册中心工厂
    private VenusRegistryFactory venusRegistryFactory;

    private ApplicationContext applicationContext;

    //判断是否有athena上报必须的依赖，如athenaDataService、configManager、athena-client等
    private boolean hasAthenaReportDepen = true;

    //判断是否有venus上报必须的依赖，如athenaDataService
    private boolean hasVenusReportDepen = true;

    //判断AthenaDataService是否已注册
    private boolean isRegistedOfAthenaDataService = false;

    //athena配置管理
    private Object clientConfigManager;

    //athena上报服务
    private AthenaDataService athenaDataService;

    private static VenusMonitorFactory venusMonitorFactory = null;

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
        //若都没开启，则跳过
        if(!enableAthenaReport && !enableVenusReport){
            return;
        }

        //校验
        valid();

        //初始化
        init();
    }

    /**
     * 校验
     */
    void valid(){
        if(venusApplication == null){
            throw new VenusConfigException("venusApplication not config.");
        }
        if(StringUtils.isEmpty(address) && venusRegistryFactory == null){
            throw new VenusConfigException("address and venusRegistryFactory not allow empty.");
        }
    }

    /**
     * 初始化
     */
    void init(){
        if(enableVenusReport){
            try {
                //初始化athenaDataService
                initAthenaDataServiceBean();
            } catch (Exception e) {
                if(exceptionLogger.isErrorEnabled()){
                    exceptionLogger.error("init athenaDataService failed,will disable venus report. fail reason:{}",e.getLocalizedMessage());
                }
                hasVenusReportDepen = false;
            }
        }

        if(enableAthenaReport){
            try {
                //初始化athenaDataService
                initAthenaDataServiceBean();

                //初始化ClientConfigManager
                initClientConfigManager();
            } catch (Throwable e) {
                if(exceptionLogger.isErrorEnabled()){
                    exceptionLogger.error("init athena report failed,will disable athena report. fail reason:{}",e.getLocalizedMessage());
                }
                hasAthenaReportDepen = false;
            }
        }
    }

    /**
     * 初始化athenaDataService
     */
    void initAthenaDataServiceBean(){
        if(this.athenaDataService == null){
            //初始化athenaDataService
            //通过ref实例化serviceFactory，避免client、monitor互相引用
            String className = "com.meidusa.venus.client.factory.simple.SimpleServiceFactory";
            Object obj = ReftorUtil.newInstance(className);
            if(obj == null){
                throw new VenusConfigException("init simpleServiceFactory failed.");
            }
            ServiceFactoryExtra serviceFactoryExtra = (ServiceFactoryExtra)obj;
            if(StringUtils.isNotEmpty(address)){
                serviceFactoryExtra.setAddressList(address);
            }else if(venusRegistryFactory != null && venusRegistryFactory.getRegister() != null){
                serviceFactoryExtra.setRegister(venusRegistryFactory.getRegister());
            }

            AthenaDataService athenaDataService = serviceFactoryExtra.getService(AthenaDataService.class);
            if(athenaDataService == null){
                throw new RpcException("init athenaDataService failed.");
            }
            this.athenaDataService = athenaDataService;
        }
    }

    /**
     * 初始化ClientConfigManager
     */
    void initClientConfigManager(){
        if(this.athenaDataService == null){
            throw new VenusConfigException("athenaDataService is null.");
        }

        //手动扫描athena以注解定义的包
        scanAndRegisteAthenaPackage();

        //初始化clientConfigManager
        DefaultClientConfigManager clientConfigManager = new DefaultClientConfigManager();
        String appName = venusApplication.getName();
        if(StringUtils.isEmpty(appName)){
            throw new VenusConfigException("venusApplication or venusApplication.name not config.");
        }
        clientConfigManager.setAppName(appName);
        clientConfigManager.setMonitorEnabled(true);
        clientConfigManager.init();
        if(clientConfigManager == null){
            throw new VenusConfigException("init clientConfigManager failed.");
        }
        this.clientConfigManager = clientConfigManager;
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
            throw new VenusConfigException("scan com.saic.framework.athena package empty.");
        }else{
            for(Class cls:classSet){
                if(logger.isDebugEnabled()){
                    logger.debug("cls:{}.",cls);
                }
            }
            scanner.regist(classSet);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if(enableVenusReport && hasVenusReportDepen){
            try {
                //注册venus上报依赖beans
                registeAthenaDataServiceBean(beanFactory);
            } catch (Exception e) {
                if(exceptionLogger.isErrorEnabled()){
                    exceptionLogger.error("registe athenaDataService failed,will disable venus report.",e);
                }
                hasVenusReportDepen = false;
            }
        }

        if(enableAthenaReport && hasAthenaReportDepen){
            try {
                //注册venus上报依赖beans
                registeAthenaDataServiceBean(beanFactory);

                //注册athena上报依赖beans
                registeClientConfigManagerBean(beanFactory);
            } catch (Exception e) {
                if(exceptionLogger.isErrorEnabled()){
                    exceptionLogger.error("registe athenaDataService and clientConfigManager failed,will disable venus report.",e);
                }
                hasAthenaReportDepen = false;
            }
        }
    }

    /**
     * 注册venus上报依赖beans
     * @param beanFactory
     */
    void registeAthenaDataServiceBean(ConfigurableListableBeanFactory beanFactory){
        if(!isRegistedOfAthenaDataService){
            //注册athenaDataService到spring上下文
            String simpleClassName = AthenaDataService.class.getSimpleName();
            String beanName = formatClassName(simpleClassName);
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

            isRegistedOfAthenaDataService = true;
        }
    }

    /**
     * 注册athena上报依赖beans
     * @param beanFactory
     */
    void registeClientConfigManagerBean(ConfigurableListableBeanFactory beanFactory){
        //注册configManager到spring上下文
        String simpleClassName = clientConfigManager.getClass().getSimpleName();
        if(simpleClassName.contains(".")){
            simpleClassName=simpleClassName.substring(simpleClassName.lastIndexOf(".")+1);
        }
        simpleClassName = simpleClassName.substring(0, 1).toLowerCase().concat(simpleClassName.substring(1));
        BeanDefinitionRegistry reg = (BeanDefinitionRegistry) beanFactory;
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(ServiceFactoryBean.class);
        beanDefinition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, simpleClassName));
        beanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
        ConstructorArgumentValues args = new ConstructorArgumentValues();
        args.addIndexedArgumentValue(0, clientConfigManager);
        args.addIndexedArgumentValue(1, ClientConfigManager.class);
        beanDefinition.setConstructorArgumentValues(args);
        beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_NAME);
        reg.registerBeanDefinition(simpleClassName, beanDefinition);
    }

    /**
     * 将simpleClassName转为首字母小写
     * @return
     */
    String formatClassName(String simpleClassName){
        if(simpleClassName.contains(".")){
            simpleClassName=simpleClassName.substring(simpleClassName.lastIndexOf(".")+1);
        }
        simpleClassName = simpleClassName.substring(0, 1).toLowerCase().concat(simpleClassName.substring(1));
        return simpleClassName;
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
        return enableAthenaReport && hasAthenaReportDepen;
    }

    public void setEnableAthenaReport(boolean enableAthenaReport) {
        this.enableAthenaReport = enableAthenaReport;
    }

    public boolean isEnableVenusReport() {
        return enableVenusReport && hasVenusReportDepen;
    }

    public void setEnableVenusReport(boolean enableVenusReport) {
        this.enableVenusReport = enableVenusReport;
    }

    public VenusApplication getVenusApplication() {
        return venusApplication;
    }

    public void setVenusApplication(VenusApplication venusApplication) {
        this.venusApplication = venusApplication;
    }

    public VenusRegistryFactory getVenusRegistryFactory() {
        return venusRegistryFactory;
    }

    public void setVenusRegistryFactory(VenusRegistryFactory venusRegistryFactory) {
        this.venusRegistryFactory = venusRegistryFactory;
    }
}
