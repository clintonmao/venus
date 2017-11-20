package com.meidusa.venus;

import java.util.Date;

/**
 * 请求抽象接口
 * Created by Zhangzhihua on 2017/9/19.
 */
public interface Invocation {

    /**
     * 获取服务接口名称
     * @return
     */
    String getServiceInterfaceName();

    /**
     * 获取服务名称
     * @return
     */
    String getServiceName();

    /**
     * 获取版本号
     * @return
     */
    String getVersion();

    /**
     * 获取方法名称
     * @return
     */
    String getMethodName();

    /**
     * 获取请求时间
     * @return
     */
    Date getRequestTime();

}
