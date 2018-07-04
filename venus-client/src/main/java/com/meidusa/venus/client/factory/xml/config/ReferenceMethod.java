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
    private String timeout;

    private int timeoutCfg;

    //重试次数
    @XStreamAsAttribute
    private String retries;

    private int retriesCfg;

    //tracer是否打印输入参数
    @XStreamAsAttribute
    private String printParam;

    //tracer是否打印输出参数
    @XStreamAsAttribute
    private String printResult;

    public int getTimeoutCfg() {
        return timeoutCfg;
    }

    public void setTimeoutCfg(int timeoutCfg) {
        this.timeoutCfg = timeoutCfg;
    }

    public int getRetriesCfg() {
        return retriesCfg;
    }

    public void setRetriesCfg(int retriesCfg) {
        this.retriesCfg = retriesCfg;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTimeout() {
        return timeout;
    }

    public void setTimeout(String timeout) {
        this.timeout = timeout;
    }

    public String getRetries() {
        return retries;
    }

    public void setRetries(String retries) {
        this.retries = retries;
    }

    public String getPrintParam() {
        return printParam;
    }

    public void setPrintParam(String printParam) {
        this.printParam = printParam;
    }

    public String getPrintResult() {
        return printResult;
    }

    public void setPrintResult(String printResult) {
        this.printResult = printResult;
    }
}
