package com.meidusa.venus.bus.dispatch;

import com.meidusa.venus.Invocation;
import com.meidusa.venus.Result;
import com.meidusa.venus.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 集群模式分发，除分发操作，还会进行集群容错重试
 * Created by Zhangzhihua on 2017/9/1.
 */
public class BusMessageClusterFailoverDispatcher {

    private static Logger logger = LoggerFactory.getLogger(BusMessageClusterFailoverDispatcher.class);

    BusMessageDispatcher busMessageDispatcher;

    public Result dispatch(Invocation invocation, List<URL> urlList){
        //TODO 容错策略
        for(URL url:urlList){
            return busMessageDispatcher.dispatch(invocation,url);
        }
        return null;
    }
}
