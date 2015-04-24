package com.martinandersson.javaee.jpa.entitymanagers.containermanaged;

import com.martinandersson.javaee.jpa.entitymanagers.AbstractJTAEntityManagerTest;
import com.martinandersson.javaee.jpa.entitymanagers.EntityManagerExposer;
import com.martinandersson.javaee.jpa.entitymanagers.lib.ContainerManagedTx;
import com.martinandersson.javaee.jpa.entitymanagers.lib.Product;
import com.martinandersson.javaee.jpa.entitymanagers.lib.Products;
import com.martinandersson.javaee.jpa.entitymanagers.lib.SingletonWithExtended;
import com.martinandersson.javaee.jpa.entitymanagers.lib.StatefulWithExtended1;
import com.martinandersson.javaee.jpa.entitymanagers.lib.StatefulWithExtended2;
import com.martinandersson.javaee.jpa.entitymanagers.lib.StatefulWithExtended3;
import com.martinandersson.javaee.jpa.entitymanagers.lib.StatelessWithExtended;
import com.martinandersson.javaee.jpa.entitymanagers.lib.TransactionalWithExtended;
import com.martinandersson.javaee.resources.SchemaGenerationStrategy;
import com.martinandersson.javaee.utils.DeploymentBuilder;
import com.martinandersson.javaee.utils.Lookup;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.enterprise.inject.spi.CDI;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * The persistence context of an container-managed entity manager may be
 * extended (for application-managed entity managers, the persistence context
 * is always extended).<p>
 * 
 * Only {@code @Stateful} EJB:s may use a container-managed entity manager with
 * an extended persistence context<sup>1</sup>.<p>
 * 
 * An extended persistence context is not bound to one specific transaction
 * (context survive transaction boundaries). The context exist "from the time
 * the EntityManager instance is created until it is closed"<sup>2</sup>.<p>
 * 
 * Speaking of container-managed entity managers, then the context will be bound
 * to the {@code @Stateful} owner and the context is destroyed when the bean is
 * destroyed.<p>
 * 
 * The extended persistence context may propagate to beans that use a
 * transaction-scoped entity manager, effectively making the transaction-scoped
 * persistence context of type extended. See
 * {@linkplain #extendedPersistenceContextPropagateToTransactional()}.<p>
 * 
 * The extended persistence context itself can not be inherited from other
 * beans, whether or not the origin uses a transactional-scoped or extended
 * persistence context. It is "created new" so to speak. But there is one
 * exception to this rule. It is possible for a stateful bean that uses an
 * extended persistence context to inherit an extended persistence context from
 * another stateful bean if the latter bean is initialized within the scope of
 * the former bean<sup>3</sup>.<p>
 * 
 * 
 * 
 * <h3>Usage of the entity manager outside a transaction</h3>
 * 
 * JPA 2.1, section "3.1.1 EntityManager Interface":
 * <pre>{@code
 * 
 *     The find method (provided it is invoked without a lock or invoked with
 *     LockModeType.NONE) and the getReference method are not required to be
 *     invoked within a transaction. [..] if an entity manager with an extended
 *     persistence context is used, they will be managed.
 * 
 * }</pre>
 * 
 * JPA 2.1, section 3.3 "Persistence Context Lifetime and Synchronization Type":
 * <pre>{@code
 * 
 *     When an EntityManager with an extended persistence context is used, the
 *     persist, remove, merge, and refresh operations can be called regardless
 *     of whether a transaction is active. The effects of these operations will
 *     be committed to the database when the extended persistence context is
 *     enlisted in a transaction and the transaction commits.
 * 
 * }</pre>
 * 
 * 
 * 
 * 
 * <h4>Note 1</h4>
 * 
 * This applies only for container-managed entity managers that use an extended
 * persistence context. Application managed entity managers can be used anywhere
 * and their persistence context is extended.
 * 
 * 
 * <h4>Note 2</h4>
 * 
 * See JPA 2.1, section "3.3 Persistence Context Lifetime and Synchronization
 * Type".
 * 
 * 
 * <h4>Note 3</h4>
 * See JPA 2.1, section 7.6.3 "Container-managed Extended Persistence Context"
 * 
 * 
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class ExtendedTest extends AbstractContainerManagedEntityManagerTest
{
    private static final Logger LOGGER = Logger.getLogger(ExtendedTest.class.getName());
    
    @Deployment
    private static Archive<?> buildDeployment() {
        return new DeploymentBuilder(ExtendedTest.class)
                .addPersistenceXMLFile(SchemaGenerationStrategy.DROP_CREATE)
                .add(AbstractContainerManagedEntityManagerTest.class,
                     AbstractJTAEntityManagerTest.class,
                     EntityManagerExposer.class,
                     Product.class,
                     Products.class,
                     Lookup.class,
                     TransactionalWithExtended.class,
                     StatelessWithExtended.class,
                     SingletonWithExtended.class,
                     StatefulWithExtended1.class,
                     StatefulWithExtended2.class,
                     StatefulWithExtended3.class,
                     ContainerManagedTx.class)
                .build();
    }
    
    
    
    @PersistenceUnit
    EntityManagerFactory emf;
    
    
    /*
     * Is the next injected  bean the only one we're testing? No! =) Fact is the
     * tests in this class will test so many different bean types in so many
     * uncollated spots that for increased code readability, I chose not to do
     * field injection but instead have the beans looked up close to where they
     * are used.
     */
    
    @EJB
    ContainerManagedTx containerManagedTx;
    
    
    
    /**
     * Only a {@code @Stateful} EJB is supposed to be able to use a
     * container-managed entity manager with an extended persistence context.<p>
     * 
     * EJB 3.2 specification, section "11.11.1.1 Injection of Persistence
     * Context References":
     * <pre>{@code
     * 
     *     References to container-managed entity managers with extended
     *     persistence contexts can only be injected into stateful session beans.
     * 
     * }</pre>
     * 
     * JPA 2.1 specification, section "7.6.3 Container-managed Extended
     * Persistence Context":
     * <pre>{@code
     * 
     *     A container-managed extended persistence context can only be
     *     initiated within the scope of a stateful session bean. It exists from
     *     the point at which the stateful session bean that declares a
     *     dependency on an entity manager of type
     *     PersistenceContextType.EXTENDED is created, and is said to be bound
     *     to the stateful session bean.
     * 
     * }</pre>
     * 
     * So what happens when a {@code @Transactional} CDI bean use a
     * container-managed entity manager with extended persistence context?<p>
     * 
     * <strong>GlassFish 4.1</strong> crash during runtime and will not survive
     * the call to our {@code @Transactional} bean. Exception thrown is {@code
     * javax.transaction.RollbackException} and no useful information at all can
     * be retrieved from the log.<p>
     * 
     * <strong>WildFly 8.1.0</strong> is even worse<sup>1</sup> and let our code
     * use the {@code @Transactional} with no apparent limitation. The product
     * will successfully be persisted.<p>
     * 
     * This test case has been set to expect a {@code RuntimeException}, making
     * GlassFish pass. From what I can tell, the specifications do not say
     * what is supposed to happen when using this type of entity manager
     * illegally.<p>
     * 
     * 
     * 
     * <h4>Note 1</h4>
     * 
     * By "worse", I mean that WildFly clearly brake the specification. Me
     * personally, I see no harm in braking this rule. The contextual CDI bean
     * is "stateful". Also see:
     * <pre>{@code
     *     https://github.com/javaee-samples/javaee7-samples/issues/238
     * }</pre>
     * 
     * 
     * 
     * TODO: File a WildFly bug.
     */
    @Test(expected = RuntimeException.class)
    public void transactionalCDIBeanCanNotUseExtended() {
        TransactionalWithExtended transactional = CDI.current().select(TransactionalWithExtended.class).get();
        
        try {
            Product desk = transactional.apply(Products.create("Desk"));
            logUnexpectedPersist(desk);
        }
        finally {
            if (transactional != null) {
                CDI.current().destroy(transactional);
            }
        }
    }
    
    /**
     * Continuing on the last test case, what happens if a {@code @Stateless}
     * use a container-managed entity manager with extended persistence
     * context?<p>
     * 
     * <strong>GlassFish 4.1</strong> happily crash during runtime. This time,
     * GlassFish provide more useful logging. The exception thrown is {@code
     * java.lang.IllegalStateException} and it has a message:
     * <pre>{@code
     * 
     *     EntityManager with PersistenceContextType.EXTENDED is not supported
     *     for this bean type
     * 
     * }</pre>
     * 
     * <strong>WildFly 8.1.0</strong>... *drum roll* also crash during runtime!
     * Yaaay =) BUT! With some weird logging. Exception thrown is {@code
     * NullPointerException} at {@code
     * org.jboss.as.jpa.container.ExtendedPersistenceDeepInheritance.findExtendedPersistenceContext()}.<p>
     * 
     * Still, the worrisome is that both servers crash during runtime, not
     * during deployment.
     * 
     * @throws NamingException hopefully not
     */
    @Test(expected = RuntimeException.class)
    public void statelessEJBCanNotUseExtended() throws NamingException {
        StatelessWithExtended ext = Lookup.globalBean(StatelessWithExtended.class);
        Product lamp = ext.apply(Products.create("Lamp"));
        logUnexpectedPersist(lamp);
    }
    
    /**
     * Same results as {@linkplain #statelessEJBCanNotUseExtended()}.
     * 
     * @throws NamingException hopefully not
     */
    @Test(expected = RuntimeException.class)
    public void singletonEJBCanNotUseExtended() throws NamingException {
        SingletonWithExtended ext = Lookup.globalBean(SingletonWithExtended.class);
        Product paper = ext.apply(Products.create("Sheet of Paper"));
        logUnexpectedPersist(paper);
    }
    
    /**
     * A container-managed entity manager with extended persistence context can
     * be used with a {@code @Stateful}, which kind of is the purpose of this
     * entity manager configuration.<p>
     * 
     * Be wary though that JTA transactions that come and go will make the
     * persistence context flush his changes to the database as they commit.<p>
     * 
     * If that is not what you want, don't use transactions until the
     * {@code @Remove} call, demonstrated in the next test case declared after
     * this one.
     * 
     * @throws NamingException hopefully not
     */
    @Test
    public void extendedPersistenceContextFlushOnEveryTransactionCommit() throws NamingException {
        StatefulWithExtended1 stateful = Lookup.globalBean(StatefulWithExtended1.class);
        
        final Product car = stateful.apply(Products.create("Car"));
        clear2ndLevelCache();
        
        /*
         * Car not in container managed repository's persistence context, yet
         * car is found in database (previous call to the stateful bean flushed!).
         */
        assertTrue(car.getId() > 0L);
        assertFalse(containerManagedTx.apply(em -> em.contains(car))); // <-- FALSE
        assertEquals(car, findByIdOf(car));
        
        stateful.remove();
        clear2ndLevelCache();
        
        // Still found..
        assertFalse(containerManagedTx.apply(em -> em.contains(car)));
        assertEquals(car, findByIdOf(car));
    }
    
    /**
     * Bean {@code StatefulWithExtended2} suspend inbound transactions during
     * casual business operations until the {@code @Remove} call which requires
     * a transaction (i.e., transaction will be started and used if no one is
     * active).<p>
     * 
     * Effect: Nothing is flushed to database before the {@code @Remove} call.
     * 
     * @throws NamingException hopefully not
     */
    @Test
    public void extendedPersistenceWithDelayedFlush() throws NamingException {
        StatefulWithExtended2 stateful = Lookup.globalBean(StatefulWithExtended2.class);
        
        final Product coffee = stateful.apply(Products.create("Coffee"));
        clear2ndLevelCache();
        
        /*
         * Coffee not in the persistence context of containerManagedTx and coffe
         * is not found in database (previous call to createAndPersist() did not
         * flush).
         */
        assertTrue(coffee.getId() > 0L);
        assertFalse(containerManagedTx.apply(em -> em.contains(coffee)));
        assertNull(findByIdOf(coffee)); // <-- NULL
        
        stateful.remove();
        clear2ndLevelCache();
        
        // Coffe is found after @Remove because @Remove used a transaction and flushed..
        assertFalse(containerManagedTx.apply(em -> em.contains(coffee)));
        assertEquals(coffee, findByIdOf(coffee));
    }
    
    /**
     * So what if the {@code @Remove} also did not use a transaction? Is changes
     * flushed to database or not? Do the application crash?<p>
     * 
     * JPA 2.1 specification, section "7.6.3 Container-managed Extended
     * Persistence Context":
     * <pre>{@code
     * 
     *     The persistence context is closed by the container when the @Remove
     *     method of the stateful session bean completes (or the stateful
     *     session bean instance is otherwise destroyed).
     * 
     * }</pre>
     * 
     * JPA 2.1, section "7.9.1 Container Responsibilities":
     * <pre>{@code
     * 
     *     The container closes the entity manager by calling
     *     EntityManager.close after the stateful session bean and all other
     *     stateful session beans that have inherited the same persistence
     *     context as the entity manager have been removed.
     *     
     *     [..]
     * 
     *     When EntityManager.close is invoked, the [EntityManager] provider
     *     should release all resources that it may have allocated after any
     *     outstanding transactions involving the entity manager have completed.
     * }</pre>
     * 
     * The specification isn't that clear. But obviously, it would be really
     * strange if things was  flushed/committed to the database outside a
     * transaction. And as this test proves, the application won't crash but
     * nothing is persisted: we "silently" lose everything.
     * 
     * @throws NamingException hopefully not
     */
    @Test
    public void extendedPersistenceContextWithoutTransactions_changesLost() throws NamingException {
        StatefulWithExtended3 stateful = Lookup.globalBean(StatefulWithExtended3.class);
        
        final Product tea = stateful.apply(Products.create("Tea"));
        clear2ndLevelCache();
        
        assertTrue(tea.getId() > 0L);
        assertFalse(containerManagedTx.apply(em -> em.contains(tea)));
        assertEquals(null, findByIdOf(tea));
        
        stateful.remove();
        clear2ndLevelCache();
        
        // Tea is NOT found after @Remove (tea gone missing!)..
        assertFalse(containerManagedTx.apply(em -> em.contains(tea)));
        assertEquals(null, findByIdOf(tea));
    }
    
    /**
     * JPA 2.1 specification, section "7.6.4.1 Requirements for Persistence
     * Context Propagation":
     * <pre>{@code
     * 
     *     If a component is called and the JTA transaction is propagated into
     *     that component:
     *     
     *         If the component is a stateful session bean to which an extended
     *         persistence context has been bound and there is a different
     *         persistence context associated with the JTA transaction, an
     *         EJBException is thrown by the container.
     * 
     * }</pre>
     * 
     * 
     * <strong>GlassFish 4.1</strong> results:
     * 
     * <pre>{@code
     * 
     *     javax.ejb.EJBException: There is an active transactional persistence
     *     context for the same EntityManagerFactory as the current stateful
     *     session bean's extended persistence context
     * 
     * }</pre>
     * 
     * 
     * <strong>WildFly 8.1.0</strong> says.. a bit more:
     * 
     * <pre>{@code
     * 
     *     javax.ejb.EJBException: JBAS011437: Found extended persistence
     *     context in SFSB invocation call stack but that cannot be used because
     *     the transaction already has a transactional context associated with
     *     it.  This can be avoided by changing application code, either
     *     eliminate the extended persistence context or the transactional
     *     context.  See JPA spec 2.0 section 7.6.3.1.  Scoped persistence unit
     *     name=ExtendedTest.war#arquillian-pu, persistence context already in
     *     transaction =org.hibernate.jpa.internal.EntityManagerImpl@16fdcf86,
     *     extended persistence context =ExtendedEntityManager
     *     [ExtendedTest.war#arquillian-pu].
     * 
     * }</pre>
     * 
     * Note that this test try to make a stateful bean, that uses an extended
     * persistence context, inherit a transaction-scoped context - which is
     * illegal. However, it is possible for a stateful bean that uses an
     * extended context to inherit an extended persistence context from another
     * stateful bean if the latter bean is initiated within the scope of the
     * former bean. See section 7.6.3 "Container-managed Extended Persistence
     * Context". Currently, this is not demonstrated/tested in this class.<p>
     * 
     * TODO: Write one test that try to illegally inherit another "foreign"
     * extended context, and one test that legally inherit an extended context
     * according to what has just been said in the last paragraph.
     * 
     * @throws NamingException hopefully not
     * @throws EJBException if all goes well
     */
    @Test(expected = EJBException.class)
    public void persistenceContextCanNotPropagateIntoExtended() throws NamingException {
        StatefulWithExtended1 extended = Lookup.globalBean(StatefulWithExtended1.class);
        
        // Optional:
//        extended.apply(Products.create("Initialize stateful's extended context please.."));
        
        Product fail = containerManagedTx.apply(
                Products.create("Initialize the outer transaction-scoped context please..")
                .andThen(newProductIgnored -> 
                    extended.apply(Products.create("..call into a stateful bean that use extended-scope!"))
                ));
    }
    
    /**
     * Continuing the last test, is the other way around possible? Can a bean
     * that uses a transaction-scoped persistence context inherit an extended
     * context?<p>
     * 
     * There's no technical reason to believe it is not possible. The
     * persistence context is expected to be propagated "as usual" and as this
     * test case show, that is the outcome.<p>
     * 
     * However doable this model is, I feel it is semantically strange. The
     * called bean has declared a dependency on a transaction-scoped entity
     * manager (or rather, persistence context), yet what he get in this test
     * case is an extended entity manager!<p>
     * 
     * JPA 2.1 specification, section "7.6.4.1 Requirements for Persistence
     * Context Propagation":
     * <pre>{@code
     * 
     *     If a component is called and the JTA transaction is propagated into
     *     that component:
     * 
     *         [..] if there is a persistence context associated with the JTA
     *         transaction, that persistence context is propagated and used.
     * 
     * }</pre>
     * 
     * @throws NamingException hopefully not
     */
    @Test
    public void extendedPersistenceContextPropagateToTransactional() throws NamingException {
        StatefulWithExtended1 extended = Lookup.globalBean(StatefulWithExtended1.class);
        
        final Product dinosaur = extended.apply(Products.create("Dinosaur"));
        clear2ndLevelCache();
        
        extended.accept(extendedEM -> {
            
            /*
             * The following two lines is just an assertion made by us that the
             * product is still managed and known by the stateful's persistence
             * context.
             * 
             * Only one of the two statements here would be enough for our use
             * case. I use EntityManager.find() to demonstrate that it is the
             * same reference we get back as the local reference we already
             * store.
             * 
             * Note that assertSame() use identity comparison '==', not
             * Object.equals().
             */
            assertTrue(extendedEM.contains(dinosaur));
            assertSame(dinosaur, extendedEM.find(Product.class, dinosaur.getId()));
            
            /*
             * Same logic is applied on the inside of a @Stateless bean that has
             * declared a dependency on a transaction-scoped entity manager. As
             * you can see, "dinosaur" is found in his persistence context as
             * well.. because the extended context from the @Stateful was
             * propagated! =)
             */
            containerManagedTx.accept(transactionalEM -> {
                assertTrue(transactionalEM.contains(dinosaur));
                assertSame(dinosaur, transactionalEM.find(Product.class, dinosaur.getId()));
            });
        });
    }
    
    
    
    /*
     *  --------------
     * | INTERNAL API |
     *  --------------
     */
    
    private void logUnexpectedPersist(Product product) {
        LOGGER.severe(() -> "Managed to persist product: " + product);
        LOGGER.severe(() -> "Product from database: " + findByIdOf(product));
    }
    
    /**
     * Full implementation:
     * <pre>{@code
     * 
     *     return containerManagedTx.apply(Products.findById(product.getId()));
     * 
     * }</pre>
     * 
     * @param product product who's id should be used
     * 
     * @return product if found, else {@code null}
     */
    private Product findByIdOf(Product product) {
        return containerManagedTx.apply(Products.findById(product.getId()));
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
    
    @Override
    protected final EntityManagerExposer getBeanBeingTested() {
        try {
            return Lookup.globalBean(StatefulWithExtended1.class);
        } catch (NamingException e) {
            return null;
        }
    }
}