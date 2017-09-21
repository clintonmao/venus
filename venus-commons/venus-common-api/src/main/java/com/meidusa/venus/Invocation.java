package com.meidusa.venus;

/**
 * 请求抽象接口
 * Created by Zhangzhihua on 2017/9/19.
 */
public interface Invocation {

    /**
     * 获取服务名称
     * @return
     */
    String getServiceName();

    /**
     * 获取方法名称
     * @return
     */
    String getMethodName();

}
