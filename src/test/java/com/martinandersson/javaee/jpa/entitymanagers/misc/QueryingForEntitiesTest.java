package com.martinandersson.javaee.jpa.entitymanagers.misc;

import com.martinandersson.javaee.jpa.entitymanagers.EntityManagerExposer;
import com.martinandersson.javaee.jpa.entitymanagers.lib.BeanManagedTx;
import com.martinandersson.javaee.jpa.entitymanagers.lib.ContainerManagedTx;
import com.martinandersson.javaee.jpa.entitymanagers.lib.Product;
import com.martinandersson.javaee.jpa.entitymanagers.lib.Products;
import com.martinandersson.javaee.resources.SchemaGenerationStrategy;
import com.martinandersson.javaee.utils.Deployments;
import com.martinandersson.javaee.utils.Lookup;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.transaction.Status;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * I am under the impression that many Java EE applications are over-using JTA
 * transactions. Issuing a SELECT statement doesn't require a transaction. What
 * did the developer have in mind rolling back? The result? =)<p>
 * 
 * Just remember that the entities fetched from the outside of a transaction,
 * <strong>will not be managed</strong>.<p>
 * 
 * This test suite will also have a look at {@linkplain FlushModeType}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
public class QueryingForEntitiesTest
{
    private static final Logger LOGGER = Logger.getLogger(QueryingForEntitiesTest.class.getName());
    
    @Deployment
    private static Archive<?> buildDeployment() {
        return Deployments.buildPersistenceArchive(
                SchemaGenerationStrategy.DROP_CREATE,
                QueryingForEntitiesTest.class,
                ContainerManagedTx.class,
                BeanManagedTx.class,
                EntityManagerExposer.class,
                Lookup.class,
                Product.class,
                Products.class);
    }
    
    
    
    @EJB
    ContainerManagedTx containerManaged;
    
    @EJB
    BeanManagedTx beanManaged;
    
    
    
    static Product p1, p2;
    
    /**
     * Create and persist dummies {@code p1} and {@code p2}.
     */
    @Test
    @InSequence(1)
    public void givenProductsExists() {
        containerManaged.accept(em -> {
            p1 = Products.create("Product #1").apply(em);
            p2 = Products.create("Product #2").apply(em);
        });
        
        LOGGER.info(() -> "Persisted p1: " + p1);
        LOGGER.info(() -> "Persisted p2: " + p2);
    }
    
    
    
    /*
     *  ---------------------------------------------------
     * | QUERYING FOR ENTITIES WITHOUT USING A TRANSACTION |
     *  ---------------------------------------------------
     */
    
    /**
     * Will use {@code EntityManager.find()} with no active transaction.
     */
    @Test
    @InSequence(99)
    public void findSingle_noTx_usingEMFind() {
        beanManaged.acceptWithoutTransaction(em -> {
            Product p = Products.findByIdOf(p1).apply(em);
            assertNotNull(p);
            
            // NOT managed:
            assertFalse(em.contains(p));

            assertEquals(p1, p);
        });
    }
    
    /**
     * JPA 2.1, section 3.1.1 "EntityManager Interface":
     * <pre>{@code
     * 
     *     The find method (provided it is invoked without a lock or invoked
     *     with LockModeType.NONE) and the getReference method are not required
     *     to be invoked within a transaction. If an entity manager with
     *     transaction-scoped persistence context is in use, the resulting
     *     entities will be detached; if an entity manager with an extended
     *     persistence context is used, they will be managed.
     * 
     * }</pre>
     * 
     * Both GlassFish 4.1 and WildFly 8.1.0 pass this test to a certain extent.
     * They manage to lookup the entity, but for GlassFish, the "entity"
     * returned is managed despite not using a transaction which is a clear
     * violation of the specification.<p>
     * 
     * Read more in source code comments.<p>
     * 
     * TODO: File a GlassFish bug.<p>
     * 
     * 
     * <h3>note:</h3>
     * This test is one that uses {@code p1}, which is setup in the first "test"
     * case executed ({@code givenProductsExists()}). Thus, this test cannot be
     * executed by itself.
     * 
     * @throws javax.naming.NamingException hopefully never
     * 
     * @see #findSingle_useTx_usingGetReference()
     */
    @Test
    @InSequence(99)
    public void findSingle_noTx_usingGetReference() throws NamingException {
        TransactionSynchronizationRegistry reg = Lookup.transactionSyncRegistry();
        
        beanManaged.acceptWithoutTransaction(em -> {
            assertEquals(Status.STATUS_NO_TRANSACTION, reg.getTransactionStatus());
            
            Product p = em.getReference(Product.class, p1.getId());
            assertNotNull(p);
            
            // If EntityNotFoundException hasn't been thrown already, it may happen now:
            long id = p.getId();
            
            /*
             * So far, both GlassFish and WildFly survive which is a good thing.
             * We managed to lookup the entity using getReference() without
             * using a transaction. But what happens next is another story.
             */
            
            /*
             * Real id of p1 is 1 for both GlassFish and WildFly. Executing next
             * statement, WildFly print 0 (bad) and GlassFish print 1 (good).
             * 
             * 1 is what we expected even for a "skeleton/proxy" like the
             * reference returned from getReference().
             * 
             * IIRC, it is said somewhere that the primary key must always be
             * fetched and shall under no circumstances be left out. Had a look
             * in the specification and couldn't find that so I dare not make
             * an assertion about the load state in this context. However,
             * because of a zeroed id, WildFly will fail
             * findSingle_useTx_usingGetReference().
             */
            LOGGER.info(() -> "Id of product found using getReference(): " + id);
            
            /*
             * But! According to the quote in the JavaDoc of this test, the
             * returned reference must not be managed and here's where
             * everything goes to hell for GlassFish.
             */
            
            boolean managed = em.contains(p);
            LOGGER.info(() -> "Product from getReference() is managed? " + managed); // WF: No. GF: Yeah sure!
            
            assertFalse(managed);
        });
    }
    
    /**
     * Will find a single product by means of building and executing a {@code
     * CriteraQuery<Product>} (which in turn is built into a {@code
     * TypedQuery<Product>} just before execution).<p>
     * 
     * Not using a transaction.
     */
    @Test
    @InSequence(99)
    public void findSingle_noTx_usingQuery() {
        beanManaged.acceptWithoutTransaction(em -> {
            Product p = Products.findByUniqueName("Product #1").apply(em);
            assertNotNull(p);
            assertFalse(em.contains(p));
            assertEquals(p1, p);
        });
    }
    
    /**
     * Will use a {@code CriteriaQuery<Product>} to go looking for all products
     * in the database.<p>
     * 
     * Not using a transaction.
     */
    @Test
    @InSequence(99)
    public void findMultiple_noTx_usingQuery() {
        beanManaged.acceptWithoutTransaction(em ->
                assertAllProductsFound(false, em));
    }
    
    
    
    /*
     *  ----------------------------
     * | WHEN ARE ENTITIES MANAGED? |
     *  ----------------------------
     */
    
    /**
     * Firstly, as all previous test cases has shown, entities returned from the
     * entity manager are NOT managed if they were queried after without using a
     * transaction.<p>
     * 
     * This test demonstrate the most basic JPA operations that a Java EE
     * application can do. While using a transaction, we go look for an entity
     * using {@code EntityManager.find()} and expect to find a managed instance.
     */
    @Test
    @InSequence(99)
    public void findSingle_activeTx_usingEMFind() {
        containerManaged.accept(em -> {
            Product p = Products.findByIdOf(p1).apply(em);
            assertNotNull(p);
            assertTrue(em.contains(p));
        });
    }
    
    /**
     * Continuing on the previously declared test case, this one demonstrate
     * that looking for many entities using queries instead of {@code
     * EntityManager.find()} has the same outcome: all entities found and
     * returned are managed.
     */
    @Test
    @InSequence(99)
    public void findManyUsingTx() {
        containerManaged.accept(em ->
                assertAllProductsFound(true, em));
    }
    
    /**
     * Using getReference() within a transaction, we expect to receive a managed
     * entity or at least one entity-like thing that has a traversable state.<p>
     * 
     * GlassFish 4.1: pass.<br>
     * WildFly 8.1.0 and 8.2.0: fail.<p>
     * 
     * This result is opposite of
     * {@linkplain #findSingle_noTx_usingGetReference() findSingle_noTx_usingGetReference}.<p>
     * 
     * TODO: Report WildFly bug.<p>
     * 
     * 
     * <h3>note:</h3>
     * This test is one that uses {@code p1}, which is setup in the first "test"
     * case executed ({@code givenProductsExists()}). Thus, this test cannot be
     * executed by itself.
     */
    @Test
    @InSequence(99)
    public void findSingle_useTx_usingGetReference() {
        containerManaged.accept(em -> {
            Product p = em.getReference(Product.class, p1.getId());
            assertTrue(em.contains(p));
            
            // WildFly's toString() actually print id = 1!
            LOGGER.info(() -> "Within a transaction, getReference() returned: " + p);
            
            // But .. fail here* (returned id is zero):
            assertEquals(p1.getId(), p.getId());
            
            // *don't do .equals() with a proxy and a real product, Product.equals() use getClass().
        });
    }
    
    /**
     * JavaDoc of {@linkplain FlushModeType} says that by default:
     * <pre>{@code
     * 
     *     [..] the persistence provider is responsible for ensuring that all
     *     updates to the state of all entities in the persistence context which
     *     could potentially affect the result of the query are visible to the
     *     processing of the query.
     * 
     * }</pre>
     * 
     * Most databases use a transaction isolation level of "read
     * committed"<sup>1</sup>. That is what JPA assume<sup>2</sup> and that is
     * what Java DB use by default<sup>3</sup>.
     * 
     * Wikipedia<sup>4</sup> says read committed..
     * <pre>{@code
     * 
     *      [..] guarantees that any data read is committed at the moment it is
     *      read.
     * 
     * }</pre>
     * 
     * Taking those words literally can be problematic. What is the transaction
     * isolated from? Other transactions! As this test demonstrates, we can
     * still SELECT what has been written in our own transaction.<p>
     * 
     * For the negation of this test, see {@linkplain #flushModeCommit()}.<p>
     * 
     * 
     * 
     * <h4>Note 1</h4>
     * {@code java.sql.Connection.TRANSACTION_READ_COMMITTED}.
     * 
     * <h4>Note 2</h4>
     * JPA 2.1, section "3.4 Locking and Concurrency".
     * 
     * <h4>Note 3</h4>
     * http://docs.oracle.com/javadb/10.10.1.2/ref/rrefjavcsti.html
     * 
     * <h4>Note 3</h4>
     * http://en.wikipedia.org/wiki/Isolation_(database_systems)#Read_committed
     * 
     * @throws Exception on a rainy day
     */
    @Test
    @InSequence(99)
    public void flushModeAuto() throws Exception {
        UserTransaction tx = Lookup.userTransaction();
        tx.begin();
        
        try {
            containerManaged.accept(em -> {
                Product airplane1 = Products.create("Airplane").apply(em);
                assertTrue(airplane1.getId() > 0L);
                assertTrue(em.contains(airplane1));
                
                assertSame(FlushModeType.AUTO, em.getFlushMode());
                
                // Using a query instead of find(), we still get back the same managed ref:
                assertSame(airplane1, Products.findByUniqueName("Airplane").apply(em));
                
                em.clear();
                em.getEntityManagerFactory().getCache().evict(Product.class, airplane1.getId());
                em.getEntityManagerFactory().getCache().evictAll();
                
                // Still no problem..
                Product airplane2 = Products.findByUniqueName("Airplane").apply(em);
                assertTrue(em.contains(airplane2));
                assertEquals(airplane1, airplane2);
                assertNotSame(airplane1, airplane2);
            });
        }
        finally {
            tx.rollback();
        }
    }
    
    /**
     * This is a continuation of {@linkplain #flushModeAuto()}. This test
     * demonstrates that we can not found in database what has not been flushed.
     * 
     * @throws Exception on a rainy day
     */
    @Test
    @InSequence(99)
    public void flushModeCommit() throws Exception {
        UserTransaction tx = Lookup.userTransaction();
        tx.begin();
        
        try {
            containerManaged.accept(em -> {
                Product airplane1 = Products.create("Airplane").apply(em);
                assertTrue(airplane1.getId() > 0L);
                assertTrue(em.contains(airplane1));
                
                em.setFlushMode(FlushModeType.COMMIT);
                
                assertNull(Products.findByUniqueName("Airplane").apply(em));
            });
        }
        finally {
            tx.rollback();
        }
    }
    
    
    
    
    /*
     *  --------------
     * | INTERNAL API |
     *  --------------
     */
    
    private void assertAllProductsFound(boolean expectManaged, EntityManager em) {
        List<Product> all = Products.findAll().apply(em);
        all.stream().forEach(p -> {
            assertNotNull(p);
            
            if (expectManaged) {
                assertTrue(em.contains(p)); }
            else {
                assertFalse(em.contains(p)); }
        });

        // initializer block in anonymous class, only for short, lazy & dumb test code:
        Set<Product> products = new HashSet<Product>(){{add(p1); add(p2);}};
        assertEquals(products, new HashSet<>(all));
    }
}