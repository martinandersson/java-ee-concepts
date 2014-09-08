/**
 * <h2>DI related specifications</h2>
 * 
 * Many annotations have been duplicated across the JavaServer Faces
 * specification<sup>1</sup> and the Contexts and Dependency Injection for Java
 * specification<sup>2</sup>, also called the "CDI specification". See {@code
 * com.martinandersson.javaee.cdi.packaging.package-info.java}. In this file and
 * package, unless otherwise noted, all types referred to are defined in the CDI
 * specification or another related one: "Dependency Injection for Java 1.0",
 * JSR-330<sup>3</sup>.<p>
 * 
 * The latter specification is listed as a Java EE 7 technology<sup>4</sup>. So
 * what's the difference between the CDI specification and JSR-330? Well,
 * apparently not the names so much.<p>
 * 
 * CDI builds on JSR-330 which defines annotations used by dependency injection
 * (DI) providers and the specification also define an injector configuration
 * API. The most famous annotation that is defined in JSR-330 is
 * {@code @Inject}.<p>
 * 
 * JSR-330 is not a technology unique to Java EE servers. Many providers or
 * "DI services" which has nothing to do with Java EE offer DI implementations
 * built on top of JSR-330, Spring 3 and Google Guice for example. Deploying our
 * Java EE application to a Java EE compatible server guarantee our application
 * that a DI service will always be present and active. For WildFly and
 * GlassFish, the provider is the same: Weld<sup>5</sup>.<p>
 * 
 * 
 * 
 * <h2>Dependency Injection</h2>
 * 
 * If a class uses a reference of another class, or a resource that is defined
 * somewhere else, then the class has a <i>dependency</i>. In complex
 * applications, getting a reference to the dependency can be tricky and
 * cumbersome. Even if it is possible to acquire dependencies without a
 * dependency injection service, such an architecture is complex and hard to
 * understand. Dependency injection, also called "Inversion Of Control",
 * delegate the responsibility of looking up such dependencies to someone else;
 * the DI service or container if you will.<p>
 * 
 * A bean class that has access to a DI service need only use the annotation
 * {@linkplain javax.inject.Inject @javax.inject.Inject} in order to get hold of
 * the dependency. The annotation is mostly put on fields, but can also annotate
 * a constructor or a method; a so called "initializer method". If annotated on
 * a constructor or a method, the arguments passed in are injected by the
 * container just like {@code @Inject} annotated fields are.<p>
 * 
 * We shall return to the {@code @Inject} annotation, but it is important to
 * note that if the bean is created programmatically using the {@code new}
 * keyword, then the container has no way of doing bean injection. Such fields
 * will become uninitialized. Likewise, if the {@code @Inject} annotation is put
 * on a constructor or put on a method, then the arguments will be injected only
 * if the constructor or the method is implicitly invoked by the container.
 * Application code invoking the constructor or method programmatically do not
 * see injected arguments.<p>
 * 
 * One huge advantage of the DI service as provided in a Java EE server is that
 * the dependencies, or rather instances thereof, may be associated with a
 * <i>context</i> for propagation across different layers of the application.
 * Thus, instances may be reused and store state. Not only do the instances
 * propagate across different layers, their life-cycle is bound to a particular
 * <i>scope</i> which determine for how long the instances are going to live and
 * be able to be reused before the instance is destroyed<sup>6</sup>. Hence, the
 * Java EE specialized specification is multifaceted and called "Contexts and
 * Dependency Injection"<p>
 * 
 * The context-association, and perhaps even the construction of the dependency
 * is completely transparent for the application programmer. For example, a
 * logged in {@code User} that is {@code @SessionScoped} might be such a
 * dependency. All components within the application can share the logged in
 * user without having to pass around the reference or rely on a third
 * application component for retrieval of the reference. When the user logout
 * and the scope ends, the user is automatically destroyed. Don't worry yourself
 * if everything sound foreign to you, code examples will be built that fully
 * illustrate this concept, and we will shortly get back to what a scope is. But
 * first, let's take a look at the technology behind.<p>
 * 
 * 
 * 
 * <h2>Context</h2>
 * 
 * Before even having a chance of understanding what a scope is, one must
 * understand what a context is. Here's a mix of two copy-paste's from the CDI
 * specification chapter 6, see if it makes any sense to you:
 * <pre>{@code
 *     
 *     Associated with every scope type is a context object. The context object
 *     determines the lifecycle and visibility of instances of all beans with
 *     that scope. In particular, the context object defines:
 *     
 *         • When a new instance of any bean with that scope is created
 *         • When an existing instance of any bean with that scope is destroyed
 *         • Which injected references refer to any instance of a bean with that
 *           scope
 *     
 *     The context implementation collaborates with the container via the
 *     Context and Contextual interfaces to create and destroy contextual
 *     instances.
 *     
 *     ...
 *     
 *     From time to time, the container must obtain an active context object
 *     for a certain scope type. The container must search for an active
 *     instance of Context associated with the scope type.
 *     
 *         • If no active context object exists for the scope type, the
 *           container throws a ContextNotActiveException.
 *         • If more than one active context object exists for the given scope
 *           type, the container must throw an IllegalStateException.
 *     
 *     If there is exactly one active instance of Context associated with the
 *     scope type, we say that the scope is active.
 * 
 * }</pre>
 * 
 * So the context is basically only a "store" were objects are put and retrieved
 * from. Objects in the store are called "contextual instances". The bean class
 * is loosely referred to as a "contextual type". Being associated with a
 * context is what makes the provider able to propagate instances across
 * different layers of the application and also able to determine a finite and
 * scoped life cycle of managed instances. Each normal scope type has a
 * beginning and an end.<p>
 * 
 * It is important to note that the context is a mere engine to realize scope
 * technology. Contexts themselves are completely transparent for the
 * application programmer. The application programmer declare a scope type for
 * his bean and doesn't really care where or how the injected instance is
 * retained in memory.<p>
 * 
 * 
 * 
 * <h2>CDI threading</h2>
 * 
 * To better understand scopes and how a scope, or rather the scope-associated
 * context object propagate, we must define the CDI threading model.<p>
 * 
 * CDI 1.1, section 6.2 "The Context interface" says:
 * <pre>{@code
 * 
 *     At a particular point in the execution of the program a context object
 *     may be active with respect to the current thread.
 * 
 * }</pre>
 * 
 * 
 * CDI 1.1, section 6.3 "The Context interface" says:
 * <pre>{@code
 * 
 *     The context object for a normal scope type is a mapping from each
 *     contextual type with that scope to an instance of that contextual type.
 *     There may be no more than one mapped instance per contextual type per
 *     thread. The set of all mapped instances of contextual types with a
 *     certain scope for a certain thread is called the context for that scope
 *     associated with that thread.
 * 
 *     A context may be associated with one or more threads. A context with a
 *     certain scope is said to propagate from one point in the execution of the
 *     program to another when the set of mapped instances of contextual types
 *     with that scope is preserved.
 * 
 *     The context associated with the current thread is called the current
 *     context for the scope. The mapped instance of a contextual type
 *     associated with a current context is called the current instance of the
 *     contextual type.
 * 
 * }</pre>
 * 
 * The "current context" is a thread-local object. Usually, such things doesn't
 * propagate across different threads. But the specification clearly said that
 * the context is associated with "one or more threads". The Weld
 * documentation<sup>7</sup> puts it this way:
 * <pre>{@code
 * 
 *     For a given thread in a CDI application, there may be an active context
 *     associated with the scope of the bean. This context may be unique to the
 *     thread (for example, if the bean is request scoped), or it may be shared
 *     with certain other threads (for example, if the bean is session scoped)
 *     or even all other threads (if it is application scoped).
 * 
 * }</pre>
 * 
 * So, how can a thread-local object be associated with multiple threads and why
 * would the developer bother about it?<p>
 * 
 * Java has a built-in awesome datatype to use for storage of thread local
 * objects: {@linkplain java.lang.ThreadLocal}. But this type has no way to
 * propagate across different threads. {@linkplain
 * java.lang.InheritableThreadLocal}, is an alternative that may propagate from
 * a parent thread to a parent-spawned child thread. Propagating an object to a
 * completely foreign thread that has no relationship with the "caller" requires
 * a bit more sophisticated code but it is completely doable (for example, see
 * {@code BlockingQueue<E>}).<p>
 * 
 * The real problem is submitting pieces of logic to a thread pool for execution
 * in the future. For example, the worker thread might execute the logic so far
 * away into the future that the callee's context that was active during
 * invocation of the asynchronous method is no longer active when the worker
 * thread finally is scheduled for work.<p>
 * 
 * In order to keep things simple and leave the programmer with an intact, not
 * too hard to understand programming model, contexts and scopes are specified
 * not to propagate across "asynchronous processes". Here's the full quote on
 * that, CDI 1.1, section "6.7 Context management for built-in scopes":
 * <pre>{@code
 * 
 *     The context associated with a built-in normal scope propagates across
 *     local, synchronous Java method calls, including invocation of EJB local
 *     business methods. The context does not propagate across remote method
 *     invocations or to asynchronous processes such as JMS message listeners or
 *     EJB timer service timeouts.
 * 
 * }</pre>
 * 
 * Don't get this quote wrong. The scope, or rather the context object
 * associated with the scope, might not <i>propagate</i> but the scope may be
 * active. Therefore, a client might successfully {@code @Inject} a
 * {@code @RequestScoped} bean because the "context is active", but the instance
 * he receive is a new instance, not an old one because the previous context did
 * not propagate. See test
 * {@linkplain com.martinandersson.javaee.cdi.scope.request.RequestScopedTest#contextDoesNotPropagateAcrossAsynchronousEJB(java.net.URL)
 * RequestScopedTest.contextDoesNotPropagateAcrossAsynchronousEJB()}.<p>
 * 
 * 
 * 
 * <h2>Scopes</h2>
 * 
 * Four "normal scopes" are built-in and provided by the CDI specification.
 * These are well described in the specification and will therefore not receive
 * any more attention in this document.
 * 
 * <ol>
 *   <li>{@code @RequestScoped}<br />
 *        CDI 1.1, section "6.7.1 Request context lifecycle"</li>
 *   <li>{@code @SessionScoped}<br />
 *        CDI 1.1, section "6.7.2 Session context lifecycle"</li>
 *   <li>{@code @ApplicationScoped}<br />
 *        CDI 1.1, section "6.7.3 Application context lifecycle"</li>
 *   <li>{@code @ConversationScoped}<br />
 *        CDI 1.1, section "6.7.4 Conversation context lifecycle"</li>
 * </ol>
 * 
 * Apart from these scopes, the application can define his own. Note that
 * {@code @ConversationScoped} is a scope that may have any developer-defined
 * life cycle inbetween the start of a Serlvet request and the end of the user
 * session.<p>
 * 
 * There is one built-in scope that is not a normal scope. It is called a
 * "pseudo scope".<p>
 * 
 * 
 * 
 * <h2>The @Depedent pseudo-scope</h2>
 * 
 * The {@code @Dependent} scope is the default scope a CDI bean has if it does
 * not declare another scope. Indirectly, a "stereotype" ({@code @Stereotype})
 * may declare another default scope.<p>
 * 
 * <strong>TODO:</strong> Continue<p>
 * 
 * 
 * 
 * <h2>The Java EE singleton dilemma</h2>
 * 
 * <strong>TODO:</strong> Explain the difference between
 * javax.enterprise.context.ApplicationScoped,
 * javax.inject.Singleton and javax.ejb.Singleton.<p>
 * 
 * 
 * 
 * <h2>Arquillian and CDI scopes</h2>
 * 
 * Arquillian has "test enrichers" that do not provide a real environment. One
 * of those tries to map the CDI built-in scopes to the Arquillian test
 * class<sup>8</sup>. Me personally, I write real tests and use Arquillian
 * primarily for ease of deployment to a real server. With that said, the test
 * cases in this package will not depend on the scopes as Arquillian defines
 * them. Instead, test cases will make real HTTP request into the server and
 * then explore how scopes behave. Read more about my stand on Arquillian test
 * enrichers in the JavaDoc of
 * {@linkplain com.martinandersson.javaee.arquillian.helloworld.HelloWorldTest HelloWorldTest}.<p>
 * 
 * 
 * 
 * <h3>Note 1</h3>
 * 
 * JavaServer Faces 2.2 (JSR-344):
 * <pre>{@code
 *     https://jcp.org/en/jsr/detail?id=344
 * }</pre>
 * 
 * 
 * 
 * <h3>Note 2</h3>
 * 
 * Contexts and Dependency Injection for Java 1.1 (JSR-346):
 * <pre>{@code
 *     https://jcp.org/en/jsr/detail?id=346
 * }</pre>
 * 
 * 
 * 
 * <h3>Note 3</h3>
 * 
 * Dependency Injection for Java 1.0 (JSR-330):
 * <pre>{@code
 *     https://jcp.org/en/jsr/detail?id=330
 * }</pre>
 * 
 * 
 * 
 * <h3>Note 4</h3>
 * 
 * Java EE 7 Technologies:
 * <pre>{@code
 *     http://www.oracle.com/technetwork/java/javaee/tech/index.html
 * }</pre>
 * 
 * 
 * 
 * <h3>Note 5</h3>
 * 
 * Home page of Weld, the CDI reference implementation:
 * <pre>{@code
 *     http://weld.cdi-spec.org/
 * }</pre>
 * 
 * 
 * 
 * <h3>Note 6</h3>
 * 
 * This applies only to "normal scopes". An injection point ("@Inject field")
 * with a @Dependent as target (bean being injected) will always see a new
 * instance. It is "dependent" of the enclosing bean. See section "The @Depedent
 * pseudo-scope".<p>
 * 
 * 
 * 
 * <h3>Note 7</h3>
 * <pre>{@code
 *     http://docs.jboss.org/weld/reference/latest/en-US/html_single/
 * }</pre>
 * 
 * 
 * 
 * <h3>Note 8</h3>
 * <pre>{@code
 *     https://docs.jboss.org/author/display/ARQ/Active+scopes
 * }</pre>
 */
package com.martinandersson.javaee.cdi.scope;