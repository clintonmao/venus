package com.meidusa.venus.rpc.loadbanlance.round;

import com.meidusa.venus.Address;
import com.meidusa.venus.rpc.loadbanlance.Loadbanlance;

import java.util.List;

/**
 * 轮询选择
 * Created by Zhangzhihua on 2017/8/1.
 */
public class RoundLoadbanlance implements Loadbanlance {

    @Override
    public Address select(List<Address> addressList) {
        return null;
    }
}
