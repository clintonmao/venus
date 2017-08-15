package com.chexiang.venus.demo.provider.model;

/**
 * Created by Zhangzhihua on 2017/8/15.
 */
public class Hello {

    private String name;

    private String nick;

    public Hello() {
    }

    public Hello(String name, String nick) {
        this.name = name;
        this.nick = nick;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }
}
