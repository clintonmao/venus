package com.chexiang.venus.demo.provider;

import com.chexiang.venus.demo.provider.model.Hello;
import com.chexiang.venus.demo.provider.model.OrderDO;
import com.meidusa.venus.annotations.Endpoint;
import com.meidusa.venus.annotations.Param;
import com.meidusa.venus.annotations.Service;
import com.meidusa.venus.notify.InvocationListener;

/**
 * Created by Zhangzhihua on 2017/8/15.
 */
@Service(name = "helloService",version = 4,description = "venus hello示例服务")
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

    @Endpoint(name = "getHelloForBench")
    Hello getHelloForBench(@Param(name = "name") byte[] params);
    
    @Endpoint(name = "testOrder")
    OrderDO testOrder(@Param(name = "name") String name,@Param(name = "params") byte[] params);
}
