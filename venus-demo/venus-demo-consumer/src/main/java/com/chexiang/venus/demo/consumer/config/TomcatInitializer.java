/**
 * (C) Copyright 2016 Ymatou (http://www.ymatou.com/).
 *
 * All rights reserved.
 */
package com.chexiang.venus.demo.consumer.config;

import java.nio.charset.Charset;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.AbstractProtocol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
public class TomcatInitializer extends SpringBootServletInitializer {

    @Autowired
    private TomcatConfig tomcatConfig;

    @Bean
    public EmbeddedServletContainerFactory createEmbeddedServletContainerFactory() {
        TomcatEmbeddedServletContainerFactory tomcatFactory = new TomcatEmbeddedServletContainerFactory();
        tomcatFactory.setPort(tomcatConfig.getPort());
        tomcatFactory.setProtocol(tomcatConfig.getProtocol());
        tomcatFactory.setUriEncoding(Charset.forName(tomcatConfig.getUriEncoding()));
        tomcatFactory.addConnectorCustomizers(new DefaultTomcatConnectorCustomizer(tomcatConfig));

        return tomcatFactory;
    }
}


class DefaultTomcatConnectorCustomizer implements TomcatConnectorCustomizer {

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
