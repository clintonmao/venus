/**
 * 
 */
package com.meidusa.venus.backend.serializer;

/**
 * @author sunning
 * 
 */
public abstract class AbstractXMLSerializer implements Serializer {

    /*
     * (non-Javadoc)
     * @see com.meidusa.relation.servicegate.serializer.Serializer#serialize(java.lang.Object)
     */
    public abstract String serialize(Object o) throws Exception;
}
