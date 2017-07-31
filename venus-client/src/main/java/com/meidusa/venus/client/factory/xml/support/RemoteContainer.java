package com.meidusa.venus.client.factory.xml.support;

import com.meidusa.toolkit.common.poolable.ObjectPool;
import com.meidusa.toolkit.net.BackendConnectionPool;
import com.meidusa.venus.client.factory.xml.bean.RemoteConfig;

public class RemoteContainer {

    private RemoteConfig remoteConfig;

    private ObjectPool bioPool;

    private BackendConnectionPool nioPool;

    public ObjectPool getBioPool() {
        return bioPool;
    }

    public void setBioPool(ObjectPool bioPool) {
        this.bioPool = bioPool;
    }

    public BackendConnectionPool getNioPool() {
        return nioPool;
    }

    public void setNioPool(BackendConnectionPool nioPool) {
        this.nioPool = nioPool;
    }

    public RemoteConfig getRemoteConfig() {
        return remoteConfig;
    }

    public void setRemoteConfig(RemoteConfig remoteConfig) {
        this.remoteConfig = remoteConfig;
    }

}
