package com.meidusa.venus.registry;

public class RegisterContext {

    private static RegisterContext instance = new RegisterContext();

    private Register register;

    public static RegisterContext getInstance(){
        return instance;
    }

    public Register getRegister() {
        return register;
    }

    public void setRegister(Register register) {
        this.register = register;
    }
}
