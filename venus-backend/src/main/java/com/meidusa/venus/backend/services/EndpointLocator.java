/**
 * 
 */
package com.meidusa.venus.backend.services;

import com.meidusa.venus.Destroyier;
import com.meidusa.venus.exception.EndPointNotFoundException;
import com.meidusa.venus.exception.ServiceNotFoundException;
import com.meidusa.venus.exception.SystemParameterRequiredException;

/**
 * locate an endpoint by service name, endpoint name and parameter names
 * 
 * @author Sun Ning
 * @since 2010-3-4
 */
public interface EndpointLocator extends Destroyier {

    /**
     * 
     * @param serviceName
     * @param endpointName
     * @param paramNames
     * @return
     * @throws EndPointNotFoundException
     */
    EndpointItem getEndpoint(String serviceName, String endpointName, String[] paramNames) throws ServiceNotFoundException, EndPointNotFoundException,
            SystemParameterRequiredException;

    EndpointItem getEndpoint(String apiName) throws ServiceNotFoundException, EndPointNotFoundException, SystemParameterRequiredException;
}
