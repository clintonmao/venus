package com.meidusa.venus.registry;

/**
 * venus register上下文信息
 * Created by Zhangzhihua on 2017/8/28.
 */
public class RegisterContext {

    private static RegisterContext registerContext;

    /**
     * 注册中心
     */
    private Register register;

    public static RegisterContext getInstance(){
        if(registerContext == null){
            registerContext = new RegisterContext();
        }
        return registerContext;
    }

    public Register getRegister() {
        return register;
    }

    public void setRegister(Register register) {
        this.register = register;
    }
}
