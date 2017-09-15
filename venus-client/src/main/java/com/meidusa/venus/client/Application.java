package com.meidusa.venus.client;

import com.meidusa.venus.VenusContext;
import com.meidusa.venus.exception.VenusConfigException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;

/**
 * Venus应用定义
 * Created by Zhangzhihua on 2017/9/15.
 */
public class Application implements InitializingBean {

    /**
     * 应用名称
     */
    private String name;

    @Override
    public void afterPropertiesSet() throws Exception {
        valid();
        VenusContext.getInstance().setApplication(name);
    }

    /**
     * 验证名称有效性
     */
    void valid(){
        if(StringUtils.isEmpty(name)){
            throw new VenusConfigException("name not allow empty.");
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
