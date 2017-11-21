package com.meidusa.venus;

/**
 * 包验证有效性接口，用于校验jar包加载有效性
 * Created by Zhangzhihua on 2017/11/21.
 */
public interface PackageValid {

    /**
     * 验证是否有效
     * @throws Exception
     */
    void valid() throws Exception;
}
