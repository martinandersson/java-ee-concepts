package com.martinandersson.javaee.ejb.transactions;

import javax.annotation.Resource;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.TransactionSynchronizationRegistry;

/**
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class Base1
{
    @Resource
    TransactionSynchronizationRegistry reg;
    
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public int foo() {
        return reg.getTransactionStatus();
    }
}