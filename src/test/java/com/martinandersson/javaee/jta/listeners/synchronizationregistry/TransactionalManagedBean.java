package com.martinandersson.javaee.jta.listeners.synchronizationregistry;

import java.util.function.IntConsumer;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.Transactional;

/**
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Transactional
@javax.annotation.ManagedBean
public class TransactionalManagedBean {
    private static final Logger LOGGER = Logger.getLogger(TransactionalManagedBean.class.getName());
    
    @Resource(lookup = "java:comp/TransactionSynchronizationRegistry")
    TransactionSynchronizationRegistry txRegistry;
    
    /*
     * Both of these methods are implicitly: @Transactional(Transactional.TxType.REQUIRED),
     * meaning that a new transaction will be started during each invocation and committed
     * when each method finish.
     */
    
    public int getTransactionStatus() {
        return txRegistry.getTransactionStatus();
    }
    
    public void registerListeners(IntConsumer beforeCompletionStatus, IntConsumer afterCompletionStatus) {
        txRegistry.registerInterposedSynchronization(new Synchronization() {

            @Override public void beforeCompletion() {
                beforeCompletionStatus.accept(txRegistry.getTransactionStatus());
            }

            @Override public void afterCompletion(int status) {
                afterCompletionStatus.accept(status);
            }
        });
    }
}