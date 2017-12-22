package com.meidusa.venus.client;

import com.meidusa.venus.client.factory.xml.XmlServiceFactory;

/**
 * 为了兼容原来配置，继承XmlServiceFactory
 * Created by Zhangzhihua on 2017/7/27.
 */
public class VenusServiceFactory extends XmlServiceFactory{

    //是否打印引用bean信息
    private boolean printRefBean = true;

    @Override
    public boolean isPrintRefBean() {
        return printRefBean;
    }

    public void setPrintRefBean(boolean printRefBean) {
        this.printRefBean = printRefBean;
    }
}
