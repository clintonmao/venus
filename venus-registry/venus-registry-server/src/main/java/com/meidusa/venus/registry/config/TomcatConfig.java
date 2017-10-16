package com.meidusa.venus.registry.config;

import org.springframework.stereotype.Component;

/**
 * Created by songcl on 2017/4/28.
 */
@Component
public class TomcatConfig {

    private int port = 8081;

    private String protocol = "org.apache.coyote.http11.Http11NioProtocol";

    private String uriEncoding = "UTF-8";

    private int connectionTimeout = 20000;

    private int maxConnections = 2000;

    private int maxThreads = 700;

    private int acceptCount = 2000;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getUriEncoding() {
        return uriEncoding;
    }

    public void setUriEncoding(String uriEncoding) {
        this.uriEncoding = uriEncoding;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public int getAcceptCount() {
        return acceptCount;
    }

    public void setAcceptCount(int acceptCount) {
        this.acceptCount = acceptCount;
    }
}
