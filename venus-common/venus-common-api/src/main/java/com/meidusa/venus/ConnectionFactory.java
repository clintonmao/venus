package com.meidusa.venus;

import com.meidusa.toolkit.net.Connection;

/**
 * Created by Zhangzhihua on 2018/3/1.
 */
public interface ConnectionFactory {

    /**
     * 释放连接资源，connetionObserver调用
     * @param conn
     */
    void releaseConnection(Connection conn);

    /**
     * 释放连接资源，MysqlRegister调用
     * @param address
     */
    void releaseConnection(String address);

    /**
     * destroy
     */
    void destroy();
}
