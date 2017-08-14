package com.meidusa.venus.client.cluster.loadbanlance;

import com.meidusa.venus.Address;
import com.meidusa.venus.client.cluster.loadbanlance.Loadbanlance;

import java.util.List;

/**
 * 随机选择
 * Created by Zhangzhihua on 2017/8/1.
 */
public class RandomLoadbanlance implements Loadbanlance {

    @Override
    public Address select(List<Address> addressList) {
        return null;
    }
}
