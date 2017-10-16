package com.meidusa.venus.registry.controller;

import com.meidusa.venus.registry.service.RegisterService;
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
@RequestMapping("/hessian")
public class HessianController {

    private static Logger logger = LoggerFactory.getLogger(HessianController.class);

    @Autowired
    RegisterService registerService;

    @Bean(name = "/registerService")
    public HessianServiceExporter exportRegisterService() {
        HessianServiceExporter exporter = new HessianServiceExporter();
        exporter.setService(registerService);
        exporter.setServiceInterface(RegisterService.class);
        return exporter;
    }

    public RegisterService getRegisterService() {
        return registerService;
    }

    public void setRegisterService(RegisterService registerService) {
        this.registerService = registerService;
    }
}
