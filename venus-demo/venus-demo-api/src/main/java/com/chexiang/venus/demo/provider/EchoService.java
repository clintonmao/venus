package com.chexiang.venus.demo.provider;

import com.chexiang.venus.demo.provider.model.Hello;
import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.annotations.Param;
import com.meidusa.venus.annotations.Service;
import com.meidusa.venus.support.Echo;

/**
 * Created by Zhangzhihua on 2017/10/29.
 */
@Service(name = "echoService",version = 1,description = "venus echo示例服务")
public interface EchoService {

    @Endpoint(name = "echo")
    void echo(@Param(name = "name") String name);

    @Endpoint(name = "getEcho")
    Echo getEcho(@Param(name = "name") String name);

}
