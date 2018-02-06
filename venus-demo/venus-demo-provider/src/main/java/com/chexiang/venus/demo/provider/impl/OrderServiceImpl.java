package com.chexiang.venus.demo.provider.impl;

import com.chexiang.venus.demo.provider.OrderService;
import com.chexiang.venus.demo.provider.ProductService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

/**
 * Created by Zhangzhihua on 2018/1/16.
 */
public class OrderServiceImpl implements OrderService,InitializingBean{

    @Autowired
    ProductService productService;

    @PostConstruct
    void init(){
    }

    @Override
    public void afterPropertiesSet() throws Exception {
    }

    @Override
    public void addOrder() {
        System.out.println("addOrder...");
        productService.addProduct();
    }
}
