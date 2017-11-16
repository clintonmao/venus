package com.meidusa.venus.manager.service;

import com.meidusa.venus.support.MonitorResource;
import com.meidusa.venus.util.VenusLoggerFactory;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Venus版本信息
 * Created by Zhangzhihua on 2017/9/15.
 */
public class AllResourceService implements MonitorResource {

    private static Logger logger = VenusLoggerFactory.getStatusLogger();

    private static Logger statusLogger = VenusLoggerFactory.getStatusLogger();

    private static boolean isRunning = false;
    public void init(){
        logger.info("start resource monitor.");
        readVersion();
        synchronized (AllResourceService.class){
            if(!isRunning){
                //1小时任务
                Timer timer = new Timer();
                TimerTask timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        schedualTask();
                    }
                };
                timer.schedule(timerTask,1000,1000*60);
                //5分钟任务
                //...
                isRunning = true;
            }
        }
    }

    /**
     * 定时输出资源占用信息
     */
    void schedualTask(){
        try {
            //logger.info("current all resource infos.");
        } catch (Exception e) {
            logger.error("schedual task error.",e);
        }
    }

    /**
     * 读取版本信息
     */
    static void readVersion(){
        String file =  AllResourceService.class.getResource("/version.txt").getFile();
        String version = readFile(new File(file));
        statusLogger.info(version);
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
            e.printStackTrace();
        }
        return result.toString();
    }

    public static void main(String[] args){
        new AllResourceService().readVersion();
    }
}
