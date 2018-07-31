package com.meidusa.venus;

import java.util.List;

public interface ConnectionProcesser {

    /**
     * 添加服务
     * @param servicePath
     * @param addressList
     */
    void put(String servicePath,List<String> addressList);

    /**
     * 删除服务
     * @param servicePath
     */
    void remove(String servicePath);

}
