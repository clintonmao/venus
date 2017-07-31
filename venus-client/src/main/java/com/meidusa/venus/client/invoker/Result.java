package com.meidusa.venus.client.invoker;

/**
 * result
 * Created by Zhangzhihua on 2017/7/31.
 */
public class Result {

    private Object object;

    public Result(Object object){
        this.object = object;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }
}
