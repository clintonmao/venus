package com.chexiang.venus.demo.consumer.controller;

import com.chexiang.venus.demo.provider.HelloService;
import com.chexiang.venus.demo.provider.HelloValidException;
import com.chexiang.venus.demo.provider.InvalidParamException;
import com.chexiang.venus.demo.provider.model.Hello;
import com.meidusa.fastjson.JSON;
import com.meidusa.fastmark.feature.SerializerFeature;
import com.meidusa.venus.Result;
import com.meidusa.venus.notify.InvocationListener;
import com.saic.ebiz.mdsecenter.carmall.vo.SpuVO;
import com.saic.ebiz.mdsecenter.vo.MdseCityPriceVO;
import com.saic.ebiz.order.service.api.HugePayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/hello")
public class HelloController {

    private static Logger logger = LoggerFactory.getLogger(HelloController.class);

    @Autowired
    HelloService helloService;

    @Autowired
    HugePayService hugePayService;

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

    @RequestMapping("/order/{param}")
    public Result order(@PathVariable String param) throws HelloValidException,InvalidParamException {
        //构造VO
        SpuVO spuVO = new SpuVO();
        spuVO.setBrandName("ABC");
        Map<Long, MdseCityPriceVO> mdseCityPriceMap = new HashMap<>();
        MdseCityPriceVO cityPriceVO = new MdseCityPriceVO();
        cityPriceVO.setStatus(1);
        mdseCityPriceMap.put(new Long(324),cityPriceVO);
        spuVO.setMdseCityPriceMap(mdseCityPriceMap);

        SerializerFeature[] serializerFeature = new SerializerFeature[]{SerializerFeature.WriteNonStringKeyAsString};
        String serialVo = com.meidusa.fastjson.JSON.toJSONString(spuVO,serializerFeature);
        logger.info("serialVo:{}",serialVo);

        Object deseriaVo = JSON.parse(serialVo);
        logger.info("deseriaVo:{}",serialVo);

        boolean ret = false;
        try {
            ret = hugePayService.isMallHugePay(spuVO);
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.info("ret:",ret);
        return new Result("ok");
    }





}
