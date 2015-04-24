package com.martinandersson.javaee.jpa.entitymanagers.containermanaged;

import com.martinandersson.javaee.jpa.entitymanagers.AbstractJTAEntityManagerTest;
import com.martinandersson.javaee.jpa.entitymanagers.EntityManagerExposer;
import com.martinandersson.javaee.jpa.entitymanagers.lib.BeanManagedTx;
import com.martinandersson.javaee.jpa.entitymanagers.lib.ContainerManagedTx;
import com.martinandersson.javaee.jpa.entitymanagers.lib.Product;
import com.martinandersson.javaee.jpa.entitymanagers.lib.Products;
import com.martinandersson.javaee.resources.SchemaGenerationStrategy;
import com.martinandersson.javaee.utils.DeploymentBuilder;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.persistence.TransactionRequiredException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Container-managed entity managers may have a persistence context that is
 * scoped to the current/active JTA transaction<sup>1</sup>.<p>
 * 
 * When the transaction finish, the persistence context is destroyed and all
 * entities in it become detached.<p>
 * 
 * Container-managed entity managers uses a transaction-scoped persistence
 * context by default. Entity changes are flushed to database when the
 * transaction commit.<p>
 * 
 * The persistence context of container-managed entity managers propagate to
 * other application components and their container-managed entity managers
 * together with the active JTA transaction<sup>2</sup> (independent of whether
 * it was the container or the bean that started the transaction).<p>
 * 
 * ..but there is an exception to this rule. A {@code @Stateful} bean that uses
 * an extended persistence context cannot be the target of such a propagation.
 * You'll find more test regarding this topic in {@linkplain ExtendedTest}<p>
 * 
 * 
 * 
 * <h3>Usage of the entity manager outside a transaction</h3>
 * 
 * JPA 2.1, section "3.1.1 EntityManager Interface":
 * <pre>{@code
 * 
 *     The persist, merge, remove, and refresh methods must be invoked within a
 *     transaction context when an entity manager with a transaction-scoped
 *     persistence context is used. If there is no transaction context, the
 *     javax.persistence.TransactionRequiredException is thrown.
 *     
 *     [..]
 * 
 *     The find method (provided it is invoked without a lock or invoked with
 *     LockModeType.NONE) and the getReference method are not required to be
 *     invoked within a transaction. If an entity manager with
 *     transaction-scoped persistence context is in use, the resulting entities
 *     will be detached[.]
 * 
 * }</pre>
 * 
 * JPA 2.1, section "7.9.1 Container Responsibilities":
 * <pre>{@code
 * 
 *     The container must throw the TransactionRequiredException if a
 *     transaction-scoped persistence context is used and the EntityManager
 *     persist, remove, merge, or refresh method is invoked when no transaction
 *     is active.
 * 
 * }</pre>
 * 
 * 
 * 
 * <h3>Note 1</h3>
 * 
 * For more info about JTA, see
 * {@linkplain com.martinandersson.javaee.jpa.entitymanagers ../entitymanagers/package-info.java}.<p>
 * 
 * 
 * <h3>Note 2</h3>
 * 
 * JPA 2.1 specification, section "7.8.2 Container Managed Persistence Contexts":
 * <pre>{@code
 * 
 *     Exactly how the container maintains the association between persistence
 *     context and JTA transaction is not defined.
 * 
 * }</pre>
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class TransactionScopedTest extends AbstractContainerManagedEntityManagerTest
{
    @Deployment
    private static Archive<?> buildDeployment() {
        return new DeploymentBuilder(TransactionScopedTest.class)
                .addPersistenceXMLFile(SchemaGenerationStrategy.DROP_CREATE)
                .add(AbstractContainerManagedEntityManagerTest.class,
                     AbstractJTAEntityManagerTest.class,
                     EntityManagerExposer.class,
                     ContainerManagedTx.class,
                     BeanManagedTx.class,
                     Product.class,
                     Products.class)
                .build();
    }
    
    
    
    /*
     *  --------
     * | FIELDS |
     *  --------
     */
    
    @PersistenceUnit
    EntityManagerFactory emf;
    
    @EJB
    ContainerManagedTx containerTx;
    
    @EJB
    BeanManagedTx beanTx;
    
    
    
    /*
     *  -------
     * | TESTS |
     *  -------
     */
    
    /**
     * Do {@code EntityManager.persist()} within a container-started
     * transaction.<p>
     * 
     * Expect a positive outcome.
     */
    @Test
    public void containerJTAPersist_okay() {
        // One transaction:
        Product cocaCola = containerTx.apply(Products.create("Coca-Cola"));
        clear2ndLevelCache();
        
        final long id = cocaCola.getId();
        
        // Another transaction:
        assertEquals("Container-managed entity manager with an active transaction should have been able to persist,",
                cocaCola, containerTx.apply(Products.findById(id)));
    }
    
    /**
     * Do {@code EntityManager.persist()} within a bean-started transaction.<p>
     * 
     * Expect a positive outcome.<p>
     * 
     * Note here that the entity manager's context will join the transaction and
     * therefore flush to database even if it is the bean itself that has
     * started a transaction.<p>
     * 
     * The transaction must have been started before first use of the entity
     * manager.
     * 
     * @throws Exception never
     */
    @Test
    public void beanJTAPersist_okay() throws Exception {
        Product whiskey = beanTx.applyWithTransaction(Products.create("Whiskey"));
        clear2ndLevelCache();
        
        final long id = whiskey.getId();
        
        assertEquals("Container-managed entity manager with active transaction should have been able to persist,",
                whiskey, containerTx.apply(Products.findById(id)));
    }
    
    /**
     * Do {@code EntityManager.persist()} without an active transaction.<p>
     * 
     * Expect the worst because a transaction-scoped entity manager must
     * be within the scope of an active transaction before he's able to do
     * persist().
     * 
     * @throws Throwable hopefully
     * 
     * @see TransactionScopedTest
     */
    @Test(expected = TransactionRequiredException.class)
    public void persistWithoutTransaction_crash() throws Throwable {
        try {
            beanTx.applyWithoutTransaction(Products.create("Fanta"));
        }
        catch (EJBException e) {
            throw e.getCause();
        }
    }
    
    /**
     * Firstly, call bean1 that persist product. Then make bean1 do a nested
     * call to bean2 and assert that the persistence context was propagated.
     */
    @Test
    public void persistenceContextPropagatedToTransactionScoped() {
        containerTx.accept(outerEM -> {
            Product bathtub = Products.create("Bathtub").apply(outerEM);
            assertTrue(outerEM.contains(bathtub));
            
            containerTx.accept(innerEM -> {
                assertTrue("Persistence context should have been propagated with the transaction,",
                        innerEM.contains(bathtub));
            });
        });
    }
    
    /**
     * Call bean1 that persist and return product to our client code. Then
     * forward the product reference to bean2 that try to remove the product
     * using {@code EntityManager.remove()}.<p>
     * 
     * {@code EntityManager.remove()} require a <strong>managed</strong> entity
     * as argument.<p>
     * 
     * Our client code has no transaction that is inherited by bean2. One
     * transaction will be started and committed by bean1. Another transaction
     * will be started and committed by bean2. Inbetween, the product entity we
     * pass will become detached and we expect that bean2 who call {@code
     * EntityManager.remove()} will fail utterly hard.<p>
     * 
     * Is there a solution we could write in bean2's code? Yes. Issue a DELETE
     * query instead of using {@code EntityManager.remove()} or make the entity
     * managed first by calling, in order of preference, {@code
     * EntityManager.getReference()}<sup>1</sup>, {@code EntityManager.find()},
     * or {@code EntityManager.merge()}<sup>2</sup>.<p>
     * 
     * 
     * 
     * <h3>Note 1</h3>
     * 
     * {@code EntityManager.getReference()} return a managed entity, see JPA
     * 2.1, section "3.2.8 Managed Instances". But, GlassFish and WildFly has
     * differences regarding {@code getReference()}. See test cases that use
     * this method in
     * {@linkplain com.martinandersson.javaee.jpa.entitymanagers.misc.QueryingForEntitiesTest QueryingForEntitiesTest}.<p>
     * 
     * 
     * <h3>Note 2</h3>
     * 
     * {@code EntityManager.merge()} <strong>return</strong> the managed
     * reference. Reference passed in as argument is left as a detached entity.
     * 
     * @throws Throwable hopefully
     */
    @Test(expected = IllegalArgumentException.class)
    public void persistenceContextNotPropagated_inbetweenTransactions() throws Throwable {
        Product temp = containerTx.apply(Products.create("Temp Product 1"));
        clear2ndLevelCache();
        
        try {
            containerTx.accept(Products.remove(temp));
        }
        catch (EJBException e) {
            throw e.getCause();
        }
    }
    
    /**
     * Equivalent to last test, only this will make bean1 call into bean2 who
     * suspend the transaction.<p>
     * 
     * It is expected that the persistence context is not propagated.
     */
    @Test
    public void persistenceContextNotPropagated_transactionNotInherited() {
        boolean propagated = containerTx.apply(Products.create("Soap").andThen(soap ->
                             containerTx.applyWithoutTransaction(noTxEm -> noTxEm.contains(soap))));
        
        assertFalse("Persistence context is not propagated if nested bean suspend the transaction",
                propagated);
    }
    
    
    
    /*
     *  --------------
     * | INTERNAL API |
     *  --------------
     */
    
    @Override
    protected final EntityManagerExposer getBeanBeingTested() {
        return containerTx;
    }
    
    /**
     * Will clear the persistence provider's 2nd level cache (if there is
     * one).<p>
     * 
     * Used only to have a clear conscience after having done persist operations
     * and before subsequent SELECT queries. Should have no effect on the
     * outcome of the tests in this class.
     */
    private void clear2ndLevelCache() {
        emf.getCache().evictAll();
    }
}