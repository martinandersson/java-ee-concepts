package com.martinandersson.javaee.jpa.entitymanagers.applicationmanaged;

import com.martinandersson.javaee.jpa.entitymanagers.AbstractJTAEntityManagerTest;
import com.martinandersson.javaee.jpa.entitymanagers.EntityManagerExposer;
import com.martinandersson.javaee.jpa.entitymanagers.lib.ContainerManagedTx;
import com.martinandersson.javaee.jpa.entitymanagers.lib.Product;
import com.martinandersson.javaee.jpa.entitymanagers.lib.Products;
import com.martinandersson.javaee.jpa.entitymanagers.lib.StatefulWithExtended1;
import com.martinandersson.javaee.resources.SchemaGenerationStrategy;
import com.martinandersson.javaee.utils.DeploymentBuilder;
import com.martinandersson.javaee.utils.Lookup;
import java.util.function.Function;
import javax.inject.Inject;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * The life cycle of an application-managed entity manager is managed by the
 * application/bean.<p>
 * 
 * The entity manager may use JTA transactions or resource-local
 * transactions<sup>1</sup>.<p>
 * 
 * 
 * 
 * <h3>The persistence context of an application managed entity manager</h3>
 * 
 * JPA 2.1, section "7.7 Application-managed Persistence Contexts":
 * <pre>{@code
 * 
 *     All such application-managed persistence contexts are extended in scope,
 *     and can span multiple transactions.
 *     
 *     [..]
 * 
 *     An extended persistence context obtained from the application-managed
 *     entity manager is a stand-alone persistence context - it is not
 *     propagated with the transaction.
 * 
 * }</pre>
 * 
 * JPA 2.1, section "7.1 Persistence Context" makes it clear what exactly a
 * stand-alone context is:
 * <pre>{@code
 * 
 *     [..] each instance of creating an entity manager causes a new isolated
 *     persistence context to be created that is not accessible through other
 *     EntityManager references within the same transaction.
 * 
 * }</pre>
 * 
 * 
 * 
 * <h3>Controlling the life cycle</h3>
 * 
 * JPA 2.1, section "7.7 Application-managed Persistence Contexts"
 * <pre>{@code
 * 
 *     The EntityManagerFactory.setupEntityManager method and the EntityManager
 *     close and isOpen methods are used to manage the lifecycle of an
 *     application-managed entity manager and its associated persistence
 *     context.
 * 
 *     The extended persistence context exists from the point at which the
 *     entity manager has been created using
 *     EntityManagerFactory.createEntityManager until the entity manager is
 *     closed by means of EntityManager.close.
 * 
 *     [..]
 * 
 *     The EntityManager.close method closes an entity manager to release its
 *     persistence context and other resources. After calling close, the
 *     application must not invoke any further methods on the EntityManager
 *     instance except for getTransaction and isOpen, or the
 *     IllegalStateException will be thrown. If the close method is invoked when
 *     a transaction is active, the persistence context remains managed until
 *     the transaction completes.
 
 }</pre>
 * 
 * JPA 2.1, section "7.9.2 Provider Responsibilities":
 * <pre>{@code
 * 
 *     When EntityManagerFactory.setupEntityManager is invoked, the provider
     must create and return a new entity manager.
 
 }</pre>
 * 
 * 
 * 
 * <h3>The entity manager can only auto join a transaction that is in scope</h3>
 * 
 * JPA 2.1, section "7.7 Application-managed Persistence Contexts"
 * <pre>{@code
 * 
 *     When a JTA application-managed entity manager is used, if the entity
 *     manager is created outside the scope of the current JTA transaction, it
 *     is the responsibility of the application to join the entity manager to
 *     the transaction (if desired) by calling EntityManager.joinTransaction.
 *     If the entity manager is created outside the scope of a JTA transaction,
 *     it is not joined to the transaction unless EntityManager.joinTransaction
 *     is called.
 * 
 * }</pre>
 * 
 * 
 * 
 * <h3>Note 1</h3>
 * 
 * For more info regarding resource-local transactions, see
 * {@code ResourceLocalTest}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
public class JTATest extends AbstractJTAEntityManagerTest
{
    @Deployment
    private static Archive<?> buildDeployment() {
        return new DeploymentBuilder(JTATest.class)
                .addPersistenceXMLFile(SchemaGenerationStrategy.UPDATE)
                .add(AbstractJTAEntityManagerTest.class,
                     EntityManagerExposer.class,
                     Lookup.class,
                     Product.class,
                     Products.class,
                     ContainerManagedTx.class,
                     StatefulWithExtended1.class)
                .build();
    }
    
    
    
    @PersistenceUnit
    EntityManagerFactory emf;
    
    @Inject
    UserTransaction tx;
    
    /**
     * Each test is responsible for setting up this entity manager and when.
     */
    EntityManager em;
    
    
    
    @After
    public void __closeEMAndRollbackActiveTx() throws SystemException {
        if (em != null && em.isOpen()) {
            em.close();
            em = null;
        }
        
        if (tx.getStatus() == Status.STATUS_ACTIVE) {
            rollbackTransaction();
        }
    }
    
    
    
    /*
     *  -------
     * | TESTS |
     *  -------
     */
    
    /**
     * Create a new entity manager within the scope of an active JTA transaction,
     * do persist a new product and expect success.
     */
    @Test
    public void normalUse_createdInsideTransaction() {
        startTransaction();
        setupEntityManager();
        
        assertTrue("Is by default SynchronizationType.SYNCHRONIZED",
                em.isJoinedToTransaction());
        
        Product pen = Products.create("Pen").apply(em); //createPersistProduct("Pen");
        assertTrue("Pen should have an id generated by now",
                pen.getId() > 0L);
        
        commitTransactionSilently();
        
        assertFalse("Transaction committed, persistence context is not joined anymore",
                em.isJoinedToTransaction());
        
        assertTrue("Persistence context of application-managed entity manager is extended",
                em.contains(pen));
    }
    
    /**
     * Create a new entity manager without having a transaction active, then
     * create a transaction and hook up entity manager.<p>
     * 
     * We expect that persisting a new product is a no-brainer.
     */
    @Test
    public void normalUse_createdOutsideTransaction() {
        setupEntityManager();
        startTransaction(); // <-- after creation
        
        assertFalse("Entity manager was created before the transaction",
                em.isJoinedToTransaction()); // <-- false
        
        em.joinTransaction();
        assertTrue(em.isJoinedToTransaction());
        
        Product milk = Products.create("Milk").apply(em);
        assertTrue("milk should have an id generated by now",
                milk.getId() > 0L);
        
        commitTransactionSilently();
        
        assertFalse("Transaction committed, persistence context is not joined anymore",
                em.isJoinedToTransaction());
        
        assertTrue("Persistence context of application-managed entity manager is extended",
                em.contains(milk));
    }
    
    @Test(expected = IllegalStateException.class)
    public void mustNotUseClosedEntityManager() {
        setupEntityManager();
        em.close();
        boolean crashPlease = em.isJoinedToTransaction();
    }
    
    /**
     * JPA 2.1, section "7.7 Application-managed Persistence Contexts":
     * <pre>{@code
     * 
     *     If the close method is invoked when a transaction is active, the
     *     persistence context remains managed until the transaction completes.
     * 
     * }</pre>
     */
    @Test
    public void entitiesManagedAfterEMClosedIfTransactionWasNotCompleted() {
        startTransaction();
        setupEntityManager();
        assertTrue(em.isJoinedToTransaction());
        
        Product alien = Products.create("Alien").apply(em);
        em.close();
        
        // Change name AFTER em has been "closed":
        alien.setName("Something else");
        
        /*
         * But the transaction wasn't closed until now, so we expect that the
         * name change is saved.
         */
        commitTransactionSilently();
        
        // Fetch from db and compare..
        restoreNewEntityManager();
        assertEquals(alien, em.find(Product.class, alien.getId())); // <-- will compare name as well
    }
    
    /**
     * Create entity manager before having a transaction started, persist.<p>
     * 
     * This is legal because the persistence context is extended. But without
     * a transaction, entities are not saved/flushed to database and we loose
     * our work.
     */
    @Test
    public void noTransactionNoFlush_changesLost() {
        setupEntityManager();
        Product car = Products.create("Car").apply(em);
        assertTrue(car.getId() > 0L);
        
        em.close();
        
        restoreNewEntityManager();
        assertNull(em.find(Product.class, car.getId()));
    }
    
    /**
     * The stand-alone persistence context of an application-managed entity
     * manager may join an inbound transaction, but it does not inherit the
     * previously associated persistence context.<p>
     * 
     * This test shows that it is true when going from a container-managed
     * entity manager that uses a transaction-scoped persistence context.
     * 
     * @throws NamingException on JNDI lookup failure
     */
    @Test
    public void standAlonePersistenceContextDoesNotInherit_transactional() throws NamingException {
        ContainerManagedTx stateless = Lookup.globalBean(ContainerManagedTx.class);
        
        stateless.accept(transactionScopedEm -> {
            Product banana = Products.create("Banana").apply(transactionScopedEm);
            assertTrue(transactionScopedEm.contains(banana));
            
            setupEntityManager();
            assertTrue(em.isJoinedToTransaction());
            
            assertFalse(em.contains(banana));
        });
    }
    
    /**
     * The stand-alone persistence context of an application-managed entity
     * manager may join an inbound transaction, but it does not inherit the
     * previously associated persistence context.<p>
     * 
     * This test shows that it is true when going from a container-managed
     * entity manager that uses an extended persistence context.
     * 
     * @throws NamingException on JNDI lookup failure
     */
    @Test
    public void standAlonePersistenceContextDoesNotInherit_extended() throws NamingException {
        StatefulWithExtended1 extended = Lookup.globalBean(StatefulWithExtended1.class);
        
        extended.accept(extendedEm -> {
            Product apple = Products.create("Apple").apply(extendedEm);
            assertTrue(extendedEm.contains(apple));
            
            setupEntityManager();
            assertTrue(em.isJoinedToTransaction());
            
            assertFalse(em.contains(apple));
        });
    }
    
    /**
     * The persistence context of an application-managed entity manager is
     * "stand-alone", meaning it is not propagated anywhere.<p>
     * 
     * This test start a transaction before making a call to a transactional
     * bean that uses a transaction-scoped entity manager. The persistence
     * context is not propagated.<p>
     * 
     * TODO: When resource-local test class is done, then move this type of
     * common tests to a shared superclass, something like
     * "AbstractApplicationManagedEntityManagerTest".
     * 
     * @throws NamingException on JNDI lookup failure
     */
    @Test
    public void standAlonePersistenceContextNotPropagated_joined() throws NamingException {
        startTransaction(); // <-- use our transaction
        setupEntityManager();
        assertTrue(em.isJoinedToTransaction());
        
        ContainerManagedTx stateless = Lookup.globalBean(ContainerManagedTx.class);
        
        TransactionSynchronizationRegistry reg = Lookup.transactionSyncRegistry();
        Object thatKey = reg.getTransactionKey();
        
        Product telephone = Products.create("Telephone").apply(em);
        assertTrue(em.contains(telephone));
        
        stateless.accept(transactionScopedEM -> {
            
            // Be very very sure rite transaction was inherited:
            assertTrue(transactionScopedEM.isJoinedToTransaction());
            Object thisKey = reg.getTransactionKey();
            assertEquals(thatKey, thisKey);
            
            /*
             * Should be noted that CMT (container-managed tx) inherit from BMT
             * (bean-managed tx), but it is not possible the other way around.
             * Fact is BMT does not inherit transactions at all (EJB 3.2,
             * section 8.6.1).
             */
            
            assertFalse(transactionScopedEM.contains(telephone)); // <-- false
        });
        
        
    }
    
    /**
     * The persistence context of an application-managed entity manager is
     * "stand-alone", meaning it is not propagated anywhere.<p>
     * 
     * This test is a variant of {@linkplain
     * #standAlonePersistenceContextNotPropagated_joined()} but uses no
     * transaction before making same bean call. The persistence
     * context is not propagated.
     * 
     * @throws NamingException on JNDI lookup failure
     */
    @Test
    public void standAlonePersistenceContextNotPropagated_notJoined() throws NamingException {
        setupEntityManager();
        
        ContainerManagedTx stateless = Lookup.globalBean(ContainerManagedTx.class);
        
        Product telephone = Products.create("Telephone").apply(em);
        assertTrue(em.contains(telephone));
        
        stateless.accept(transactionScopedEM -> {
            assertTrue(transactionScopedEM.isJoinedToTransaction()); // <-- bean's own transaction
            assertFalse(transactionScopedEM.contains(telephone));
        });
    }
    
    
    
    /*
     *  --------------
     * | INTERNAL API |
     *  --------------
     */
    
    /**
     * Create a default-synchronized JTA entity manager.
     */
    private void setupEntityManager() {
        if (em != null) {
            throw new IllegalStateException();
        }
        
        em = emf.createEntityManager();
    }
    
    private void restoreNewEntityManager() {
        if (em == null) {
            throw new IllegalStateException();
        }

        emf.getCache().evictAll();
        
        em = null;
        setupEntityManager();
    }
    
    private void startTransaction() {
        try {
            if (tx.getStatus() != Status.STATUS_NO_TRANSACTION) {
                throw new IllegalStateException();
            }
            
            tx.begin();
        }
        catch (NotSupportedException | SystemException e) {
            throw new AssertionError(e);
        }
    }
    
    private void rollbackTransaction() {
        try {
            tx.rollback();
        }
        catch (SecurityException | IllegalStateException | SystemException e) {
            throw new AssertionError(e);
        }
    }
    
    private void commitTransaction() throws RollbackException, HeuristicMixedException, HeuristicRollbackException {
        try {
            tx.commit();
        }
        catch (SecurityException | IllegalStateException | SystemException e) {
            throw new AssertionError(e);
        }
    }
    
    private void commitTransactionSilently() {
        try {
            commitTransaction();
        }
        catch (RollbackException | HeuristicMixedException | HeuristicRollbackException e) {
            throw new AssertionError(e);
        }
    }
    
    
    
    /*
     *  -----------
     * | OVERRIDES |
     *  -----------
     */
    
    @Override
    protected final EntityManagerExposer getBeanBeingTested() {
        return new EntityManagerExposer() {
            @Override public <R> R apply(Function<EntityManager, R> entityManagerFunction) {
                EntityManager em = emf.createEntityManager();
                return entityManagerFunction.apply(em);
            }
        };
    }
}