package com.meidusa.venus.client.cluster.loadbanlance;

import com.meidusa.venus.Address;
import com.meidusa.venus.client.cluster.loadbanlance.Loadbanlance;

import java.util.List;
import java.util.Random;

/**
 * 随机选择
 * Created by Zhangzhihua on 2017/8/1.
 */
public class RandomLoadbanlance implements Loadbanlance {

    private final Random random = new Random();

    @Override
    public Address select(List<Address> addressList) {
        //TODO 实现及权重因子
        int index = random.nextInt(addressList.size());
        return addressList.get(index);
    }
}
