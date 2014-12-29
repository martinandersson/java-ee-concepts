package com.martinandersson.javaee.ejb.transactions;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.REQUIRED;
import static javax.ejb.TransactionAttributeType.SUPPORTS;

/**
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Stateless
@TransactionAttribute(SUPPORTS)
public class Derived6 extends Base4<Object>
{
    @TransactionAttribute(REQUIRED)
    @Override
    public int foo(Object ignored) {
        return super.foo(new Object());
    }
}