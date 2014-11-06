package com.martinandersson.javaee.cdi.transactional;

import java.io.IOException;
import java.util.Objects;
import javax.annotation.Resource;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.Transactional;
import static org.junit.Assert.assertEquals;

/**
 * A {@code @Transactional} CDI bean that always crash.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Transactional
public class CrashingBean {
    
    private static volatile Boolean transactionRolledback;
    
    public static boolean transactionRolledBack() {
        try {
            return Objects.requireNonNull(transactionRolledback, "Which transaction??");
        }
        finally {
            transactionRolledback = null;
        }
    }
    
    
    
    @Resource(lookup = "java:comp/TransactionSynchronizationRegistry")
    TransactionSynchronizationRegistry reg;
    
    
    
    /*
     *  --------------------
     * | UNCHECKED THROWERS |
     *  --------------------
     */
    
    public void throwNPE_noThrowsClause() {
        assertInTx();
        
        /*
         * We expect client to invoke this method without a transaction and then
         * use CrashingBean.transactionRolledBack() to query for the transaction
         * status. Thus:
         */
        registerRollbackListener();
        
        throw new NullPointerException("Oops.");
    }
    
    /*
     * We expect client to invoke this method with an active transaction and
     * then query his own transaction for status. So in this method, we do not
     * register the rollback listener but we get his transaction key and assert
     * we really did inherit the same transaction.
     */
    public void throwNPE_noThrowsClause(Object transactionKey) {
        assertCallerTx(transactionKey);
        throw new NullPointerException("Oops.");
    }
    
    public void throwNPE_hasThrowsClause() throws NullPointerException {
        assertInTx();
        registerRollbackListener();
        throw new NullPointerException("Oops.");
    }
    
    public void throwNPE_hasThrowsClause(Object transactionKey) throws NullPointerException {
        assertCallerTx(transactionKey);
        throw new NullPointerException("Oops.");
    }
    
    
    
    /*
     *  ------------------
     * | CHECKED THROWERS |
     *  ------------------
     */
    
    public void throwIOException() throws IOException {
        assertInTx();
        registerRollbackListener();
        throw new IOException("Oops.");
    }
    
    public void throwIOException(Object transactionKey) throws IOException {
        assertCallerTx(transactionKey);
        throw new IOException("Oops.");
    }
    
    
    
    /*
     *  --------------
     * | INTERNAL API |
     *  --------------
     */    
    
    private void assertInTx() {
        assertEquals(Status.STATUS_ACTIVE, reg.getTransactionStatus());
    }
    
    private void assertCallerTx(Object transactionKey) {
        assertInTx();
        assertEquals(transactionKey, reg.getTransactionKey());
    }
    
    private void registerRollbackListener() {
        reg.registerInterposedSynchronization(new Synchronization(){
            @Override public void beforeCompletion() { /* ignored */ }
            @Override public void afterCompletion(int status) {
                transactionRolledback = (status == Status.STATUS_ROLLEDBACK);
            }
        });
    }
}