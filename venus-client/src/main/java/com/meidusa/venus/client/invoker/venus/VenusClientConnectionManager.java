package com.meidusa.venus.client.invoker.venus;

import com.meidusa.toolkit.common.runtime.GlobalScheduler;
import com.meidusa.venus.ConnectionProcesser;
import com.meidusa.venus.URL;
import com.meidusa.venus.support.VenusContext;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 客户端连接管理，负责初始化所有服务连接及状态
 */
public class VenusClientConnectionManager implements ConnectionProcesser {

    private static Logger logger = LoggerFactory.getLogger("venus.default");

    //所有服务地址映射表
    private static Map<String,List<String>> serviceAddressesMap = new ConcurrentHashMap<>();

    private VenusClientConnectionFactory connectionFactory = VenusClientConnectionFactory.getInstance();

    private static VenusClientConnectionManager instance;

    private static Object lock = new Object();

    public static VenusClientConnectionManager getInstance(){
        synchronized (lock){
            if(instance == null){
                instance = new VenusClientConnectionManager();
            }
        }
        return instance;
    }

    private VenusClientConnectionManager(){
        init();
    }

    void init(){
        GlobalScheduler.getInstance().scheduleAtFixedRate(new VenusClientConnectionManagerTask(), 0, 10, TimeUnit.SECONDS);

        //设置上下文
        VenusContext.getInstance().setConnectionProcesser(this);
    }

    public void put(String servicePath, final List<String> addressList){
        serviceAddressesMap.put(servicePath,addressList);

        //对于首次添加地址，立即初始化连接
        if(CollectionUtils.isNotEmpty(addressList) && serviceAddressesMap.get(servicePath) == null ){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    initConnectionPool(addressList);
                }
            }).start();
        }

        //处理节点变化事件
        processNodeChanged(servicePath,serviceAddressesMap.get(servicePath),addressList);
    }

    @Override
    public void remove(String servicePath) {
        serviceAddressesMap.remove(servicePath);

        //处理节点变化事件
        processNodeChanged(servicePath,serviceAddressesMap.get(servicePath),new ArrayList<String>());
    }

    /**
     * 初始化连接池，指定地址，用于实时调用
     * @param addressList
     */
    void initConnectionPool(List<String> addressList){
        if(CollectionUtils.isEmpty(addressList)){
            return;
        }
        for(String address:addressList){
            //若连接不存在或无效，则创建
            if(!connectionFactory.isExistConnPool(address) || !connectionFactory.isValidConnPool(address)){
                String[] arr = address.split(":");
                URL url = new URL();
                url.setHost(arr[0]);
                url.setPort(Integer.parseInt(arr[1]));
                connectionFactory.createNioConnPool(url);
            }
        }
    }


    /**
     * 处理服务定义变化事件，释放下线连接资源,新增节点连接初始化由VenusClientConnectionManagerTask实现
     * @param servicePath
     * @param oldAddressList
     * @param newAddressList
     */
    void processNodeChanged(String servicePath, List<String> oldAddressList, List<String> newAddressList){
        //第一次加载
        if(CollectionUtils.isEmpty(oldAddressList)){
            return;
        }

        //新地址映射表
        Map<String,String> newAddressMap = new HashMap();
        if(CollectionUtils.isNotEmpty(newAddressList)){
            for(String newAddress:newAddressList){
                newAddressMap.put(newAddress,newAddress);
            }
        }

        //对比新旧地址，若地址已下线，则释放资源
        for(String oldAddress:oldAddressList){
            if(!newAddressMap.containsKey(oldAddress)){
                releaseConnection(servicePath,oldAddress);
            }
        }

        //logger.info("####process node changed cost time:{}",System.currentTimeMillis()-bTime);
    }

    /**
     * 释放连接资源
     * @param address
     */
    void releaseConnection(String servicePath,String address){
        if(logger.isWarnEnabled()){
            logger.warn("######service:{},node:{} offline,release connection.",servicePath,address);
        }
        if(connectionFactory != null){
            connectionFactory.releaseConnection(address);
        }
    }

    /**
     * 连接管理任务
     */
    class VenusClientConnectionManagerTask implements Runnable{

        @Override
        public void run() {
            try {
                //logger.info("initConnectionPool...");
                initConnectionPool();
            } catch (Throwable e) {
                logger.error("connectionManagerTask errror.",e);
            }
        }

        /**
         * 比较新旧地址列表，对于新增地址初始化连接池
         */
        void initConnectionPool(){
            for(Map.Entry<String,List<String>> entry:serviceAddressesMap.entrySet()){
                List<String> addressList = entry.getValue();
                if(CollectionUtils.isEmpty(addressList)){
                    continue;
                }
                for(String address:addressList){
                    //若连接不存在或无效，则创建
                    if(!connectionFactory.isExistConnPool(address) || !connectionFactory.isValidConnPool(address)){
                        String[] arr = address.split(":");
                        URL url = new URL();
                        url.setHost(arr[0]);
                        url.setPort(Integer.parseInt(arr[1]));
                        connectionFactory.createNioConnPool(url);
                    }
                }

            }
        }
    }
}
