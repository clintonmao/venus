package com.meidusa.venus.client.loadbanlance;

import com.meidusa.venus.Address;

import java.util.List;

/**
 * loadbanlance接口
 * Created by Zhangzhihua on 2017/8/1.
 */
public interface Loadbanlance {

    /**
     * 选择目标地址
     * @param addressList
     * @return
     */
    Address select(List<Address> addressList);
}
