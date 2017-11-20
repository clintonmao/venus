/**
 * 
 */
package com.meidusa.venus.exception.wrapper;

/**
 * @author Sun Ning
 * 
 */
public interface HttpStatusCodeAwareExceptionWrapperFactory {

    HttpStatusCodeAwareExceptionWrapper getExceptionWrapper(int status, Throwable e);

}
