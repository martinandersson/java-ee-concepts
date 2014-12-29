package com.martinandersson.javaee.ejb.transactions;

import javax.annotation.Resource;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.TransactionSynchronizationRegistry;

/**
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class Base7<T>
{
    @Resource
    TransactionSynchronizationRegistry reg;
    
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public int foo(T ignored) {
        return reg.getTransactionStatus();
    }
}
