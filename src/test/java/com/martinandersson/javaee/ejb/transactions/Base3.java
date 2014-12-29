package com.martinandersson.javaee.ejb.transactions;

import javax.annotation.Resource;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.MANDATORY;
import static javax.ejb.TransactionAttributeType.SUPPORTS;
import javax.transaction.TransactionSynchronizationRegistry;

/**
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@TransactionAttribute(SUPPORTS)
public abstract class Base3<T> implements GenericFoo1<T>
{
    @Resource
    TransactionSynchronizationRegistry reg;
    
    @TransactionAttribute(MANDATORY)
    @Override
    public int foo(T ignored) {
        return reg.getTransactionStatus();
    }
}
