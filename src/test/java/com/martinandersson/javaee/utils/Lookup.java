package com.martinandersson.javaee.utils;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

/**
 * Utility class with an API to look things up using JNDI.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public final class Lookup
{
    private Lookup() {
        // Utility class
    }
    
    public static <T> T globalBean(Class<? extends T> beanType) throws NamingException {
        return InitialContext.doLookup("java:global/" + moduleName() + "/" + beanType.getSimpleName());
    }
    
    public static String moduleName() throws NamingException {
        return InitialContext.doLookup("java:module/ModuleName");
    }
    
    public static TransactionSynchronizationRegistry transactionSyncRegistry() throws NamingException {
        return InitialContext.doLookup("java:comp/TransactionSynchronizationRegistry");
    }
    
    public static UserTransaction userTransaction() throws NamingException {
        return InitialContext.doLookup("java:comp/UserTransaction");
    }
}