package com.meidusa.venus.rpc.limit;

import com.meidusa.venus.rpc.Filter;
import com.meidusa.venus.rpc.Invocation;
import com.meidusa.venus.rpc.Result;
import com.meidusa.venus.rpc.RpcException;

/**
 * tps流控处理
 * Created by Zhangzhihua on 2017/8/1.
 */
public class TpsLimitFilter implements Filter {

    @Override
    public Result filte(Invocation invocation) throws RpcException {
        //TODO 判断开启状态
        //TODO
        return null;
    }
}
