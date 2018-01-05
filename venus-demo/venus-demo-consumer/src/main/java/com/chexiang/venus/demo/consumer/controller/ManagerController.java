//package com.chexiang.venus.demo.consumer.controller;
//
//import com.meidusa.venus.Result;
//import com.meidusa.venus.service.registry.ServiceDefinition;
//import com.meidusa.venus.service.registry.ServiceRegistry;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.List;
//
///**
// * ManagerController
// * Created by Zhangzhihua on 2017/9/25.
// */
//@RestController
//@RequestMapping("/mgr")
//public class ManagerController {
//
//    private static Logger logger = LoggerFactory.getLogger(ManagerController.class);
//
//    @Autowired
//    @Qualifier("serviceRegistryEx")
//    ServiceRegistry serviceRegistry;
//
//    @RequestMapping("/exit")
//    public void exit(){
//        logger.info("exit...");
//        System.exit(0);
//    }
//
//    @RequestMapping("/getReg/{name}")
//    public Result getReg(@PathVariable String name){
//        List<ServiceDefinition> serviceDefinitionList = serviceRegistry.getServiceDefinitions();
//        logger.info("serDefList:{}",serviceDefinitionList);
//        return new Result("ok");
//    }
//
//}
