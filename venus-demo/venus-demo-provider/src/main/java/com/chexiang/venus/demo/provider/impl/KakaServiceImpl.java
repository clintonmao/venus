package com.chexiang.venus.demo.provider.impl;

import com.chexiang.venus.demo.provider.KakaService;
import org.springframework.stereotype.Component;

/**
 * Created by Zhangzhihua on 2017/10/29.
 */
@Component
public class KakaServiceImpl implements KakaService {

    @Override
    public void kaka() {
        System.out.println("kaka...");
    }
}
