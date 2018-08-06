package com.chexiang.venus.demo.consumer.controller;

import com.meidusa.venus.Result;
import com.meidusa.venus.client.rpcclient.JsonRpc;
import com.meidusa.venus.registry.Register;
import com.meidusa.venus.registry.RegisterContext;
import com.meidusa.venus.support.VenusContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * HelloController
 * Created by Zhangzhihua on 2017/9/25.
 */
@RestController
@RequestMapping("/rpc")
public class RpcController {

    private static Logger logger = LoggerFactory.getLogger(RpcController.class);

    JsonRpc jsonRpc;

    Object lock = new Object();

    @RequestMapping("/invoke/{name}")
    public Result invoke(@PathVariable String name){
        try {
            String serviceName = "helloService";
            String endpointName = "getHello";
            Map<String,Object> parameterMap = new HashMap<>();
            parameterMap.put("name","zhangzh");
            String result = getJsonRpc().invoke(serviceName,endpointName,parameterMap);
            logger.info("result:{}",result);
            return new Result("OK");
        } catch (Exception e) {
            logger.error("e:{}.",e);
            return new Result(e);
        }
    }

    public JsonRpc getJsonRpc() {
        if(jsonRpc == null){
           synchronized (lock){
               if(jsonRpc == null){
                   String appName = VenusContext.getInstance().getApplication();
                   Register register = RegisterContext.getInstance().getRegister();
                   jsonRpc = new JsonRpc(appName,register);
               }
           }
        }
        return jsonRpc;
    }

    void buildParam(){
        /*
        if (req.getContentLength() > 0) {
                byte[] message = new byte[req.getContentLength()];
                req.getInputStream().read(message);
                parameterMap = JSON.parseObject(new String(message, "UTF-8"));
            } else {
                parameterMap = new HashMap<String, Object>();
            }

            Set<String> keys = req.getParameterMap().keySet();
            for (String key : keys) {
                parameterMap.put(key, req.getParameter(key));
            }
            parameterMap.remove(VERSION);
        */
    }

}
