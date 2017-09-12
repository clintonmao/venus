package com.meidusa.venus.client.filter.mock;

import com.meidusa.venus.Invocation;
import com.meidusa.venus.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 降级基类
 * Created by Zhangzhihua on 2017/8/30.
 */
public class BaseMockFilter {

    private static Logger logger = LoggerFactory.getLogger(BaseMockFilter.class);

    //降级类型-return
    static final String MOCK_TYPE_RETURN = "MOCK_TYPE_RETURN ";
    //降级类型-throw
    static final String MOCK_TYPE_THROW = "MOCK_TYPE_THROW";
    //降级类型-callback
    static final String MOCK_TYPE_CALLBACK = "MOCK_TYPE_CALLBACK";

    /**
     * 获取方法标识路径
     * @param invocation
     * @param url
     * @return
     */
    String getMethodPath(Invocation invocation, URL url){
        String methodPath = String.format(
                "%s/%s?version=%s&method=%s",
                invocation.getMethod().getDeclaringClass().getName(),
                invocation.getServiceName(),
                "0.0.0",
                invocation.getMethod().getName()
        );
        logger.info("methodPath:{}.", methodPath);
        return methodPath;
    }

    /**
     * 获取降级类型
     * @param invocation
     * @param url
     * @return
     */
    String getMockType(Invocation invocation,URL url){
        //TODO 获取流控类型
        return null;//MOCK_TYPE_RETURN;
    }

}
