package com.chexiang.venus.demo.consumer.xstream;

import com.meidusa.venus.client.factory.xml.config.ReferenceService;
import com.meidusa.venus.client.factory.xml.config.VenusClientConfig;
import com.thoughtworks.xstream.XStream;

import java.net.URL;

/**
 * Created by Zhangzhihua on 2017/11/3.
 */
public class XstreamTest {

    void toObject(){
        URL url = this.getClass().getResource("/conf/hello-venus-client.xml");
        XStream xStream = new XStream();
        xStream.autodetectAnnotations(true);
        xStream.processAnnotations(VenusClientConfig.class);
        xStream.processAnnotations(ReferenceService.class);
        Object object = xStream.fromXML(url);
        System.out.println(object);
    }

    public static void main(String[] args){
        new XstreamTest().toObject();
    }

}
