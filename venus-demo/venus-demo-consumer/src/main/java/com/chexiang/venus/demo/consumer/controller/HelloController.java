package com.chexiang.venus.demo.consumer.controller;

import com.alibaba.fastjson.JSON;
import com.chexiang.venus.demo.provider.service.HelloService;
import com.chexiang.venus.demo.provider.exception.HelloValidException;
import com.chexiang.venus.demo.provider.exception.InvalidParamException;
import com.chexiang.venus.demo.provider.service.SaleLeadsService;
import com.chexiang.venus.demo.provider.model.Hello;
import com.chexiang.venus.demo.provider.model.SgmSaleLeadsDto;
import com.chexiang.venus.demo.provider.model.SgmSaleLeadsRequest;
import com.meidusa.venus.Result;
import com.meidusa.venus.notify.InvocationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

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

    @RequestMapping("/exit")
    public void exit(){
        logger.info("exit...");
        System.exit(0);
    }

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

    @RequestMapping("/sayHelloForException/{param}")
    public Result sayHelloForException(@PathVariable String param) {
        try {
            int ret = helloService.sayHelloForException(Integer.parseInt(param));
        } catch (HelloValidException e) {
            logger.error("HelloValidException error",e);
        } catch (InvalidParamException e) {
            logger.error("InvalidParamException error",e);
        } catch (NumberFormatException e) {
            logger.error("NumberFormatException error",e);
        }
        return new Result("ok");
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

    @RequestMapping("/queryHello/{name}")
    public Result queryHello(@PathVariable String name){
        try {
            List<Hello> list = helloService.queryHello(name);
            logger.info("list:{}", JSON.toJSONString(list));
            return new Result(list);
        } catch (Exception e) {
            logger.error("e:{}.",e);
            return new Result(e);
        }
    }

    @RequestMapping("/testJson/{name}")
    public Result testJsonFieldAnno(@PathVariable String name){
        try {
            SgmSaleLeadsRequest dto = new SgmSaleLeadsRequest();
            List<SgmSaleLeadsDto> list = new ArrayList<>();
            SgmSaleLeadsDto dtoItem = new SgmSaleLeadsDto();
            dtoItem.setAddress("adress");
            list.add(dtoItem);
            dto.setSaleLeadsList(list);
           saleLeadsService.receiveSgmSaleLeads(dto);
            return new Result("OK");
        } catch (Exception e) {
            logger.error("e:{}.",e);
            return new Result(e);
        }
    }


    @Autowired
    SaleLeadsService saleLeadsService;

}
