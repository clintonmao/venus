package com.meidusa.venus.rpc.router;

import com.meidusa.venus.Address;
import com.meidusa.venus.rpc.Invocation;

import java.util.List;

/**
 * 路由接口
 * Created by Zhangzhihua on 2017/7/31.
 */
public interface Router {

    /**
     * 根据路由规划过滤可使用地址列表 TODO invocation参数定义?
     * @param addressList
     * @param invocation
     * @return
     */
    List<Address> filte(List<Address> addressList, Invocation invocation);
}
