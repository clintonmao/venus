package com.meidusa.venus.client.filter.mock;

import com.meidusa.venus.Filter;
import com.meidusa.venus.Invocation;
import com.meidusa.venus.Result;
import com.meidusa.venus.RpcException;

/**
 * 快速返回放通处理
 * Created by Zhangzhihua on 2017/8/1.
 */
public class ReturnMockFilter implements Filter {

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        //TODO
        return null;
    }

}
