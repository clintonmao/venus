package com.meidusa.venus.support;

import com.meidusa.venus.annotations.Endpoint;

/**
 * 原endpoint注解包装扩展，因原endpoint注解类在vm destroy处理时为NULL
 * Created by Zhangzhihua on 2017/10/13.
 */
public class EndpointWrapper {

    /*
    String name() default "";
    boolean async() default false;
    int timeWait() default 30000;
    String loadbalancingKey() default "";
    */
    private String name = "";
    private boolean async = false;
    private int timeWait = 30000;
    private String loadbalancingKey = "";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public int getTimeWait() {
        return timeWait;
    }

    public void setTimeWait(int timeWait) {
        this.timeWait = timeWait;
    }

    public String getLoadbalancingKey() {
        return loadbalancingKey;
    }

    public void setLoadbalancingKey(String loadbalancingKey) {
        this.loadbalancingKey = loadbalancingKey;
    }

    public static EndpointWrapper wrapper(Endpoint endpoint){
        EndpointWrapper ew = new EndpointWrapper();
        ew.setName(endpoint.name());
        ew.setAsync(endpoint.async());
        ew.setTimeWait(endpoint.timeWait());
        ew.setLoadbalancingKey(endpoint.loadbalancingKey());
        return ew;
    }
}
