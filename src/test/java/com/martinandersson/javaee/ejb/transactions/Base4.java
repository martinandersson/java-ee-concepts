/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.martinandersson.javaee.ejb.transactions;

import javax.annotation.Resource;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.NEVER;
import javax.transaction.TransactionSynchronizationRegistry;

/**
 *
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@TransactionAttribute(NEVER)
public abstract class Base4<T> implements GenericFoo1<T>
{
    @Resource
    TransactionSynchronizationRegistry reg;
    
    @TransactionAttribute(NEVER)
    @Override
    public int foo(T ignored) {
        return reg.getTransactionStatus();
    }
}