package com.meidusa.venus.client;

import com.meidusa.venus.Invocation;
import com.meidusa.venus.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * venus路径计算util
 * Created by Zhangzhihua on 2017/9/21.
 */
public class VenusPathUtil {

    private static Logger logger = LoggerFactory.getLogger(VenusPathUtil.class);

    /**
     * 获取方法路径
     * @param invocation
     * @param url
     * @return
     */
    public static String getMethodPath(Invocation invocation, URL url){
        String methodPath = String.format(
                "%s/%s?version=%s&method=%s",
                invocation.getServiceInterfaceName(),
                invocation.getServiceName(),
                invocation.getVersion(),
                invocation.getMethodName()
        );
        logger.info("methodPath:{}.", methodPath);
        return methodPath;
    }
}
