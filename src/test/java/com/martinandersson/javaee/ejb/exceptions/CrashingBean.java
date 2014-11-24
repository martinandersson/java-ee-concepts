package com.martinandersson.javaee.ejb.exceptions;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * A crashing bean.<p>
 * 
 * All methods throw {@code ArithmeticException}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Stateless
public class CrashingBean
{
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void crash_neverTx() {
        int crash = 1 / 0;
    }
    
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void crash_requiresNewTx() {
        int crash = 1 / 0;
    }
    
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void crash_mandatoryTx() {
        int crash = 1 / 0;
    }
}
