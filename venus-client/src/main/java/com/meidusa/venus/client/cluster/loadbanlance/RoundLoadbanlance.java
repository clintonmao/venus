package com.meidusa.venus.client.cluster.loadbanlance;

import com.meidusa.venus.Address;
import com.meidusa.venus.client.cluster.loadbanlance.Loadbanlance;

import java.util.List;

/**
 * 轮询选择
 * Created by Zhangzhihua on 2017/8/1.
 */
public class RoundLoadbanlance implements Loadbanlance {

    private int index = 0;

    @Override
    public Address select(List<Address> addressList) {
        //TODO 实现及权重因子
        Address address = addressList.get(index);
        if(index < addressList.size()){
            index++;
        }else{
            index = 0;
        }
        return address;
    }
}
