package com.meidusa.venus.manager.service;

import com.meidusa.fastjson.JSON;
import com.meidusa.venus.support.MonitorResource;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 默认资源监控实现
 * Created by Zhangzhihua on 2017/9/15.
 */
public class DefaultMonitorResource implements MonitorResource {

    private static Logger logger = VenusLoggerFactory.getDefaultLogger();

    private static Logger exceptionLogger = VenusLoggerFactory.getExceptionLogger();

    private static boolean isRunning = false;

    //版本号
    private String version;
    //其它属性项
    private Map<String,Object> properties = new HashMap<String,Object>();


    public void init(){
        logger.info("start resource monitor.");
        synchronized (DefaultMonitorResource.class){
            if(!isRunning){
                //1小时任务
                Timer timer = new Timer();
                TimerTask timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        oneHourTask();
                    }
                };
                timer.schedule(timerTask,5000,1000*60*60);
                //5分钟任务
                timer = new Timer();
                timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        oneMinutesTask();
                    }
                };
                timer.schedule(timerTask,5000,1000*60*10);
                isRunning = true;
            }
        }
    }

    /**
     * 1小时任务
     */
    void oneHourTask(){
        try {
            //打印版本号
            //readVersion();
        } catch (Exception e) {
            exceptionLogger.error("schedual task error.",e);
        }
    }

    /**
     * 1分钟任务
     */
    void oneMinutesTask(){
        try {
            //打印属性项
            /*
            if(logger.isInfoEnabled()){
                logger.info("properties:{}.", JSON.toJSONString(this.properties));
            }
            */
        } catch (Exception e) {
            exceptionLogger.error("schedual task error.",e);
        }
    }

    /**
     * 读取版本信息
     */
    static void readVersion(){
        String file =  DefaultMonitorResource.class.getResource("/version.txt").getFile();
        String version = readFile(new File(file));
        if(logger.isInfoEnabled()){
            logger.info(version);
        }
    }

    /**
     * 读取文件内容
     * @param file
     * @return
     */
    static String readFile(File file){
        StringBuilder result = new StringBuilder();
        try{
            BufferedReader br = new BufferedReader(new FileReader(file));
            String s = null;
            while((s = br.readLine())!=null){
                result.append(System.lineSeparator()+s);
            }
            br.close();
        }catch(Exception e){
            exceptionLogger.error("read file failed.",e);
        }
        return result.toString();
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public void addProperty(String key, Object object) {
        this.properties.put(key, object);
    }

    @Override
    public Object getProperty(String key) {
        return this.properties.get(key);
    }

    @Override
    public Map<String, Object> getAllProperties() {
        return this.properties;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

}
