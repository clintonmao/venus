package com.meidusa.venus.registry.service;

import com.meidusa.venus.URL;
import com.meidusa.venus.registry.RegisterService;
import com.meidusa.venus.registry.VenusRegisteException;
import com.meidusa.venus.service.registry.ServiceDefinition;

/**
 * Created by Zhangzhihua on 2017/8/16.
 */
public class MysqlRegisterService implements RegisterService {

    @Override
    public void registe(URL url) throws VenusRegisteException {

    }

    @Override
    public void unregiste(URL url) throws VenusRegisteException {

    }

    @Override
    public void subscrible(URL url) throws VenusRegisteException {

    }

    @Override
    public void unsubscrible(URL url) throws VenusRegisteException {

    }

    @Override
    public void heartbeat() throws VenusRegisteException {

    }

    @Override
    public ServiceDefinition lookup(URL url) throws VenusRegisteException {
        return null;
    }

    @Override
    public void load() throws VenusRegisteException {

    }

    @Override
    public void destroy() throws VenusRegisteException {

    }
}
