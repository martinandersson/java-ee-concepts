package com.martinandersson.javaee.cdi.producers.entitymanager.unsafe;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * This class is [almost] a replica of:
 * 
 * <pre>{@code
 * 
 * https://svn.java.net/svn/javaeetutorial~svn/branches/javaee-tutorial-7.0.5/examples/cdi/producerfields/src/main/java/javaeetutorial/producerfields/db/UserDatabaseEntityManager.java
 * 
 * }</pre>
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Singleton
public class OracleProducer
{
    /*
     * JLS $17.4.5:
     * 
     *     "A call to start() on a thread happens-before any actions in the
     *      started thread."
     * 
     * Hence disposeException is not marked volatile and we can be safe (like
     * 100 %) knowing an exception will not leak to the other thread.
     */
    private static RuntimeException disposeException;
    
    public static RuntimeException consumeDisposeException() {
        try {
            return disposeException;
        }
        finally {
            disposeException = null;
        }
    }
    
    
    private static final AtomicInteger INSTANCE_COUNTER = new AtomicInteger();
    
    public static final int numberOfInstancesCreated() {
        return INSTANCE_COUNTER.get();
    }
    
    
    public OracleProducer() {
        INSTANCE_COUNTER.incrementAndGet();
    }
    
    
    @Produces
    @UserDatabase
    @PersistenceContext
    private EntityManager em;
    
    @PersistenceContext
    private EntityManager forComparison;
    
    
    @PostConstruct
    private void __logEntityManager() {
        OracleTestDriver.log("PRODUCER-POSTCONSTRUCT JPA PROXY", em);
        OracleTestDriver.log("PRODUCER-POSTCONSTRUCT FOR COMPARISON", forComparison);
    }
    
    
    public void close(@Disposes @UserDatabase EntityManager em) {
        OracleTestDriver.log("DISPOSE CDI PROXY", em);
        OracleTestDriver.log("DISPOSE JPA PROXY", em.unwrap(EntityManager.class));
        
        try {
            em.close();
            disposeException = null;
        }
        catch (RuntimeException e) {
            Logger.getLogger("TEST").info(() ->
                    Thread.currentThread().getName() + " DISPOSER CAUGHT EXCEPTION: " + e);
            
            disposeException = e;
            
            /*
             * Weld will log a "WELD-000019: Error destroying an instance.." but
             * the cause is lost.
             */
            throw e;
        }
    }
}