package com.chexiang.venus.demo.provider;

import com.meidusa.venus.support.Echo;

/**
 * Created by Zhangzhihua on 2017/10/29.
 */
public interface EchoService {

    void echo(String name);

    Echo getEcho(String name);

}
