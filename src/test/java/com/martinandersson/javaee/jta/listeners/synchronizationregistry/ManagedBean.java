package com.martinandersson.javaee.jta.listeners.synchronizationregistry;

import javax.annotation.Resource;
import javax.transaction.TransactionSynchronizationRegistry;

/**
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@javax.annotation.ManagedBean
public class ManagedBean {
    @Resource(lookup = "java:comp/TransactionSynchronizationRegistry")
    TransactionSynchronizationRegistry txRegistry;
    
    public int getTransactionStatus() {
        return txRegistry.getTransactionStatus();
    }
}