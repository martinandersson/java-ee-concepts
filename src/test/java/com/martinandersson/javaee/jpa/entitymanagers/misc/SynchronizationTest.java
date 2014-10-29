package com.martinandersson.javaee.jpa.entitymanagers.misc;

import com.martinandersson.javaee.jpa.entitymanagers.lib.UnsynchronizedEM;
import com.martinandersson.javaee.jpa.entitymanagers.EntityManagerExposer;
import com.martinandersson.javaee.jpa.entitymanagers.lib.ContainerManagedTx;
import com.martinandersson.javaee.jpa.entitymanagers.lib.Product;
import com.martinandersson.javaee.jpa.entitymanagers.lib.Products;
import com.martinandersson.javaee.resources.SchemaGenerationStrategy;
import com.martinandersson.javaee.utils.Deployments;
import com.martinandersson.javaee.utils.Lookup;
import java.util.Objects;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.persistence.EntityManager;
import javax.persistence.TransactionRequiredException;
import javax.transaction.TransactionSynchronizationRegistry;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Propagation of the persistence context is one thing, actually joining the
 * transaction is another.<p>
 * 
 * WHAT?<p>
 * 
 * Even though the context propagate across component boundaries together with
 * the transaction, the context is a resource that must be enlisted with the JTA
 * transaction or otherwise not see his entity changes flushed when the
 * transaction commit.<p>
 * 
 * Enlisting a context with the JTA transaction is also called "joining" the JTA
 * transaction. By default, all persistence contexts join the active transaction
 * unless {@code SynchronizationType} has explicitly been set to {@code
 * UNSYNCHRONIZED}.<p>
 * 
 * See JPA 2.1 specification, section 3.3.1 "Synchronization with the Current
 * Transaction".<p><p>
 * 
 * 
 * 
 * TODO, test the following:<p>
 * 
 * JPA 2.1, section "7.9.2 Provider Responsibilities":
 * <pre>{@code
 * 
 *     When the JTA transaction rolls back, the provider must detach all
 *     managed entities if the persistence context is of type
 *     SynchronizationType.SYNCHRONIZED or has otherwise been joined to the
 *     transaction.
 * 
 * }</pre>
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
public class SynchronizationTest
{
    private static final Logger LOGGER = Logger.getLogger(SynchronizationTest.class.getName());
    
    @Deployment
    private static Archive<?> buildDeployment() {
        return Deployments.buildPersistenceArchive(
                SchemaGenerationStrategy.DROP_CREATE,
                SynchronizationTest.class,
                ContainerManagedTx.class,
                UnsynchronizedEM.class,
                EntityManagerExposer.class,
                Lookup.class,
                Product.class,
                Products.class);
    }
    
    
    
    @EJB
    ContainerManagedTx synced;
    
    @EJB
    UnsynchronizedEM unsynced;
    
    
    
    /**
     * JPA 2.1, section 7.6.1 "Persistence Context Synchronization Type":
     * <pre>{@code
     * 
     *     The application is permitted to invoke the persist, merge, remove,
     *     and refresh entity lifecycle operations on an entity manager of type
     *     SynchronizationType.UNSYNCHRONIZED independent of whether the
     *     persistence context is joined to the current transaction. After the
     *     persistence context has been joined to a transaction, changes in a
     *     persistence context can be flushed to the database either explicitly
     *     by the application or by the provider.
     * 
     * }</pre>
     * 
     * Part A of the previous quote says it is okay to use persist on a
     * unsynchronized entity manager. Part B says that the entity manager should
     * flush once joined to a transaction.
     */
    @Test
    public void unsynchronizedMayPersist() {
        Product p = unsynced.apply(unsyncedEM -> {
            assertFalse(unsyncedEM.isJoinedToTransaction());
            
            // Persist before join..
            Product phone = Products.create("Phone").apply(unsyncedEM);
            
            assertTrue(phone.getId() > 0L);
            assertTrue(unsyncedEM.contains(phone));
            
            unsyncedEM.joinTransaction(); // <-- .. then join before returning
            return phone;
        });
        
        // Found In db:
        assertEquals(p, synced.apply(Products.findByIdOf(p)));
    }
    
    /**
     * JPA 2.1, section 7.6.1 "Persistence Context Synchronization Type":
     * <pre>{@code
     * 
     *     A persistence context of type SynchronizationType.UNSYNCHRONIZED must
     *     not be flushed to the database unless it is joined to a transaction.
     * 
     * }</pre>
     * 
     * @throws Throwable if all goes well
     */
    @Test(expected = TransactionRequiredException.class)
    public void unsynchronizedMustNotFlushWithoutTransaction() throws Throwable {
        try {
            unsynced.accept(EntityManager::flush);
        }
        catch (EJBException e) {
            throw e.getCause();
        }
    }
    
    /**
     * Persistence context propagated, but not joined to transaction.
     */
    @Test
    public void legalPropagation_butNotJoined() {
        unsynced.accept(outerEM -> {
            
            Product tv = Products.create("TV").apply(outerEM);
            
            assertTrue(outerEM.contains(tv));
            assertFalse(outerEM.isJoinedToTransaction()); // <-- not joined
            
            unsynced.accept(innerEm -> {
                assertTrue(innerEm.contains(tv)); // <-- context propagated
                assertFalse(innerEm.isJoinedToTransaction()); // <-- still not joined
            });
        });
    }
    
    /**
     * JPA 2.1, section "7.6.4.1 Requirements for Persistence Context
     * Propagation":
     * <pre>{@code
     * 
     *     If there is a persistence context of type
     *     SynchronizationType.UNSYNCHRONIZED associated with the JTA
     *     transaction and the target component specifies a persistence context
     *     of type SynchronizationType.SYNCHRONIZED, the IllegalStateException
     *     is thrown by the container.
     * 
     * }</pre>
     * 
     * Both GlassFish 4.1 and WildFly 8.1.0 fail this test.
     * TODO: Report bugs.
     * 
     * @throws Throwable if all goes well
     */
    @Test(expected = IllegalStateException.class)
    public void illegalPropagation() throws Throwable {
        TransactionSynchronizationRegistry reg = Lookup.transactionSyncRegistry();
        
        try {
            unsynced.accept(unsynchedEM -> {
                Product pizza = Products.create("Pizza").apply(unsynchedEM);

                assertFalse("UNSYNCHRONIZED is not supposed to be joined",
                        unsynchedEM.isJoinedToTransaction());

                Object outerKey = reg.getTransactionKey();
                Objects.requireNonNull(outerKey);

                synced.accept(syncedEM -> {

                    // Expected IllegalStateException to have been thrown by now! But:

                    Object innerKey = reg.getTransactionKey();

                    assertEquals("Transaction supposed to be inherited",
                            outerKey, innerKey);

                    assertTrue("Persistence Context illegally propagated :'(",
                            syncedEM.contains(pizza));

                    assertFalse("Both WildFly and GlassFish let a SYNCHRONIZED EM become UNSYNCHRONIZED",
                            syncedEM.isJoinedToTransaction()); // <-- not joined, uberstrange!

                    // HERE is where GlassFish and WildFly fail:
                    fail("Illegal propagation of persistence context, EntityManager=" + syncedEM);
                });
            });
        }
        catch (EJBException e) {
            throw e.getCause();
        }
    }
}