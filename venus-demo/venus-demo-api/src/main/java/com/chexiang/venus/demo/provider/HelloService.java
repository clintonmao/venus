package com.chexiang.venus.demo.provider;

import com.chexiang.venus.demo.provider.model.Hello;
import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.annotations.Param;
import com.meidusa.venus.annotations.Service;
import com.meidusa.venus.notify.InvocationListener;

/**
 * Created by Zhangzhihua on 2017/8/15.
 */
@Service(name = "helloService", versionx = "0.0.1", description = "venus hello示例服务")
public interface HelloService {

    /**
     * sayHello
     * @param name
     */
    @Endpoint(name = "sayHelloWithCallback")
    void sayHello(@Param(name="name") String name,@Param(name="callback")InvocationListener<Hello> invocationListener);

    /**
     * sayHello
     * @param name
     */
    @Endpoint(name = "sayHello")
    void sayHello(@Param(name="name") String name);

    /**
     * getHello
     * @param name
     * @return
     */
    @Endpoint(name = "getHello")
    Hello getHello(@Param(name = "name") String name);
}
