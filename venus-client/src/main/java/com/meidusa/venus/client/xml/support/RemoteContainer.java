package com.meidusa.venus.client.xml.support;

import com.meidusa.toolkit.common.poolable.ObjectPool;
import com.meidusa.toolkit.net.BackendConnectionPool;
import com.meidusa.venus.client.xml.bean.Remote;

public class RemoteContainer {

    private Remote remote;

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

    public Remote getRemote() {
        return remote;
    }

    public void setRemote(Remote remote) {
        this.remote = remote;
    }

}
