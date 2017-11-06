package com.meidusa.venus.client.factory.xml.config;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Created by Zhangzhihua on 2017/11/4.
 */
@XStreamAlias("method")
public class ReferenceMethod {

    @XStreamAsAttribute
    private String name;

    //超时时间
    @XStreamAsAttribute
    private int timeout;

    //重试次数
    @XStreamAsAttribute
    private int retries;

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
