package com.meidusa.venus;

/**
 * 协议接口
 * Created by Zhangzhihua on 2017/11/16.
 */
public interface Protocol {

    /**
     * 初始化
     * @throws Exception
     */
    void init() throws Exception;

    /**
     * 资源释放
     */
    void destroy() throws Exception;
}
