package com.meidusa.venus.registry.controller;

import com.meidusa.venus.registry.model.Result;
import com.meidusa.venus.registry.service.RegisterService;
import com.meidusa.venus.registry.service.impl.MysqlRegisterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.remoting.caucho.HessianServiceExporter;
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

    @RequestMapping("/echo")
    public Result echo(){
        logger.info("hi,echo.");
        return new Result("hi,echo.");
    }

}
