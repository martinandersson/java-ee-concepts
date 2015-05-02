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
 * </ol><p>
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
 * On the other hand, some state is "eagerly" fetched/loaded. Eager data loading
 * is a requirement for all {@code @Basic} fields (the default for a "basic"
 * field if it is left unannotated) and fields that represent "single-valued"
 * associations with other entities ({@code @OneToOne}, {@code @ManyToOne}).<p>
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
 * 
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
 * terminology "persistence context" and "entity manager" are used pseudo
 * randomly. Such is the case for all "hello world" samples the Internet
 * provide. Unfortunately, sometimes it is also the case for Java EE
 * specifications. This blur translates to a tuff challenge when trying to
 * understand the difference and exact nature of entity managers and persistence
 * contexts. In this file and all related ones, I'll try my best to separate the
 * two as far as possible and as far as my understanding serves me right.<p>
 * 
 * What we do know for sure and what probably is the most important point, is
 * that a "persistence context" is something the entity manager has a reference
 * to. The persistence context is "an invisible cloud" where managed entities
 * live.<p>
 * 
 * 
 * 
 * <h2>Container-managed versus application-managed entity managers</h2>
 * 
 * Street language often speak of a multitude of different "types" of entity
 * managers. But, there are no different types of entity managers. However, we
 * do say there exists different types of a persistence context. We will return
 * to persistence context types a bit later.<p>
 * 
 * Just like {@code @Singleton} threading<sup>2</sup> and JTA transaction
 * demarcation<sup>3</sup>, the entity manager too can opt for <i>management</i>
 * by the container or by the application. If I manage my own monetary funds or
 * if I let an incompetent fund manager do that for me, is not the same as
 * having different "types" of money. The money remains the same.<p>
 * 
 * Okay, "managed" is a bit of a saturated word in our line of business. In JPA,
 * what is "managed" is the life cycle of the entity manager, life cycle of the
 * persistence context involved and how a persistence context propagate to a
 * container-managed entity manager.<p>
 * 
 * Section "7.1 Persistence Contexts":
 * 
 * <pre>{@code
 * 
 *     In Java EE environments, a JTA transaction typically involves calls
 *     across multiple components. Such components may often need to access the
 *     same persistence context within a single transaction. To facilitate such
 *     use of entity managers in Java EE environments, when an entity manager is
 *     injected into a component or looked up directly in the JNDI naming
 *     context, its persistence context will automatically be propagated with
 *     the current JTA transaction, and the EntityManager references that are
 *     mapped to the same persistence unit will provide access to this same
 *     persistence context within the JTA transaction. This propagation of
 *     persistence contexts by the Java EE container avoids the need for the
 *     application to pass references to EntityManager instances from one
 *     component to another. An entity manager for which the container manages
 *     the persistence context in this manner is termed a container-managed
 *     entity manager. A container-managed entity manager's lifecycle is managed
 *     by the Java EE container.
 * 
 * }</pre>
 * 
 * The persistence context of container-managed entity managers propagate to
 * other application components and their container-managed entity managers
 * together with the active JTA transaction (independent of whether it was the
 * container or the bean that started the transaction)<sup>4</sup>. But..
 * 
 * Section "7.8.2 Container Managed Persistence Contexts":
 * 
 * <pre>{@code
 * 
 *     Exactly how the container maintains the association between persistence
 *     context and JTA transaction is not defined.
 * 
 * }</pre>
 * 
 * The life cycles may either be controlled by the container or by application
 * code. The entity manager is therefore said to be either a "container-managed
 * entity manager" or an "application-managed entity manager".<p>
 * 
 * For container-managed entity managers, then the container (= some server-side
 * box you shouldn't need to care about) is the one that open/close the entity
 * manager. Client code inject a container-managed entity manager using
 * {@code @PersistenceContext}.<p>
 * 
 * For application-managed entity managers, the client code is responsible for
 * creating a <i>new</i> and opened entity manager using {@code
 * EntityManagerFactory.createEntityManager()}. These entity managers must be
 * closed by the application using {@code EntityManager.close()}.<p>
 * 
 * Application-managed entity managers has exactly one persistence context and
 * that persistence context is not shared with other entity managers regardless
 * of their type. The life cycle of a persistence context used by an
 * application-managed entity manager is exactly the same as the life cycle of
 * the entity manager. The life of the persistence context begin when client
 * code call {@code EntityManagerFactory.createEntityManager()} and the
 * persistence context ends when client code call {@code EntityManager.close()}.
 * Hence, application-managed entity managers can be viewed as <i>the</i>
 * persistence context in use. Please read all about the details in
 * {@linkplain com.martinandersson.javaee.jpa.entitymanagers.applicationmanaged.JTATest JTATest}.<p>
 * 
 * Too bad for us, the same is not true for container-managed entity
 * managers.<p>
 * 
 * 
 * 
 * <h3>The persistence context used by container-managed entity managers</h3>
 * 
 * We shall look into persistence context propagation and its "scope" (i.e. life
 * time) a bit closer in the following sections. For now, let's try to
 * understand the relationship between container-managed entity managers and the
 * persistence context that they use. The following discussion applies
 * <strong>only</strong> to container-managed entity managers.<p>
 * 
 * A persistence context may be associated with one or shared by many entity
 * managers. The entity manager may be reused (pooled).<p>
 * 
 * Section "7.6.4 Persistence Context Propagation":
 * 
 * <pre>{@code
 * 
 *     [..] a single persistence context may correspond to one or more JTA
 *     entity manager instances (all associated with the same entity manager
 *     factory).
 * 
 * }</pre>
 * 
 * Section "7.8.2 Container Managed Persistence Contexts":
 * 
 * <pre>{@code
 * 
 *     [..] it is undefined whether a new entity manager instance is created for
 *     every persistence context, or whether entity manager instances are
 *     sometimes reused.
 * 
 * }</pre>
 * 
 * Section "7.9.1 Container Responsibilities" (with note 86):
 * 
 * <pre>{@code
 * 
 *     The container creates a new entity manager by calling
 *     EntityManagerFactory.createEntityManager when the first invocation of an
 *     entity manager with PersistenceContextType.TRANSACTION occurs within the
 *     scope of a business method executing in the JTA transaction.
 * 
 *     After the JTA transaction has completed (either by transaction commit or
 *     rollback), the container closes the entity manager by calling
 *     EntityManager.close.
 * 
 *     The container may choose to pool EntityManagers: it instead of creating
 *     and closing in each case, it may acquire one from its pool and call
 *     clear() on it.
 * 
 * }</pre>
 * 
 * Moral of the story is that the actual life cycle of the instance that is our
 * API, the "entity manager", is not defined. Nor should we care. What we should
 * <u>not</u> do is to invoke {@code EntityManager.close()} on a
 * container-managed instance. It might be good to know that if you do that on a
 * container-managed entity manager, then he will throw an {@code
 * IllegalStateException}.<p>
 * 
 * What about the life cycle of the persistence context?<p>
 * 
 * Section "3.3. Persistence Context Lifetime and Synchronization Type":
 * 
 * <pre>{@code
 * 
 *     The lifetime of a container-managed persistence context can either be
 *     scoped to a transaction (transaction-scoped persistence context), or have
 *     a lifetime scope that extends beyond that of a single transaction
 *     (extended persistence context).
 * 
 *     [..]
 * 
 *     By default, the lifetime of the persistence context of a
 *     container-managed entity manager corresponds to the scope of a
 *     transaction [..].
 * 
 *     When an extended persistence context is used, the extended persistence
 *     context exists from the time the EntityManager instance is created until
 *     it is closed. This persistence context might span multiple transactions
 *     and non-transactional invocations of the EntityManager.
 * 
 * }</pre>
 * 
 * 
 * Section "7.6. Container-managed Persistence Contexts":
 * 
 * <pre>{@code
 * 
 *     When a container-managed entity manager is used, the lifecycle of the
 *     persistence context is always managed automatically, transparently to the
 *     application [..].
 * 
 * }</pre>
 * 
 * So it is pritty clear that the life cycle of the default transaction-scoped
 * persistence context is the same as the transaction. We shall return to the
 * "extended" persistence context in a subsequent section.<p>
 * 
 * 
 * 
 * <h3>Obtaining a container-managed entity manager</h3>
 * 
 * As will be evident when we talk about the entity manager factory, the entity
 * manager and whatever persistence context he use, both of them are not thread
 * safe and should not be invoked concurrently.<p>
 * 
 * One must assume that if the provider's persistence context is not thread
 * safe, then it is either 1) not shared by many entity managers or 2) access to
 * the persistence context is synchronized.<p>
 * 
 * The JPA specification does not restrict where an entity manager is used. From
 * the application developer's perspective, he <i>should</i> not have to
 * care.<p>
 * 
 * Section "7.2.1. Obtaining an Entity Manager in the Java EE Environment":
 * <pre>{@code
 * 
 *     A container-managed entity manager is obtained by the application through
 *     dependency injection or through direct lookup of the entity manager in
 *     the JNDI namespace. The container manages the persistence context
 *     lifecycle and the creation and the closing of the entity manager instance
 *     transparently to the application.
 * }</pre>
 * 
 * The entity manager has historically been something used on the inside of
 * enterprise Java beans. These are thread safe unless a {@code @Singleton} has
 * been configured differently. The EJB specification allow all EJB:s to use a
 * container-managed entity manager<sup>5</sup>. The application developer can
 * rest assured that whenever an entity manager is used by an EJB, he really
 * should not worry about a thing. Let the container manage the entity manager,
 * and you're all set.<p>
 * 
 * About the same reasoning can be applied to entity managers injected into
 * other thread safe things like a {@code @RequestScoped} CDI bean for
 * example.<p>
 * 
 * So what if we use CDI to lookup/produce the entity manager and slap a "CDI
 * scope" on him? If we do, then we risk exposing the entity manager reference
 * to many different threads. If we go one step further and try to close a
 * container-managed entity manager in a disposer method, we violate the
 * use contract and the entity manager will throw an {@code
 * IllegalStateException}. In most cases, there is no point in using CDI to
 * produce a container-managed entity manager. See
 * {@linkplain com.martinandersson.javaee.cdi.producers.entitymanager}.<p>
 * 
 * In general, wherever we mutate JPA entities in a transaction-scoped
 * persistence context, we need to have an active transaction going. Serious
 * applications must handle transaction failures. EJB:s provide automatic
 * transaction demarcation only (start and stop of a transaction). Same thing
 * can be provided by a {@code @Transactional} CDI bean since Java EE 7. The
 * only portable way an application has to figure out what the hell happened
 * when a transaction fails, is to control the demarcation as well. The one
 * reason left why you would prefer to only use entity managers inside
 * {@code @Stateless} EJB:s is for thread safety and object pooling.<p>
 * 
 * Thread-safety can be achieved by so many other means. To be a bit blunt, if
 * one don't know shit about concurrent programming, then this guy shouldn't be
 * writing code for back ends to begin with.<p>
 * 
 * As far as object pooling goes, it is most likely that the component that
 * deals with inbound requests to the Java EE application is throttled by means
 * of a thread pool or a similar construct. Also, it is "typically"<sup>6</sup>
 * the case that the database connections used is pooled as well. Overall, that
 * should translate to a limited use case for {@code @Stateless} beans in your
 * application. Why reduce the parallelism in your business layer if the layers
 * on top and below yours are already throttled?<p>
 * 
 * Therefore, use {@code @PersistenceContext} in whatever type of application
 * components you have designated to handle persistence. Make sure you have a
 * transaction whenever one is needed, and make sure the entity manager is not
 * accessed concurrently.<p>
 * 
 * 
 * 
 * <h2>Persistence context propagation and transaction type</h2>
 * 
 * <strong>Propagation happens only for container-managed entity
 * managers</strong>. The persistence context in application-managed entity
 * managers is "stand-alone" and does not propagate anywhere, nor do an
 * application-managed entity manager inherit a persistence context from someone
 * else. For application-managed entity managers, it is application code that
 * bear the responsibility of passing around the entity manager
 * reference<sup>7</sup>.<p>
 * 
 * Most likely, there is a persistence context associated with the entity
 * manager. As the thread continuous his execution path into other components,
 * the context may or may not be propagated to the entity managers used in these
 * other components. Put in another way; one entity manager may inherit the
 * persistence context of another. When this propagation occur, it will do so
 * independent of how the "target" entity manager was injected; using
 * {@code @PersistenceContext} or looked up programmatically with JNDI.<p>
 * 
 * <strong>Container-managed entity managers can only use JTA
 * transactions</strong> (JTA transactions will be briefly described a bit later
 * in this document). <strong>Application-managed entity managers can use JTA,
 * but has also the <i>option</i> to use resource-local transactions</strong>.
 * Resource-local transactions involve only one entity manager and one resource
 * (i.e. a database). The most common transaction type used is JTA
 * transactions.<p>
 * 
 * 
 * 
 * <h2>Scope of the persistence context</h2>
 * 
 * The container-managed life cycle of the persistence context, is in most
 * use-cases bound to the active JTA transaction.<p>
 * 
 * As we learned a bit earlier in this document, application-managed entity
 * managers have exactly one persistence context not shared by any one else.
 * Hence, both of them are application-managed. Their life cycle can not be
 * bound to the JTA transaction. If that was possible, then it would be the same
 * as asking the container to manage an application-managed entity manager.
 * Clearly, such a configuration wouldn't make much sense.<p>
 * 
 * If the persistence context of a container-managed entity manager is bound to
 * the JTA transaction, then the persistence context will be destroyed when the
 * transaction commit. All managed entities in the persistence context will
 * become detached. Thankfully, all entity changes are flushed/saved to the
 * database before the entity become detached.<p>
 * 
 * Alternatively, the persistence context may be <i>extended</i>. An extended
 * persistence context "survive" the JTA transaction meaning that the entities
 * in it will not become detached when the transaction commit. The application
 * can even persist entities in this type of persistence context without an
 * active transaction which is not possible in a JTA-bound persistence
 * context.<p>
 * 
 * The persistence context used by application-managed entity managers can only
 * be extended. Container-managed entity managers has the option of choosing
 * scope; JTA-bound or extended. If a container-managed entity manager want to
 * use a persistence context with an extended scope, then that entity manager
 * must only be used within a {@code @Stateful} EJB bean. The "extended scope"
 * will become bound to the stateful's life cycle instead of a JTA transaction.
 * JTA transactions may freely come and go during the life time of an extended
 * persistence context. Read a bit more about extended persistence contexts in
 * {@linkplain com.martinandersson.javaee.jpa.entitymanagers.containermanaged.ExtendedTest}.<p>
 * 
 * <u>Summarization of this topic</u>: container-managed entity managers may be
 * bound to the active JTA transaction or be extended. Application-managed
 * entity managers can not be bound to a JTA transaction, their persistence
 * contest is always extended.<p>
 * 
 * 
 * 
 * <h2>Synchronization type and flush mode</h2>
 * 
 * As if all the bloated talk this far wasn't enough, the persistence context
 * can be synchronized or unsynchronized and use two different flush modes.
 * Yaaay =)<p>
 * 
 * Note that synchronizing a persistence context with a transaction is also
 * called <i>joining</> the transaction.<p>
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
 * interest with the transaction manager to be part of the same transaction. For
 * example a JMS queue and a JDBC compliant database driver. The resources
 * might, and often do for home-built Java EE applications, reside on the same
 * machine.<p>
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
 * database.<p>
 * 
 * Given modern optimizations, you should not choose to manage your own entity
 * manager only for the unproved belief of a performance gain that
 * resource-local transactions supposedly have over JTA. If you have the need
 * for performance, then the single most important performance boost you can
 * monetize is to not use transactions at all. All entity managers support
 * issuing SELECT statements despite not being in an active transaction. See
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
 *     other detached objects - for example, they may fail when passed to the
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
 * in the {@code persistence.xml} file. Applications mostly use
 * container-managed entity managers and has no use of the entity manager
 * factory.<p>
 * 
 * As we learned previously, applications that want to use application-managed
 * entity managers must use the factory method
 * {@code EntityManagerFactory.createEntityManager()} to actually get hold of an
 * application-managed entity manager.<p>
 * 
 * I said before that applications mostly use container managed entity managers
 * and "has no use of the entity manager factory". Well, actually there is one
 * use case. What if your bean used a container-managed entity manager that
 * crashed the active transaction but the bean must for whatever reason call the
 * database again with a query? The only plausible option you have is to create
 * a new application-managed entity manager and issue the query. Using the
 * container-managed entity manager again from the same bean will most likely
 * fail when the entity manager discover that the transaction is a big failure
 * and won't proceed.<p>
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
 *     Entity manager instances obtained from different entity manager factories
 *     never share the same persistence context.
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
 * persistence unit defined in the {@code persistence.xml} file.<p>
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
 * If you do use the entity manager factory, then you're safe as well. Section
 * 7.3 Obtaining an Entity Manager Factory:
 * 
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
 * There is an exception to this rule. A {@code @Stateful} bean that uses an
 * extended persistence context cannot be the target of such a propagation.
 * You'll find more test regarding this topic in
 * {@linkplain com.martinandersson.javaee.jpa.entitymanagers.containermanaged.ExtendedTest}.
 * But it is much advised that you read through this entire page before going
 * elsewhere.
 * 
 * 
 * <h4>Note 5</h4>
 * 
 * See EJB 3.2 specification, sections 4.6.2, 4.7.2, 4.8.6, 5.5.1 and 11.11.1.1.
 * 
 * 
 * <h4>Note 6</h4>
 * 
 * See JDBC specification 4.1, section 11.1.
 * 
 * 
 * <h4>Note 7</h4>
 * 
 * Annotation {@code @PersistenceContext} can only inject container-managed
 * entity managers. Applications that want to use an application-managed entity
 * manager use {@code EntityManagerFactory.createEntityManager()} which always
 * return a new entity manager.
 */
package com.martinandersson.javaee.jpa.entitymanagers;