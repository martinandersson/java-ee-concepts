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
public class Derived3 extends Base3<Integer>
{
    @TransactionAttribute(REQUIRED)
    @Override
    public int foo(Integer ignored) {
        return super.foo(123);
    }
}
