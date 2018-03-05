package com.meidusa.venus.monitor;

import com.meidusa.toolkit.common.bean.config.ConfigUtil;
import com.meidusa.venus.ServiceFactoryBean;
import com.meidusa.venus.VenusApplication;
import com.meidusa.venus.exception.VenusConfigException;
import com.meidusa.venus.monitor.support.ApplicationContextHolder;
import com.meidusa.venus.monitor.support.CustomScanAndRegisteUtil;
import com.meidusa.venus.util.VenusLoggerFactory;
import com.saic.framework.athena.configuration.ClientConfigManager;
import com.saic.framework.athena.configuration.DefaultClientConfigManager;
import com.saic.framework.athena.message.impl.DefaultMessageManager;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Properties;
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

    //监控上报topic
    private static final String topic = "venus-report-topic";

    private static final String KAFKA_PORT = "9092";

    //是否开启athena上报
    private boolean enableAthenaReport = true;

    //是否开启venus上报
    private boolean enableVenusReport = true;

    private VenusApplication venusApplication;

    private ApplicationContext applicationContext;

    //athena配置管理
    private Object clientConfigManager;

    //判断是否有venus上报必须的依赖，如kafka client配置等
    private boolean hasVenusReportDepen = true;

    //判断是否有athena上报必须的依赖，如athena-client配置等
    private boolean hasAthenaReportDepen = true;

    private Producer<String, String> kafkaProducer;

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

        //校验地址有效性
        validAddress(address);
    }

    /**
     * 校验地址有效性
     * @param address
     */
    void validAddress(String address){
        //eg:10.32.221.6:9092,10.32.221.18:9092
        if(StringUtils.isEmpty(address)){
            String kafkaAddress = (String)ConfigUtil.filter("${venus.kafka.bootstrap.servers}");
            address = kafkaAddress;
            this.address = address;
        }
        if(StringUtils.isEmpty(address)){
            throw new VenusConfigException("monitor address not allow empty.");
        }
        String[] arr = address.split(",");
        if(arr == null || arr.length == 0){
            throw new VenusConfigException("monitor address is empty or invalid,valid address formt,eg:${venus.kafka.bootstrap.servers} or (10.32.221.6:9092,10.32.221.18:9092)");
        }

        String[] item = arr[0].split(":");
        String port = item[1];
        if(!port.equals(KAFKA_PORT)){
            throw new VenusConfigException("monitor address is invalid,valid address formt,eg:${venus.kafka.bootstrap.servers} or (10.32.221.6:9092,10.32.221.18:9092)");
        }
    }

    /**
     * 初始化
     */
    void init(){
        if(enableVenusReport){
            try {
                Producer<String, String> kafkaProducer = initKafkaProduer(address);
                this.kafkaProducer = kafkaProducer;
            } catch (Exception e) {
                if(exceptionLogger.isErrorEnabled()){
                    exceptionLogger.error("###########init kafka producer failed,will disable venus report. fail reason:{}",e.getLocalizedMessage());
                }
                hasVenusReportDepen = false;
            }
        }

        if(enableAthenaReport){
        }
    }

    /**
     * 创建kafka producer
     * @param address
     * @return
     */
    Producer<String, String> initKafkaProduer(String address){
        Properties props = new Properties();
        props.put("bootstrap.servers", address);//服务器ip:端口号，集群用逗号分隔
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("auto.create.topics.enable", true);
        Producer<String, String> producer = new KafkaProducer<>(props);
        return producer;
    }


    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if(enableVenusReport && hasVenusReportDepen){
            //ignore
        }

        if(enableAthenaReport && hasAthenaReportDepen){
            try {
                initAthenaClient(beanFactory);
            } catch (Exception e) {
                if(exceptionLogger.isErrorEnabled()){
                    exceptionLogger.error("###########init athena client failed,will disable athena report. fail reason:{}",e.getLocalizedMessage());
                }
                hasAthenaReportDepen = false;
            }
        }
    }

    /**
     * 初始化athena client配置
     */
    void initAthenaClient(ConfigurableListableBeanFactory beanFactory){
        //若未定义athena扫描包，则手动扫描athena以注解定义的包
        boolean isAthenaScanConfig = false;
        try {
            Object bean = beanFactory.getBean(DefaultMessageManager.class);
            if(bean != null){
                isAthenaScanConfig = true;
            }
        } catch (BeansException e) {
            logger.warn("get DefaultMessageManager failed:" + e.getClass());
        }
        if(!isAthenaScanConfig){
            if(logger.isWarnEnabled()){
                logger.warn("########athena scan package not config,will scan athena package.");
            }
            scanAndRegisteAthenaPackage();
        }


        //若未配置clientConfigManager，则初始化配置
        boolean isAthenaConfigManager = false;
        try {
            Object bean = beanFactory.getBean(DefaultClientConfigManager.class);
            if(bean != null){
                isAthenaConfigManager = true;
            }
        } catch (BeansException e) {
            logger.warn("get DefaultClientConfigManager failed:" + e.getClass());
        }
        if(!isAthenaConfigManager){
            if(logger.isWarnEnabled()){
                logger.warn("########athena ClientConfigManager not config,will init athena client config manager.");
            }
            //初始化clientConfigManager
            initClientConfigManager();

            //注册clientConfigManager
            registeClientConfigManager(beanFactory);
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

    /**
     * 初始化ClientConfigManager
     */
    void initClientConfigManager(){
        //初始化clientConfigManager
        DefaultClientConfigManager clientConfigManager = new DefaultClientConfigManager();
        String appName = venusApplication.getName();
        if(StringUtils.isEmpty(appName)){
            throw new VenusConfigException("venusApplication or venusApplication.name not config.");
        }
        clientConfigManager.setAppName(appName);
        //初始化
        clientConfigManager.setMonitorEnabled(true);
        clientConfigManager.init();
        if(clientConfigManager == null){
            throw new VenusConfigException("init clientConfigManager failed.");
        }
        this.clientConfigManager = clientConfigManager;
    }

    /**
     * 注册athena上报依赖beans
     * @param beanFactory
     */
    void registeClientConfigManager(ConfigurableListableBeanFactory beanFactory){
        //注册configManager到spring上下文
        /*
        String simpleClassName = clientConfigManager.getClass().getSimpleName();
        if(simpleClassName.contains(".")){
            simpleClassName=simpleClassName.substring(simpleClassName.lastIndexOf(".")+1);
        }
        simpleClassName = simpleClassName.substring(0, 1).toLowerCase().concat(simpleClassName.substring(1));
        */
        String beanName = "configManager";
        BeanDefinitionRegistry reg = (BeanDefinitionRegistry) beanFactory;
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(ServiceFactoryBean.class);
        beanDefinition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, beanName));
        beanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
        ConstructorArgumentValues args = new ConstructorArgumentValues();
        args.addIndexedArgumentValue(0, clientConfigManager);
        args.addIndexedArgumentValue(1, ClientConfigManager.class);
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

    public Producer<String, String> getKafkaProducer() {
        return kafkaProducer;
    }

    public void setKafkaProducer(Producer<String, String> kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    public String getTopic() {
        return topic;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
