/**
 * (C) Copyright 2016 Ymatou (http://www.ymatou.com/).
 *
 * All rights reserved.
 */
package com.meidusa.venus.registry.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.Charset;

//@Configuration
public class TomcatInitializer{

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