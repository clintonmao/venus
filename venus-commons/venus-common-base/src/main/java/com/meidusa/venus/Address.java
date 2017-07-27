package com.meidusa.venus;

/**
 * 服务地址
 * Created by Zhangzhihua on 2017/7/27.
 */
public class Address {

    private String host;

    private int port;

    /**
     * 权重
     */
    private int weight;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }
}
