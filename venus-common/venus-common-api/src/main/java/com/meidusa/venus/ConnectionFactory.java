package com.meidusa.venus;

import com.meidusa.toolkit.net.Connection;

/**
 * Created by Zhangzhihua on 2018/3/1.
 */
public interface ConnectionFactory {

    /**
     * 判断连接池是否存在
     * @param address
     * @return
     */
    boolean isExistConnPool(String address);

    /**
     * 判断连接池是否有效
     * @param address
     * @return
     */
    boolean isValidConnPool(String address);

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
