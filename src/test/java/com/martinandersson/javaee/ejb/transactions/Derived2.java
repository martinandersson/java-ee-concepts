package com.martinandersson.javaee.ejb.transactions;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class Derived2 extends Base2
{
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Override
    public int foo() {
        return super.foo();
    }
}
