/**
 * <h2>Introduction</h2>
 * 
 * This document is a high-level introduction to entity managers and persistence
 * contexts. For details, see the actual test files in this package and their
 * JavaDoc.<p>
 * 
 * This document do not describe how to configure a persistence unit, how to
 * define a datasource, nor does it describe how to package and deploy
 * applications that use a database. For a great introduction to all these
 * topics, you should first read and understand {@linkplain
 * com.martinandersson.javaee.arquillian.persistence.PersistenceTest PersistenceTest}.<p>
 * 
 * All references made to a "section" in this document refer to the JPA 2.1
 * specification unless otherwise noted.<p>
 * 
 * 
 * 
 * <h2>Order of study</h2>
 * 
 * 1. Read this file. Then:
 * 
 * <ol start="2">
 *   <li>{@linkplain com.martinandersson.javaee.jpa.entitymanagers.containermanaged.TransactionScopedTest containermanaged/TransactionScopedTest}</li>
 *   <li>{@linkplain com.martinandersson.javaee.jpa.entitymanagers.containermanaged.ExtendedTest containermanaged/ExtendedTest}</li>
 *   <li>{@linkplain com.martinandersson.javaee.jpa.entitymanagers.applicationmanaged.JTATest applicationmanaged/JTATest}</li>
 *   <li>TODO: applicationmanaged.ResourceLocalTest!</li>
 *   <li>{@linkplain com.martinandersson.javaee.jpa.entitymanagers.misc.QueryingForEntitiesTest misc/QueryingForEntitiesTest}</li>
 *   <li>{@linkplain com.martinandersson.javaee.jpa.entitymanagers.misc.SynchronizationTest misc/SynchronizationTest}</li>
 * </ol>
 * 
 * 
 * 
 * <h2>Managed versus detached JPA entities</h2>
 * 
 * One cool thing about "managed entities" is that application code can
 * mutate the state of a managed entity and rest assured that all changes will
 * be saved to the database sooner or later (there are exceptions to this rule
 * as will be demonstrated by tests in this package<sup>1</sup>). The
 * application need not bother about the underlying JDBC connection or know how
 * to issue SQL statements for the updates to take effect.<p>
 * 
 * One optimization that many persistence providers can do when loading an
 * entity from the database, is to fetch some parts, or state, of the entity
 * "lazily". Doing so means loading the state only upon first being requested
 * (traversed). For many applications, this saves a few database round trips.<p>
 * 
 * On the other hand, some state is "eagerly" fetched. That is actually a
 * requirement for all unannotated entity fields (so called {@code @Basic}
 * fields) and fields that represent "single-valued" associations with other
 * entities ({@code @OneToOne}, {@code @ManyToOne}).<p>
 * 
 * Lazy data loading, whether by default or set by the application, is only a
 * hint to the provider who may ignore the hint and do eager loading instead.
 * On the other hand, whenever eager loading has been configured, then the
 * provider must honor and use this strategy. Some stuff that by default is
 * eagerly fetched was listed in the last paragraph. Other things such as
 * multi-valued associations ({@code @OneToMany}, {@code @ManyToMany}) and
 * collections of basic java types ({@code @ElementCollection}) may be fetched
 * lazily, and that is what many providers actually do. Which leads me to the
 * second great thing about "managed entities"!<p>
 * 
 * Even though the entity state might not have been fully fetched from the
 * database, application code can always traverse a managed entity and the state
 * will seamlessly be fetched by the provider working in the background.
 * Awesome!<p>
 * 
 * Freely mutating state and be safe knowing it will actually be saved to the
 * database, is only a benefit that managed entities has. Of course, you may
 * mutate the state of a detached entity (one that is not managed), but the
 * changes will not be saved. A detached entity is most likely no more than a
 * stupid POJO (Plain Old Java Object) with no extra magic attached.<p>
 * 
 * Traversing a not fully loaded state is always safe to do with managed
 * entities. It is most likely not that safe to traverse the state of a detached
 * entity. The provider is not forbidden by the specification to honor such
 * traversals, but the providers seldom do.<p>
 * 
 * 
 * 
 * <h2>Persistence Context and the Entity Manager</h2>
 * 
 * Section "7.1 Persistence Contexts":
 * <pre>{@code
 * 
 *     A persistence context is a set of managed entity instances in which for
 *     any persistent entity identity there is a unique entity instance. Within
 *     the persistence context, the entity instances and their lifecycle are
 *     managed by the entity manager.
 * 
 * }</pre>
 * 
 * The persistence context is "a set of managed entity instances". Awesome. The
 * {@code EntityManager} is the application code's interface for access to the
 * persistence context and the interface has also methods for creating
 * queries.<p>
 * 
 * Entity manager and persistence context are two different things.<p>
 * 
 * But, from a logical viewpoint, the entity manager can be viewed as <i>the</i>
 * persistence context. Entity managers and their persistence context are so
 * tightly coupled that Java EE code often inject an entity manager using the
 * annotation "{@code @PersistenceContext}".<p>
 * 

 * 
 * To illustrate the tight coupling, here's the JavaDoc of {@code
 * EntityManager.contains(Object)}:
 * 
 * <pre>{@code
 * 
 *     Check if the instance is a managed entity instance belonging to the
 *     current persistence context.
 * 
 * }</pre>
 * 
 * The tight coupling will inevitably lead to a language used where the
 * terminology "persistence context" and "entity manager" are interchangeable.
 * This will happen in this package and its related documentation, as well as
 * elsewhere in Java EE literature.<p>
 * 
 * Finally, do note that "it is undefined whether a new entity manager instance is
 * created for every persistence context, or whether entity manager instances
 * are sometimes reused" (section 7.8.2).<p>
 * 
 * 
 * 
 * <h2>Container-managed versus application-managed entity managers</h2>
 * 
 * Granted, the Java EE API is a bit of a mess. The continuous aspiration of
 * degrading development in favor of "backward compatibility" is the major
 * reason and of course, the dollars behind. Entity managers and persistence
 * contexts are no exception to this rule. As such, it is not uncommon to hear
 * street language speaking of a multitude of different "types" of entity
 * managers. But, there are no different types of entity managers.<p>
 * 
 * Just like {@code @Singleton} threading<sup>2</sup> and JTA transaction
 * demarcation<sup>3</sup>, the entity manager too can opt for <i>management</i>
 * by the container or by the application.<p>
 * 
 * Okay, "managed" is a bit of a saturated word in our line of business. In JPA,
 * what is "managed" is the life cycle of the entity manager. The life cycle may
 * either be controlled by the container or by application code. The entity
 * manager is therefore said to be either a "container-managed entity manager"
 * or an "application-managed entity manager".<p>
 * 
 * 
 * 
 * <h2>Persistence context propagation and transaction type</h2>
 * 
 * Most likely, there is a persistence context associated with the entity
 * manager. As the thread continuous his execution path into other components,
 * the context may or may not be propagated to the entity managers used in these
 * other components. Put in another way; one entity manager may inherit the
 * persistence context of another. When this propagation occur, it will do so
 * whether the "target" entity manager was injected using
 * {@code @PersistenceContext} or looked up programmatically using JNDI.<p>
 * 
 * <strong>Propagation happens only for container-managed entity
 * managers</strong>. The persistence context in application-managed entity
 * managers is "stand-alone" and does not propagate anywhere, nor do an
 * application-managed entity manager inherit a persistence context from someone
 * else. For application-managed entity managers, it is application code that
 * bear the responsibility of passing around the entity manager
 * reference<sup>4</sup>.<p>
 * 
 * <strong>Container-managed entity managers can only use JTA
 * transactions</strong> (JTA transactions will be briefly described a bit later
 * in this document). <strong>Application-managed entity managers can use JTA,
 * but has also the <i>option</i> to use resource-local transactions</strong>.
 * Resource-local transactions involve only one entity manager and one resource
 * (i.e, a database). The most common transaction type used is JTA
 * transactions.<p>
 * 
 * 
 * 
 * <h2>Scope of the persistence context</h2>
 * 
 * The container-managed life cycle of the entity manager, or rather the
 * persistence context, is in most use-cases bound to the active JTA
 * transaction. Application-managed entity managers are, application-managed.
 * Their life cycle can not be bound to the JTA transaction. If that was
 * possible, then it would be the same as asking the container to manage an
 * application-managed entity manager. Clearly, such a configuration wouldn't
 * make much sense.<p>
 * 
 * If a container-managed entity manager is bound to the JTA transaction, then
 * the context will be destroyed when the transaction commit. All managed
 * entities in the persistence context will become detached. Thankfully, all
 * entity changes are flushed/saved to the database before that happen.<p>
 * 
 * Alternatively, the persistence context may be <i>extended</i>. An extended
 * persistence context "survive" the JTA transaction meaning that the entities
 * in it will not become detached when the transaction commit. The application
 * can even persist entities in this type of persistence context without an
 * active transaction which is not possible in a JTA-bound persistence
 * context.<p>
 * 
 * Application-managed entity managers can only be extended. Container-managed
 * entity managers has the option of choosing scope; JTA-bound or extended. If a
 * container-managed entity manager want to use an extended scope, then that
 * entity manager must only be used within a {@code @Stateful} EJB bean. The
 * "extended scope" will become bound to the stateful's life cycle instead of
 * a JTA transaction. JTA transactions may freely come and go during the life
 * time of an extended persistence context.<p>
 * 
 * <u>Summarization of this topic</u>: container-managed entity managers may be
 * bound to the active JTA transaction or be extended. Application-managed
 * entity managers can not be bound to a JTA transaction, their persistence
 * contest is extended.<p>
 * 
 * 
 * 
 * <h2>Synchronization type and flush mode</h2>
 * 
 * As if all the bloated talk this far wasn't enough, the persistence context
 * can further be synchronized or unsynchronized and use two different flush
 * modes. Yaaay =)<p>
 * 
 * Note that context synchronization with a transaction is also called
 * <i>joining</> the transaction.<p>
 * 
 * Synchronization type and flush mode are for niche applications only and not
 * further described in this document. If you want to know more, head over to
 * the test files
 * {@linkplain com.martinandersson.javaee.jpa.entitymanagers.misc.QueryingForEntitiesTest QueryingForEntitiesTest}
 * and
 * {@linkplain com.martinandersson.javaee.jpa.entitymanagers.misc.SynchronizationTest SynchronizationTest}.
 * Suffice it is to say for now that by default, all entity managers that use
 * JTA transactions are synchronized (meaning they won't cause you headache) and
 * the flush mode of all entity managers is auto (also meaning your life will be
 * better).<p>
 * 
 * 
 * 
 * <h2>JTA Transactions</h2>
 * 
 * JTA transactions are said to be "distributed" but that doesn't necessarily
 * mean they are distributed across different machines and networks. JTA
 * transactions are distributed in that multiple resources has registered their
 * interest to be part of the transaction. For example a JMS queue and a JDBC
 * compliant database driver. In this example, the resources might, and often
 * do for home-built Java EE applications, reside on the same machine.<p>
 * 
 * JTA transactions use the XA protocol, also known as "two-phase commit". Hence
 * using JTA requires a XA compliant database driver to be setup in the data
 * source definition (for a configuration example, see
 * {@linkplain com.martinandersson.javaee.resources.ArquillianDS ArquillianDS}).
 * XA is quite expensive but modern Java EE servers may be able to optimize away
 * the use of XA for a single resource (google "Logging Last Resource" and "Last
 * Agent Optimization"). This kind of optimization is transparent for the
 * application programmer. But the application programmer must specify a XA
 * compliant database driver when using entity managers that in turn use JTA
 * transactions.<p>
 * 
 * Remember that container-managed entity managers which is the most common
 * "type" of entity manager to use, must use JTA transactions.
 * Application-managed entity managers can use JTA transactions, but they also
 * have the option to use resource-local transactions. Resource-local
 * transactions are not distributed. They involve only one single resource, the
 * database in this case.<p>
 * 
 * Given modern optimizations, you should not choose to manage your own entity
 * manager only for the unproved belief of a performance gain of
 * resource-local over JTA. If you have the need for performance, then the
 * single most important performance boost you can monetize is to not use
 * transactions at all. All entity managers support issuing SELECT statements
 * despite not being in an active transaction. See
 * {@linkplain com.martinandersson.javaee.jpa.entitymanagers.misc.QueryingForEntitiesTest QueryingForEntitiesTest}.<p>
 * 
 * 
 * 
 * <h2>On transaction failure</h2>
 * 
 * Section "3.3.3 Transaction Rollback":
 * 
 * <pre>{@code
 * 
 *     For both transaction-scoped persistence contexts and for extended
 *     persistence contexts that are joined to the current transaction,
 *     transaction rollback causes all pre-existing managed instances and
 *     removed instances to become detached. The instances' state will be the
 *     state of the instances at the point at which the transaction was rolled
 *     back. Transaction rollback typically causes the persistence context to be
 *     in an inconsistent state at the point of rollback. In particular, the
 *     state of version attributes and generated state (e.g., generated primary
 *     keys) may be inconsistent. Instances that were formerly managed by the
 *     persistence context (including new instances that were made persistent in
 *     that transaction) may therefore not be reusable in the same manner as
 *     other detached objects—for example, they may fail when passed to the
 *     merge operation.
 * 
 *     NOTE: Because a transaction-scoped persistence context’s lifetime is
 *     scoped to a transaction regardless of whether it is joined to that
 *     transaction, the container closes the persistence context upon
 *     transaction rollback. However, an extended persistence context that is
 *     not joined to a transaction is unaffected by transaction rollback.
 * 
 * }</pre>
 * 
 * 
 * 
 * <h2>Entity Manager Factory</h2>
 * 
 * The entity manager factory may be injected using {@code @PersistenceUnit} or
 * using JNDI. The factory represents a JPA entity unit configuration as defined
 * in the <tt>persistence.xml</tt> file. Container-managed entity managers that
 * most applications use never use this annotation and the entity manager
 * factory. Applications that want to use application-managed entity managers
 * must use the factory method {@code
 * EntityManagerFactory.createEntityManager()} to actually get hold of an
 * application-managed entity manager.<p>
 * 
 * Code that already have a reference to an entity manager may get the entity
 * manager's factory using {@code EntityManager.getEntityManagerFactory()}.<p>
 * 
 * Section "7.2 Obtaining an EntityManager":
 * <pre>{@code
 * 
 *     When container-managed entity managers are used (in Java EE
 *     environments), the application does not interact with the entity manager
 *     factory. The entity managers are obtained directly through dependency
 *     injection or from JNDI, and the container manages interaction with the
 *     entity manager factory transparently to the application. When
 *     application-managed entity managers are used, the application must use
 *     the entity manager factory to manage the entity manager and persistence
 *     context lifecycle.
 * 
 * }</pre>
 * 
 * Noteworthy is section "7.6.4 Persistence Context Propagation":
 * <pre>{@code
 * 
 *     [..] a single persistence context may correspond to one or more JTA
 *     entity manager instances (all associated with the same entity manager
 *     factory). [..] Entity manager instances obtained from different entity
 *     manager factories never share the same persistence context.
 * 
 * }</pre>
 * 
 * 
 * 
 * <h2>Life cycle of an entity manager factory</h2>
 * 
 * Section "7.3 Obtaining an Entity Manager Factory" says (this quote begin with
 * the footnote in the section):
 * <pre>{@code
 * 
 *     There is only one entity manager factory per persistence unit[.]
 *     
 *     [..]
 * 
 *     More than one entity manager factory instance may be available
 *     simultaneously in the JVM.
 * 
 * }</pre>
 * 
 * There may only be more than one factory available if there is more than one
 * persistence unit defined in the <tt>persistence.xml</tt> file.<p>
 * 
 * Section "7.4 EntityManagerFactory Interface":
 * <pre>{@code
 * 
 *     The EntityManagerFactory interface is used by the application to obtain an
 *     application-managed entity manager. When the application has finished
 *     using the entity manager factory, and/or at application shutdown, the
 *     application should close the entity manager factory. Once an entity
 *     manager factory has been closed, all its entity managers are considered to
 *     be in the closed state.
 * 
 * }</pre>
 * 
 * The specification doesn't say when the server open or close an entity manager
 * factory. Only when speaking of third party factories do section
 * "7.8.1 Application-managed Persistence Contexts" say that:
 * <pre>{@code
 * 
 *     [..] the container is required to support third-party persistence
 *     providers, and in this case the container must use the
 *     PersistenceProvider.createContainerEntityManagerFactory method to create
 *     the entity manager factory and the EntityManagerFactory.close method to
 *     destroy the entity manager factory prior to shutdown (if it has not been
 *     previously closed by the application).
 * 
 * }</pre>
 * 
 * As a conclusion, application code must never bother about opening a factory.
 * That is handled by the provider. Likewise, application code <i>should</i>
 * never bother to close the entity manager factory either.<p>
 * 
 * 
 * 
 * <h2>Threading model</h2>
 * 
 * Section "7.2 Obtaining an EntityManager":
 * <pre>{@code
 * 
 *     An entity manager must not be shared among multiple concurrently
 *     executing threads, as the entity manager and persistence context are not
 *     required to be threadsafe. Entity managers must only be accessed in a
 *     single-threaded manner.
 * 
 * }</pre>
 * 
 * Using container-managed entity managers in EJB:s that serialize their calls
 * (except a non-default configured {@code @Singleton}) translates to a
 * trouble free application. This is the most used setup so - no worries.<p>
 * 
 * If you ever need to use the entity manager factory, then you're safe as well.
 * Section 7.3 Obtaining an Entity Manager Factory:
 * <pre>{@code
 * 
 *     Methods of the EntityManagerFactory interface are threadsafe.
 * 
 * }</pre>
 * 
 * 
 * 
 * <h4>Note 1</h4>
 * 
 * Even managed entities may loose changes if they were modified outside a
 * transaction and the persistence context that they belong to never joined a
 * transaction after the modification. See
 * {@linkplain com.martinandersson.javaee.jpa.entitymanagers.containermanaged.ExtendedTest#extendedPersistenceContextWithoutTransactions_changesLost()}
 * and
 * {@linkplain com.martinandersson.javaee.jpa.entitymanagers.applicationmanaged.JTATest#noTransactionNoFlush_changesLost()}.
 * 
 * 
 * <h4>Note 2</h4>
 * 
 * {@linkplain javax.ejb.ConcurrencyManagement @ConcurrencyManagement} may either be
 * {@linkplain javax.ejb.ConcurrencyManagementType#BEAN ConcurrencyManagementType.BEAN}
 * or
 * {@linkplain javax.ejb.ConcurrencyManagementType#CONTAINER ConcurrencyManagementType.CONTAINER}.
 * 
 * 
 * <h4>Note 3</h4>
 * 
 * For CDI beans, see {@linkplain javax.transaction.Transactional @Transactional}
 * and {@linkplain javax.transaction.Transactional.TxType Transactional.TxType}.
 * For EJB beans, see
 * {@linkplain javax.ejb.TransactionManagement @TransactionManagement} and
 * {@linkplain javax.ejb.TransactionManagementType TransactionManagementType}.
 * 
 * 
 * <h4>Note 4</h4>
 * 
 * Annotation {@code @PersistenceContext} can only inject container-managed
 * entity managers. Applications that want to use an application-managed entity
 * manager use {@code EntityManagerFactory.createEntityManager()} which always
 * return a new entity manager.
 */
package com.martinandersson.javaee.jpa.entitymanagers;