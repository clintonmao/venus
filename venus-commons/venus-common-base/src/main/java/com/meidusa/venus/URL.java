package com.meidusa.venus;

import org.apache.commons.collections.map.HashedMap;

import java.util.HashMap;
import java.util.Map;

/**
 * 各种地址抽象URL，如
 * 服务注册地址,venus://com.chexiang.order.OrderService/orderService?version=1.0.0&host=192.168.1.1&port=9000
 * 服务订阅地址,subscrible://com.chexiang.order.OrderService/orderService?version=1.0.0&host=192.168.1.2
 * Created by Zhangzhihua on 2017/7/27.
 */
public class URL {

    /**
     * 协议，如venus
     */
    private String protocol;

    /**
     * 路径，如/com.chexiang.order.OrderService/orderService
     */
    private String path;

    /**
     * 服务地址属性映射表，即?后属性<K,V>
     */
    private Map<String,Object> properties = new HashMap<String,Object>();

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    /**
     * 解析url
     * @param url
     * @return
     */
    public static URL parse(String url){
        //TODO
        return null;
    }

    @Override
    public String toString() {
        return "URL{" +
                "protocol='" + protocol + '\'' +
                ", path='" + path + '\'' +
                ", properties=" + properties +
                '}';
    }
}
