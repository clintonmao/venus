package com.meidusa.venus.client.filter.limit;

import com.meidusa.venus.Filter;
import com.meidusa.venus.Invocation;
import com.meidusa.venus.Result;
import com.meidusa.venus.RpcException;

/**
 * 并发数流控处理
 * Created by Zhangzhihua on 2017/8/1.
 */
public class ActivesLimitFilter implements Filter {

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        //TODO 判断开启情况
        //FIXME
        return null;
    }
}
