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
public class Derived5 extends Base5
{
    @TransactionAttribute(REQUIRED)
    @Override
    public <T> int foo(T ignored) {
        return super.foo(ignored);
    }
}