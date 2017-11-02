package com.chexiang.venus.demo.consumer.controller;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chexiang.venus.demo.provider.HelloService;
import com.chexiang.venus.demo.provider.model.Hello;
import com.meidusa.venus.Result;

import ch.qos.logback.core.net.SyslogOutputStream;

/**
 * ManagerController
 * Created by Zhangzhihua on 2017/9/25.
 */
@RestController
@RequestMapping("/ben")
public class BenchmarkController {

    private static Logger logger = LoggerFactory.getLogger(BenchmarkController.class);

    @Autowired
    HelloService helloService;

    AtomicInteger totalCount = new AtomicInteger(0);

    AtomicInteger currentCount = new AtomicInteger(0);

    AtomicInteger failCount = new AtomicInteger(0);
    AtomicInteger thread_count = new AtomicInteger(0);
    
    ConcurrentMap<Integer,Long> concurrentMap=new ConcurrentHashMap<Integer,Long>();

    long bTime;

    static boolean isRunning = false;

    @RequestMapping("/start/{threadCount}/{total}/{packetSize}")
    public Result start(@PathVariable String threadCount,@PathVariable int total,@PathVariable int packetSize){
        if(isRunning){
            Result result = new Result();
            result.setErrorCode(500);
            result.setErrorMessage("task is running.");
            return result;
        }
        isRunning =  true;

        //set
        totalCount.set(total);
        thread_count.set(Integer.valueOf(threadCount));
        currentCount.set(0);
        failCount.set(0);
        concurrentMap.clear();
        int thrCount = Integer.parseInt(threadCount);

        //测试服务可用
        logger.error("start...");
        try {
            logger.error("test start...");
            helloService.getHello("jack");
            logger.error("test ok.");
        } catch (Exception e) {
            logger.error("test failed.",e);
            Result result = new Result();
            result.setErrorCode(500);
            result.setErrorMessage(e.getLocalizedMessage());
            return result;
        }
        try {
            TimeUnit.MICROSECONDS.sleep(500);
        } catch (InterruptedException e) {}
        
        StringBuilder sendPacket=new StringBuilder();
        for (int i = 0; i < packetSize; i++) {
        	sendPacket.append("q");
		}
        final String sendPacketStr=sendPacket.toString();

        //启动测试线程
        bTime = System.currentTimeMillis();
        for(int i=0;i<thrCount;i++){
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while(currentCount.get() < totalCount.get()){
                        try {
                        	Long start=System.nanoTime();
                            Hello hello = helloService.getHello(sendPacketStr);
                            Long end=System.nanoTime();
                            Long constTime=end-start;
                            int andIncrement = currentCount.getAndIncrement();
                            concurrentMap.put(Integer.valueOf(andIncrement), constTime);
                        } catch (Exception e) {
                            failCount.getAndIncrement();
                            logger.error("occur error.",e);
                        }
                    }
                    isRunning = false;
                    logger.error("complete.");
                }
            });
            thread.setName("beanchmark thread-" + i);
            thread.start();
        }

        //启动计数定时器
        Timer timer = new Timer();
        timer.schedule(new CountTask(),1000,1000);

        Result result = new Result();
        result.setResult("start ok.");
        return result;
    }

    /**
     * 统计task
     */

    class CountTask extends TimerTask {

        //上一秒最后记录数
        int lastSecCount = 0;

        @Override
        public void run() {
            try {
                long totalCostTime = (System.currentTimeMillis() - bTime)/1000;
                if(currentCount.get() < totalCount.get()){
                    long thisSecTps = currentCount.get() - lastSecCount;
                    logger.error("current count:{},total time:{},tps:{}.",currentCount.get(),totalCostTime,thisSecTps);
                    lastSecCount = currentCount.get();
                }else{//总计
                    logger.error("###########complete###########"+concurrentMap.size());
                    
                    Collection<Long> values = concurrentMap.values();
                    
					Long total_const_time = 0L;
					List<Long> list = new ArrayList<Long>();
					for (Iterator<Long> iterator = values.iterator(); iterator.hasNext();) {
						Long l =  iterator.next();
						total_const_time = total_const_time + l;
						list.add(l);
						iterator.remove();

					}
					
					logger.error("###########complete###########"+total_const_time);
					Collections.sort(list);
					long min = list.get(0);
					long max = list.get(list.size() - 1);
					long tcount = Long.valueOf(totalCount.get());
					double minTime = (double) (TimeUnit.MICROSECONDS.convert(min, TimeUnit.NANOSECONDS)) / (double) 1000;
					logger.error("min=" + minTime + " ms");
					double maxTime = (double) TimeUnit.MICROSECONDS.convert(max, TimeUnit.NANOSECONDS) / (double) 1000;
	                logger.error("max=" + maxTime + " ms");
	                
					double avgTime = (double) TimeUnit.MICROSECONDS.convert(total_const_time, TimeUnit.NANOSECONDS)
							/ (double) (tcount * 1000);
					DecimalFormat fmt = new DecimalFormat("#.###");
					logger.error("average=" + fmt.format(avgTime) + " ms");
					logger.error("minTime=>{},maxTime=>{},avgTime=>{}", min, max, avgTime);
					
					long totalseconds =  TimeUnit.SECONDS.convert(total_const_time, TimeUnit.NANOSECONDS);

					long totalTps = totalCount.get() / totalCostTime;
					logger.error("totalCostTime=>{}",totalCostTime);
					logger.error("totalseconds=>{}",totalseconds);
					logger.error("total count:{},total time:{},fail count:{},tps:{}.", totalCount.get(),
							total_const_time, failCount.get(), totalTps);

                    this.cancel();
                }
            } catch (Exception e) {
                logger.error("count task error.",e);
            }
        }
    }
    
    public static void main(String args[]){
    	System.out.println("a".getBytes().length);
    	System.out.println(System.nanoTime());
    	System.out.println(System.nanoTime());
    	System.out.println(Long.MAX_VALUE);
    	System.out.println(0.334*1000);
    }
    
}
