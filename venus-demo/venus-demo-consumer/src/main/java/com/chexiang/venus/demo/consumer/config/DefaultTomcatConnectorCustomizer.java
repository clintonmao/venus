package com.chexiang.venus.demo.consumer.config;

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

        // 最大排队数
        connector.setProperty("acceptCount", String.valueOf(tomcatConfig.getAcceptCount()));

        // 最大连接数
        protocol.setMaxConnections(tomcatConfig.getMaxConnections());

        // 最大线程数
        protocol.setMaxThreads(tomcatConfig.getMaxThreads());

        // 连接超时
        protocol.setConnectionTimeout(tomcatConfig.getConnectionTimeout());
    }
}
