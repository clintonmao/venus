package com.meidusa.venus.rpc.limit;

import com.meidusa.venus.rpc.Filter;
import com.meidusa.venus.rpc.Invocation;
import com.meidusa.venus.rpc.Result;
import com.meidusa.venus.rpc.RpcException;

/**
 * 并发数流控处理
 * Created by Zhangzhihua on 2017/8/1.
 */
public class ActivesLimitFilter implements Filter {

    @Override
    public Result filte(Invocation invocation) throws RpcException {
        //TODO 判断开启情况
        //FIXME
        return null;
    }
}
