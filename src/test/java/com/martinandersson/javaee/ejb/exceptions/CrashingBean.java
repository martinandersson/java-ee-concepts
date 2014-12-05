package com.martinandersson.javaee.ejb.exceptions;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

/**
 * A crashing bean.<p>
 * 
 * All methods throw {@code ArithmeticException}. Transactional methods accept
 * a boolean {@code deferred} which if it is false, shall cause the bean to
 * crash immediately, otherwise the crash will be deferred until transaction
 * commit.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Stateless
public class CrashingBean
{
    @Resource
    TransactionSynchronizationRegistry reg;
    
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void crash_neverTx() {
        int crash = 1 / 0;
    }
    
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void crash_requiresNewTx(boolean deferred) {
        crash(deferred);
    }
    
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void crash_mandatoryTx(boolean deferred) {
        crash(deferred);
    }
    
    private void crash(boolean deferred) {
        if (deferred) {
            reg.registerInterposedSynchronization(new Synchronization() {
                @Override public void beforeCompletion() {
                    int crash = 1 / 0; }
                @Override public void afterCompletion(int status) { }
            });
        }
        else {
            int crash = 1 / 0;
        }
    }
}
