package com.meidusa.venus.validate.holder;

/**
 * ValueHolder hold a value in it and can be computed by expression.
 * 
 * @author lichencheng.daisy
 * @since 1.0.0-SNAPSHOT
 * 
 */
public interface ValueHolder {

    /**
     * Get the CompoundRoot which holds the objects pushed onto the stack
     * 
     * @return the root
     */
    Object getRoot();

    void setRoot(Object root);

    /**
     * Attempts to set a property on a config in the stack with the given expression using the default search order.
     * 
     * @param expr the expression defining the path to the property to be set.
     * @param value the value to be set into the named property
     */
    void setValue(String expr, Object value);

    String findString(String expr);

    /**
     * Find a value by evaluating the given expression against the stack in the default search order.
     * 
     * @param expr the expression giving the path of properties to navigate to find the property value to return
     * @return the result of evaluating the expression
     */
    Object findValue(String expr);

    /**
     * Find a value by evaluating the given expression against the stack in the default search order.
     * 
     * @param expr the expression giving the path of properties to navigate to find the property value to return
     * @param asType the type to convert the return value to
     * @return the result of evaluating the expression
     */
    Object findValue(String expr, Class<?> asType);

}
