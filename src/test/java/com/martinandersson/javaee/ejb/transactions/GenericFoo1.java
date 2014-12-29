package com.martinandersson.javaee.ejb.transactions;

/**
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public interface GenericFoo1<T> {
    public int foo(T ignored);
}
