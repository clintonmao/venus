package com.chexiang.venus.demo.consumer.controller;

import com.chexiang.venus.demo.provider.HelloService;
import com.chexiang.venus.demo.provider.HelloValidException;
import com.chexiang.venus.demo.provider.InvalidParamException;
import com.chexiang.venus.demo.provider.model.Hello;
import com.meidusa.venus.Result;
import com.meidusa.venus.notify.InvocationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HelloController
 * Created by Zhangzhihua on 2017/9/25.
 */
@RestController
@RequestMapping("/hello")
public class HelloController {

    private static Logger logger = LoggerFactory.getLogger(HelloController.class);

    @Autowired
    HelloService helloService;

    //@Autowired
    //UniMessageService uniMessageService;

    @RequestMapping("/sayHello")
    public Result sayHello(){
    	helloService.sayHello("jack");
    	return new Result("ok");
    }
    
    @RequestMapping("/sayHelloWithCallback")
    public Result sayHelloWithCallback(){
        helloService.sayHello("jack", new InvocationListener<Hello>() {

            @Override
            public void callback(Hello object) {
                logger.info("Hello:" + object);
            }

            @Override
            public void onException(Exception e) {
                logger.error("e:" + e);
            }
        });
        return new Result("callback.");
    }

    @RequestMapping("/exit")
    public void exit(){
        logger.info("exit...");
        System.exit(0);
    }

    @RequestMapping("/getHello/{name}")
    public Result getHello(@PathVariable String name){
        if("A".equalsIgnoreCase("B")){
            return new Result(new Hello("hi","meme"));
        }
        Hello hello = null;
        try {
            logger.info("testGetHello begin...");
            hello = helloService.getHello(name);
            logger.info("testGetHello end,result:" + hello);
        } catch (Exception e) {
            logger.error("e:{}.",e);
            return new Result(e);
        }
        return new Result(hello);
    }

    @RequestMapping("/cal/{param}")
    public Result cal(@PathVariable String param) throws HelloValidException,InvalidParamException {
        try {
            int ret = helloService.cal(Integer.parseInt(param));
        } catch (HelloValidException e) {
            logger.error("HelloValidException error",e);
        } catch (InvalidParamException e) {
            logger.error("InvalidParamException error",e);
        } catch (NumberFormatException e) {
            logger.error("NumberFormatException error",e);
        }
        return new Result("ok");
    }

    @RequestMapping("/sms/{param}")
    public Result sms(@PathVariable String param) throws HelloValidException,InvalidParamException {
//        Sms sms = new Sms("smsVenus","schemaId1");
//
//        List destPhoneList = new ArrayList();
//        destPhoneList.add("18588888888");
//        sms.setDestPhones(destPhoneList);
//
//        Map<String, String> paramMap = new HashMap<>();
//        paramMap.put("key1","value1");
//        paramMap.put("key2","value2");
//        sms.setParams(paramMap);
//
//        try {
//            uniMessageService.sendSms(sms);
//        } catch (SMSValidateException e) {
//            e.printStackTrace();
//        }
        return new Result("ok");
    }

}
