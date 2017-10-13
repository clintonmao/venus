package com.chexiang.venus.demo.provider.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ManagerController
 * Created by Zhangzhihua on 2017/9/25.
 */
@RestController
@RequestMapping("/mgr")
public class ManagerController {

    private static Logger logger = LoggerFactory.getLogger(ManagerController.class);

    @RequestMapping("/exit")
    public void exit(){
        logger.info("exit...");
        System.exit(0);
    }


}
