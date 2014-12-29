package com.martinandersson.javaee.ejb.transactions;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class Derived7 extends Base7<Integer>
{
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Override
    public int foo(Integer ignored) {
        return super.foo(123);
    }
}