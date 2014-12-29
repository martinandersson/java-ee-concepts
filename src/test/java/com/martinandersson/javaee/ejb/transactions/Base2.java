package com.martinandersson.javaee.ejb.transactions;

import javax.annotation.Resource;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.TransactionSynchronizationRegistry;

/**
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class Base2 implements NonGenericFoo
{
    @Resource
    TransactionSynchronizationRegistry reg;
    
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    @Override
    public int foo() {
        return reg.getTransactionStatus();
    }
}
