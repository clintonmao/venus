package com.meidusa.venus.monitor;

import com.meidusa.venus.VenusApplication;
import com.meidusa.venus.exception.VenusConfigException;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.Properties;

/**
 * venus监控工厂类
 * Created by Zhangzhihua on 2017/9/11.
 */
public class VenusMonitorFactory implements InitializingBean, BeanFactoryPostProcessor{

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    //监控心地址，多个地址以;分隔
    private String address;

    //监控上报topic
    private static final String topic = "venus-report-topic";

    //是否开启athena上报
    private boolean enableAthenaReport = true;

    //是否开启venus上报
    private boolean enableVenusReport = true;

    private VenusApplication venusApplication;

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
        if(StringUtils.isEmpty(address)){
            throw new VenusConfigException("monitor address not allow empty.");
        }
        //10.32.221.6:9092,10.32.221.18:9092
        String[] arr = address.split(",");
        if(arr == null || arr.length == 0){
            throw new VenusConfigException("monitor address is empty or invalid,valid address formt,eg:${venus.kafka.bootstrap.servers} or (10.32.221.6:9092,10.32.221.18:9092)");
        }

        String[] item = arr[0].split(":");
        String port = item[1];
        if(!port.equals("9092")){
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
            try {
                validAthenaClient();
            } catch (Throwable e) {
                if(exceptionLogger.isErrorEnabled()){
                    exceptionLogger.error("###########init athena client failed,will disable athena report. fail reason:{}",e.getLocalizedMessage());
                }
                hasAthenaReportDepen = false;
            }
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

    /**
     * 校验athena client配置
     */
    void validAthenaClient(){
        try {
            Class.forName("com.saic.framework.athena.configuration.ClientConfigManager");
        } catch (ClassNotFoundException e) {
            throw new VenusConfigException("athena client not config.");
        }
    }


    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if(enableVenusReport && hasVenusReportDepen){
            //ignore
        }

        if(enableAthenaReport && hasAthenaReportDepen){
            //ignore
        }
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

}
