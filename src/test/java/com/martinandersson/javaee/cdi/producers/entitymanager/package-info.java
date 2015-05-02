/**
 * This package take a close look at how {@code @Produces} can be used to inject
 * an {@code EntityManager}, as well as how it <u>should not</u> be used to
 * inject an entity manager.<p>
 * 
 * Internet is littered with "hello world" samples and this package is not one
 * of them. If you're new to producer methods and/or fields, please google it a
 * bit first.<p>
 * 
 * Most Internet samples will use a {@code @Produces} annotation in combination
 * with a {@code @PersistenceContext} annotation without thinking twice about
 * it. I get a feeling that some people go this way just because it is 1) status
 * to show off a [shallow] understanding of invisible technology. Also, which we
 * both surely agree with, 2) {@code @Inject} sound way cooler than
 * {@code @PersistenceContext} (and it saves a few keystrokes).<p>
 * 
 * But, anyone who want to design a killer app can't afford to be so sloppy that
 * one prefer to use CDI without knowing the full implication.<p>
 * 
 * 
 * 
 * <h2>When is CDI useful for looking up an entity manager?</h2>
 * 
 * CDI is useful to look up application-managed entity managers. This pattern
 * abstract away the entity manager factory. For example:
 * <pre>
 * 
 * &#064;RequestScoped
 * public class EntityManagerProducer {
 *     &#064;PersistenceUnit
 *     EntityManagerFactory factory;
 *     
 *     &#064;Produces
 *     &#064;RequestScoped
 *     public EntityManager newEntityManager() {
 *         return factory.createEntityManager();
 *     }
 *     
 *     public void closeEntityManager(@Disposes EntityManager em) {
 *         em.close();
 *     }
 * }
 * 
 * public class EntityManagerConsumer {
 *     &#064;Inject
 *     EntityManager em;
 *     
 *     // ...
 * }
 * </pre>
 * 
 * The example above produce a {@code @RequestScoped} application-managed entity
 * manager. The persistence context used by the entity manager is "extended" and
 * survive many JTA transactions that come and go during the entire request
 * [scope]. The persistence context is automatically closed by CDI when the
 * request scope ends.<p>
 * 
 * The producer itself (the {@code EntityManagerProducer} class) is also
 * {@code @RequestScoped}. Most Internet samples you will find will use a
 * {@code @Dependent} producer class. If a disposer method is combined with a
 * producer field- or method like in the example above, then putting a dependent
 * scope on a producer class has no point as it will force the container to
 * create a new instance of the producer when time comes to dispose (close) the
 * entity manager. See
 * {@linkplain com.martinandersson.javaee.cdi.producers.entitymanager.producer.ProducerClassTest}.<p>
 * 
 * CDI can also be used to provide isolated access to many different persistence
 * units and increase the type-safety. Instead of having client code depend on
 * many and different strings, collect all strings in one class and map them to
 * custom qualifiers instead:
 * <pre>
 * 
 * &#064;ApplicationScoped
 * public class EntityManagerFactories {
 *     &#064;PersistenceUnit(unitName = "PU1")
 *     &#064;MyPU1Qualifier
 *     EntityManagerFactory factory1;
 *     
 *     &#064;PersistenceUnit(unitName = "PU2")
 *     &#064;MyPU2Qualifier
 *     EntityManagerFactory factory2;
 *     
 *     // ...
 * }
 * 
 * &#064;Stateful
 * public class EntityManagerConsumer {
 *     private EntityManager em;
 *     
 *     &#064;Inject
 *     private void initialize(&#064;MyPU1Qualifier EntityManagerFactory factory) {
 *         em = factory.createEntityManager();
 *     }
 *     
 *     &#064;Remove
 *     public void finish() {
 *         em.close();
 *     }
 * }
 * </pre>
 * 
 * In this example, we used a CDI initializer method to get a reference to the
 * factory. This method is marked private as there is no point in exposing the
 * initializer method in the business interface of the bean.<p>
 * 
 * As an alternative to the initializer method, we could have used a CDI bean
 * constructor. If we use a CDI bean constructor with at least one parameter,
 * then we must explicitly provide a public no-arg constructor as having a
 * public no-arg constructor in the EJB class is a requirement of the EJB
 * specification. If we have two constructors, then it is not apparent anymore
 * how many instances of the class will be created. Yes, CDI and EJB is not
 * closely aligned with each other (IIRC, the alignment will be improved in Java
 * EE 8 although I don't know if this particular caveat will be resolved).<p>
 * 
 * Of course, even without CDI, we are not required to bleed strings all over
 * the system. Arguably, this example has less clutter and is just as
 * "type-safe" as the preceding one:
 * <pre>
 * 
 * public final class EntityManagerFactories {
 *     private EntityManagerFactories() {}
 *     
 *     public static final String PU1 = "PU1",
 *                                PU2 = "PU2";
 *     
 *     // ...
 * }
 * 
 * &#064;Stateful
 * public class EntityManagerConsumer {
 *     &#064;PersistenceContext(unitName=EntityManagerFactories.PU1, type = PersistenceContextType.EXTENDED)
 *     EntityManager em;
 *     
 *     &#064;Remove
 *     public void finish() {}
 * }
 * </pre>
 * 
 * So far, we have covered one very useful practice of using CDI to lookup an
 * application-managed entity manager. We have also covered a corner case where
 * CDI can be used to map between strings and qualifiers, at the expense of
 * "type explosion" and increased complexity of the application. Are there any
 * downsides of using CDI to lookup a container-managed entity manager? Yes.
 * Keep on reading!<p>
 * 
 * 
 * 
 * <h2>A closer look at entity managers with a CDI scope</h2>
 * 
 * A producer field "may also specify scope"<sup>1</sup>. This producer field
 * has no explicit scope declared and uses the default {@code @Dependent}:
 * <pre>
 * 
 * public class EntityManagerProducer {
 *     &#064;Produces
 *     &#064;PersistenceContext
 *     EntityManager em;
 * }
 * </pre>
 * 
 * In the provided example, the producer class is dependent, and so is the CDI
 * proxy that client code get when using {@code @Inject} to lookup the entity
 * manager. The CDI specification does not require the container to wrap the
 * entity manager reference in a proxy, but at least Weld does so.<p>
 * 
 * Had the producer class been declared to have a normal scope wider than
 * request scope, we'd run into the risk of exposing the entity manager to many
 * different threads as the producer field itself is not reinitialized. This is
 * a topic we shall return to shortly.<p>
 * 
 * The only thing the preceding example does is to add complexity and reduce
 * performance (one extra proxy layer and everything else that goes with it).
 * Instead of using the producer field, the consumer should have used
 * {@code @PersistenceContext} directly.<p>
 * 
 * With a scope declared..
 * <pre>
 * 
 * public class EntityManagerProducer {
 *     &#064;Produces
 *     &#064;RequestScoped
 *     &#064;PersistenceContext
 *     EntityManager em;
 * }
 * </pre>
 * 
 * ..there is a high probability that your application crash with something like
 * this:
 * <pre>
 * 
 * org.jboss.weld.exceptions.DefinitionException: WELD-001502:
 *     Resource producer field [Resource Producer Field [EntityManager] with
 *     qualifiers [&#064;Any &#064;Default] declared as [[BackedAnnotatedField]
 *     &#064;Produces &#064;RequestScoped &#064;PersistenceContext
 *     com.somepackage.em]] must be &#064;Dependent scoped
 * </pre>
 * 
 * A "CDI resource"<sup>2</sup> is a producer field that allow injection of Java
 * EE resources like the previous example. A CDI resource is not required to
 * support any other scope than {@code @Dependent}.<p>
 * 
 * CDI 1.2, section "3.7. Resources":
 * <pre>{@code
 * 
 *     The container is not required to support resources with scope other
 *     than @Dependent. Portable applications should not define resources with
 *     any scope other than @Dependent.
 * }</pre>
 * 
 * A producer field "is a slightly simpler alternative to a producer
 * method"<sup>3</sup>. To solve our problem, we must upgrade to a "real"
 * producer method:
 * <pre>
 * 
 * public class EntityManagerProducer {
 *     &#064;PersistenceContext
 *     EntityManager em;
 *     
 *     &#064;Produces
 *     &#064;RequestScoped
 *     public EntityManager getEntityManager() {
 *         return em;
 *     }
 * }
 * </pre>
 * 
 * Many Internet samples will use this night hack to get hold of a
 * container-managed entity manager with a normal CDI scope. Is that a good
 * idea?<p>
 * 
 * In the beginning of this JavaDoc, we used CDI to abstract away the entity
 * manager factory in order to put scope on an application-managed entity
 * manager. That is a plausible thing to do. Our producer produced the
 * application-managed entity manager, and our disposer method closed it. But
 * there really is no point in having CDI "manage" an already
 * <i>container-managed</i> entity manager.<p>
 * 
 * Note that CDI doesn't really manage the entity manager despite popular
 * slogans like "automatic context and life cycle management"<sup>4</sup>. That
 * is why we need a disposer method for the application-managed entity manager.
 * We are the ones managing the entity manager. CDI is an invisible object store
 * bound to a scope. CDI is unknowing about particular calls that need to be
 * done to open or close the stuff we put into that store.<p>
 * 
 * The {@code package-info.java} file in
 * {@linkplain com.martinandersson.javaee.jpa.entitymanagers} concludes that the
 * entity manager and his associated persistence context are two different
 * things. A single persistence context may correspond to one or more entity
 * managers. The life cycle of the container-managed entity manager is not
 * defined. In most use cases, the persistence context used by a
 * container-managed entity manager is scoped to the active transaction.<p>
 * 
 * Most importantly, application code does not open or close a container-managed
 * entity manager. Fact is that the application code is not even allowed to call
 * {@code EntityManager.close()} on a container-managed entity manager. An
 * unnamed container manages the life cycle of a container-managed entity
 * manager, as well as the life cycle of the persistence context and the
 * persistence context propagation from one container-managed entity manager to
 * the next.<p>
 * 
 * JPA 2.1, section "7.1 Persistence Contexts":
 * <pre>{@code
 * 
 *     This propagation of persistence contexts by the Java EE container avoids
 *     the need for the application to pass references to EntityManager
 *     instances from one component to another.
 * }</pre>
 * 
 * But, what we have just asked CDI to do for us in the previous example <u>is
 * to pass around the reference</u>.<p>
 * 
 * The one thing that JPA required by application code is to not use the entity
 * manager concurrently as the entity manager is not required to be thread-safe.
 * Obviously, putting a scope like {@code @SessionScoped} or
 * {@code @ApplicationScoped} on a container-managed entity manager will only
 * complicate that picture and might result in CDI exposing the entity manager
 * reference to many different threads.<p>
 * 
 * In reality, the entity manager is probably a thread-local object, i.e., it
 * is thread-safe. But, a portable application cannot rely on an implementation
 * detail.<p>
 * 
 * Basically, what the JPA specification says about container-managed entity
 * managers is the following: "Don't bother about putting a CDI scope on a
 * container-managed entity manager". But if we put the "normal" JPA programming
 * model aside for a minute, does using CDI to "produce" an entity manager
 * change anything related to how we interact or handle the entity manager?<p>
 * 
 * CDI section "7.3.6. Lifecycle of resources":
 * <pre>{@code
 * 
 *     For an entity manager associated with a resource definition, it must
 *     behave as though it were injected directly using @PersistencContext.
 * }</pre>
 * 
 * So, nothing changes. Instead, using CDI can put you at risk not using the
 * entity manager in a thread-safe manner.<p>
 * 
 * Despite not having a compelling reason to use CDI producers for
 * container-managed entity managers, many people do. And they even go one step
 * further by <i>closing</i> the entity manager in a disposer method.<p>
 * 
 * This is an example taken from the official Java EE 7 tutorial produced by
 * Oracle<sup>5</sup>:
 * <pre>
 * 
 * &#064;javax.inject.Singleton
 * public class UserDatabaseEntityManager {
 * 
 *     &#064;Produces
 *     &#064;PersistenceContext
 *     &#064;UserDatabase
 *     private EntityManager em;
 * 
 * 
 *     public void close(@Disposes @UserDatabase EntityManager em) {
 *         em.close();
 *     }
 * }
 * </pre>
 * 
 * The following example from the CDI 1.2 specification (section "3.5.2.
 * Declaring a disposer method") is similar. Although not exposing the entity
 * manager in a Singleton, they will try to close it:
 * <pre>
 * 
 * public class Resources {
 * 
 *     &#064;PersistenceContext
 *     &#064;Produces @UserDatabase
 *     private EntityManager em;
 * 
 *     public void close(@Disposes @UserDatabase EntityManager em) {
 *         em.close();
 *     }
 * }
 * </pre>
 * 
 * Not only does these examples have no benefit, they are wrong. The Oracle
 * example risk exposing the entity manager reference to many threads and both
 * examples try to close a container-managed entity manager. The only thing the
 * disposer method does in these examples is to throw an {@code
 * IllegalStateException} that is consumed by the container. The entity manager
 * itself remains open. See
 * {@linkplain com.martinandersson.javaee.cdi.producers.entitymanager.unsafe.OracleTest}.<p>
 * 
 * 
 * 
 * <h2>Final verdict</h2>
 * 
 * If you by any chance want to abstract away a couple of calls to the entity
 * manager factory (my personal preference), then go ahead and use CDI. If you
 * have many persistence units and prefer qualifiers with a minor type explosion
 * instead of just putting the strings somewhere as constants in a class, then
 * go ahead and use CDI. But for container-managed entity managers that all
 * share the same entity manager factory, CDI has no use. More so than anything
 * else, don't use a disposer method to close a container-managed entity
 * manager.<p>
 * 
 * 
 * 
 * <h4>Note 1</h4>
 * 
 * CDI 1.2, section "3.4.2. Declaring a producer field".
 * 
 * 
 * <h4>Note 2</h4>
 * 
 * You will not find the term "CDI resource" anywhere in the specification. That
 * is just my own made up terminology to differentiate between a normal Java EE
 * resource and one such resource we lookup using a CDI producer field.
 * 
 * 
 * <h4>Note 3</h4>
 * 
 * CDI 1.2, section "3.4. Producer fields".
 * 
 * 
 * <h4>Note 4</h4>
 * 
 * http://www.oracle.co.jp/jdt2015/pdf/2-4.pdf
 * 
 * 
 * <h4>Note 5</h4>
 * 
 * http://docs.oracle.com/javaee/7/tutorial/cdi-adv-examples003.htm#JEETT01139
 */
package com.martinandersson.javaee.cdi.producers.entitymanager;