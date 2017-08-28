package com.meidusa.venus.monitor.athena.reporter.impl;

import com.meidusa.venus.monitor.athena.reporter.AthenaMetricReporter;
import com.saic.framework.athena.site.helper.AthenaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by GodzillaHua on 7/3/16.
 */
public class DefaultMetricReporter implements AthenaMetricReporter {

    private static Logger logger = LoggerFactory.getLogger(DefaultMetricReporter.class);

    private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    private static Map<String, AtomicLong> maps = new HashMap<String, AtomicLong>();

    static {
        executor.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try{
                    Iterator<String> iterator = maps.keySet().iterator();
                    while(iterator.hasNext()) {
                        String key = iterator.next();
                        AtomicLong value = maps.get(key);
                        long count = value.get();
                        value.set(0L);
                        if (count == 0L) {
                            continue;
                        }
                        AthenaUtils.getInstance().logMetirc(key, count);
                    }
                }catch (Exception e) {
                    logger.error("upload metric error", e);
                }
            }
        }, 60, 15, TimeUnit.SECONDS);
    }

    public void metric(String key, int count) {
        try{
            AtomicLong value = maps.get(key);
            if (value == null) {
                value = new AtomicLong(1);
                maps.put(key, value);
            }else {
                value.getAndAdd(count);
            }
        }catch (Exception e) {
            logger.error("set metric error", e);
        }

    }
}
