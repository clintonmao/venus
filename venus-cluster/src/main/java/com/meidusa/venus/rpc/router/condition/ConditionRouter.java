package com.meidusa.venus.rpc.router.condition;

import com.meidusa.venus.Address;
import com.meidusa.venus.Invocation;
import com.meidusa.venus.rpc.router.Router;

import java.util.List;

/**
 * 条件路由
 * Created by Zhangzhihua on 2017/7/31.
 */
public class ConditionRouter implements Router {

    @Override
    public List<Address> filter(List<Address> addressList, Invocation invocation) {
        //TODO 若没有可用提供者，抛异常
        return null;
    }
}
