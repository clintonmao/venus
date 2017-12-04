package com.meidusa.venus;

/**
 * ServiceFactory扩展接口，添加设置目标服务地址方法
 * Created by Zhangzhihua on 2017/10/9.
 */
public interface ServiceFactoryExtra extends ServiceFactory{

    /**
     * 设置地址列表，格式为"192.168.0.1:9000;192.168.0.2:9000"
     * @param ipAddressList
     */
    void setAddressList(String ipAddressList);
}
