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
public abstract class Base5 implements GenericFoo2
{
    @Resource
    TransactionSynchronizationRegistry reg;
    
    @TransactionAttribute(MANDATORY)
    @Override
    public <T> int foo(T ignored) {
        return reg.getTransactionStatus();
    }
}