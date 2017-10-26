package com.chexiang.venus.demo.provider.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.AbstractProtocol;
import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer;

/**Tomcat连接个性配置
 * Created by Zhangzhihua on 2017/9/25.
 */
public class DefaultTomcatConnectorCustomizer implements TomcatConnectorCustomizer {

    private TomcatConfig tomcatConfig;

    public DefaultTomcatConnectorCustomizer(TomcatConfig tomcatConfig) {
        this.tomcatConfig = tomcatConfig;
    }

    public void customize(Connector connector) {
        AbstractProtocol<?> protocol = (AbstractProtocol<?>) connector.getProtocolHandler();

        // 业务处理最大线程数
        protocol.setMaxThreads(tomcatConfig.getMaxThreads());

        //业务处理core默认线程数
        protocol.setMinSpareThreads(tomcatConfig.getMinSpareThreads());

        //IO接收线程
        protocol.setAcceptorThreadCount(tomcatConfig.getAcceptorThreadCount());

        // 最大排队数
        connector.setProperty("acceptCount", String.valueOf(tomcatConfig.getAcceptCount()));

        // 最大连接数
        protocol.setMaxConnections(tomcatConfig.getMaxConnections());

        // 连接超时
        protocol.setConnectionTimeout(tomcatConfig.getConnectionTimeout());
    }
}
