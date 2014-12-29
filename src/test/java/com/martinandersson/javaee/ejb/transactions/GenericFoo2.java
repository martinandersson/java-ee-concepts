package com.martinandersson.javaee.ejb.transactions;

/**
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public interface GenericFoo2 {
    public <T> int foo(T ignored);
}