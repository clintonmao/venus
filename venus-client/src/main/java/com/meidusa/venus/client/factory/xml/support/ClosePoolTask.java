package com.meidusa.venus.client.factory.xml.support;

import com.meidusa.toolkit.common.poolable.ObjectPool;
import com.meidusa.toolkit.net.BackendConnectionPool;

import java.util.Map;
import java.util.TimerTask;

/**
 * 关闭连接池task
 * Created by Zhangzhihua on 2017/7/28.
 */
public  class ClosePoolTask extends TimerTask {

    Map<String, Object> pools;

    public ClosePoolTask(Map<String, Object> pools) {
        this.pools = pools;
    }

    @Override
    public void run() {
        for (Map.Entry<String, Object> pool : pools.entrySet()) {
            try {
                if (pool.getValue() instanceof ObjectPool) {
                    ((ObjectPool) pool.getValue()).close();
                } else if (pool.getValue() instanceof BackendConnectionPool) {
                    ((BackendConnectionPool) pool.getValue()).close();
                }
            } catch (Exception e) {
            }
        }
    }
}
