package com.meidusa.venus.support;

/**
 * 性能测试临时用
 * Created by Zhangzhihua on 2017/8/15.
 */
public class Echo {

    private String name;

    private String nick;

    public Echo() {
    }

    public Echo(String name, String nick) {
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
