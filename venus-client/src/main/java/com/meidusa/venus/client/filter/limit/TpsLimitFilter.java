package com.meidusa.venus.client.filter.limit;

import com.meidusa.venus.Filter;
import com.meidusa.venus.Invocation;
import com.meidusa.venus.Result;
import com.meidusa.venus.RpcException;

/**
 * tps流控处理
 * Created by Zhangzhihua on 2017/8/1.
 */
public class TpsLimitFilter implements Filter {

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        //TODO 判断开启状态
        //TODO
        return null;
    }
}
