package com.martinandersson.javaee.cdi.producers.entitymanager.unsafe;

import com.martinandersson.javaee.cdi.producers.entitymanager.unsafe.OracleTestDriver.Report;
import com.martinandersson.javaee.resources.SchemaGenerationStrategy;
import com.martinandersson.javaee.utils.DeploymentBuilder;
import com.martinandersson.javaee.utils.Deployments;
import com.martinandersson.javaee.utils.HttpRequests;
import java.net.URL;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This test will "prove" that using CDI to lookup a container-managed entity
 * manager might put you at risk using the entity manager concurrently and the
 * disposer method will not close the entity manager.<p>
 * 
 * 
 * 
 * <h2>Background</h2>
 * 
 * Using CDI to lookup an entity manager is only useful for application-managed
 * entity managers or if you have more than one persistence unit. The latter
 * allow one to map a String to a Qualifier.<p>
 * 
 * As the
 * {@linkplain com.martinandersson.javaee.cdi.producers.entitymanager package-info.java}
 * file try to argue, even String-mapping has alternatives and using CDI for
 * such a small task will probably only add complexity to your application
 * code base and slow down performance.<p>
 * 
 * But more importantly, using CDI to inject container-managed entity managers
 * put you at risk of using the entity manager wrongfully if using a too wide
 * CDI scope. The entity manager reference is not required to be thread-safe.<p>
 * 
 * Many Internet "Hello World" samples will combine a producer- field or method
 * with a disposer method that also close the container-managed entity manager.
 * This practice is really taking it too far as the container-managed entity
 * manager must not be closed by application code. {@code EntityManager.close()}
 * will throw an {@code IllegalStateException} that is swallowed by the CDI
 * container and the entity manager itself will remain open.<p>
 * 
 * For details, please refer to
 * {@linkplain com.martinandersson.javaee.cdi.producers.entitymanager package-info.java}.
 * The remaining JavaDoc will examine the structure of this test.<p>
 * 
 * 
 * 
 * <h2>Test goals</h2>
 * 
 * To the best of my ability, this test will recreate and examine this example
 * from the official Java EE 7 tutorial provided by Oracle:
 * <pre>{@code
 * 
 *     http://docs.oracle.com/javaee/7/tutorial/cdi-adv-examples003.htm#GKHRG
 * }</pre>
 * 
 * The Oracle example put a producer field on the inside of a
 * {@code @javax.inject.Singleton}. It then dispose the entity manager by
 * calling {@code EntityManager.close()}. Both practices are a terrible thing to
 * do.<p>
 * 
 * The CDI 1.2 specification itself has a similar example (section
 * 3.5.2)<sup>1</sup>. Although the specification use a {@code @Dependent}
 * scope, they still try to close the entity manager in a disposer method.<p>
 * 
 * This code:
 * <pre>
 * 
 *     &#064;PersistenceContext
 *     EntityManager em;
 * 
 * </pre>
 * 
 * ..will inject the entity manager only once. The field is not "live", in that
 * it is magically reinitialized some time later. The reference we've acquired
 * must not be called concurrently.<p>
 * 
 * Whether or not the instance injected into the field is a entity manager
 * proxy, or the entity manager implementation, is unspecified. In this test
 * class and related files, the instance injected will simply be referred to as
 * the "JPA proxy".<p>
 * 
 * We can if we will, make the field a CDI "producer field":
 * <pre>
 * 
 *     &#064;Produces
 *     &#064;PersistenceContext
 *     EntityManager em;
 * 
 * </pre>
 * 
 * Currently, this producer has scope {@code @Dependent}. Every time someone
 * {@code @Inject} an entity manager, this field is reread (assuming the
 * producer class itself has a wide enough scope) and then [most likely] proxied
 * by the CDI container. In this test class and related files, the instance
 * provided to client code that uses {@code @Inject} is referred to as the "CDI
 * proxy".<p>
 * 
 * It is important to understand that we're actually dealing with two proxies
 * here. One provided to us by the unnamed container that injected <i>the</i>
 * entity manager using annotation {@code @PersistenceContext}, and one wrapper
 * proxy that is returned to the client code using {@code @Inject}.<p>
 * 
 * Furthermore, the "JPA proxy" may forward the call even deeper down the stack
 * to a delegate of its own. This object may be retrieved using {@code
 * EntityManager.getDelegate()}. But, and this is quite important, this test
 * won't bother one bit about the identity or life cycle of the object instance
 * that is furthest away.<p>
 * 
 * Most likely, the JPA proxy is a thread-local object. If so, then the entity
 * manager <strong>is</strong> thread-safe. But, it is not required to be
 * thread-safe. From the application code's perspective, the JPA proxy is the
 * entity manager and this reference must not be invoked concurrently.
 * Furthermore, the JPA proxy represents a container-managed entity manager
 * which must not be closed.<p>
 * 
 * So what this test will focus on is the identity and life cycle of the JPA
 * proxy. It will be shown, that by declaring the producer field in a
 * {@code @javax.inject.Singleton}, the CDI container will distribute the same
 * JPA proxy reference to many threads. It will also be shown that using a
 * disposer method to close a container-managed entity manager has no effect
 * other than to throw the expected {@code IllegalStateException}.<p>
 * 
 * 
 * 
 * <h2>How to track the JPA proxy</h2>
 * 
 * Given that it is the JPA proxy reference we're trying to track, how can it be
 * retrieved from the CDI proxy that our client code use? Unfortunately, because
 * the CDI proxy has a {@code @Dependent} scope (a <i>pseudo</i> scope), it can
 * not be done:
 * <pre>{@code
 *     https://issues.jboss.org/browse/CDI-10
 * }</pre>
 * 
 * Instead, I've had to take a detour.<p>
 * 
 * {@code EMProducerExtension} is a CDI extension that will be installed in the
 * test archive sent to the server. During deployment time, the extension will
 * replace the container's entity manager producer with our own customized
 * producer. Our custom producer will delegate all calls to the container's
 * original producer and will therefore not affect how the lookup happens (lol,
 * at least in theory).<p>
 * 
 * But, when the CDI container ask our custom producer for the JPA proxy
 * produced by the producer field, what the container will get back is a proxy
 * to the JPA proxy. One we build ourselves. Our JPA "proxy proxy" store a local
 * reference to the underlying JPA proxy instance. The only trick left is how to
 * get it out from our proxy to client code.<p>
 * 
 * The JPA proxy proxy will delegate all calls to the real JPA proxy instance,
 * unless client code call {@code EntityManager.unwrap()} with the {@code
 * EntityManager.class} as specified argument. For this particular invocation,
 * the proxy proxy will intercept the call and return the real underlying JPA
 * proxy instance.<p>
 * 
 * And that is how client code in related tests classes retrieve and identify
 * the underlying JPA proxy instance behind the CDI proxy instance that client
 * code see. Client code make a "secret" call and our "man in the middle" of
 * the CDI proxy and the JPA proxy will respond with a nice leak of the JPA
 * proxy.<p>
 * 
 * 
 * 
 * <h2>Implementation</h2>
 * 
 * Firstly, the qualifier {@code @UserDatabase} is used in this test only to
 * provide the same environment as the Oracle Java EE 7 tutorial example. But it
 * really does not matter for the outcome of this test (verified). Without a
 * custom qualifier, each injection point and injection target will have the
 * {@code @Default} qualifier. So, it is safe for you to ignore this type.<p>
 * 
 * This test class make a HTTP GET requests to a test Servlet ({@code
 * OracleTestDriver}). The Servlet will use two threads executed serially to
 * do the same investigating job. The job will prove that both threads saw the
 * same JPA proxy reference and both of them caught an {@code
 * IllegalStateException} when the disposer method called {@code
 * EntityManager.close()}.<p>
 * 
 * Each job will begin by looking up an entity manager through a entity manager
 * wrapper ({@code EntityManagerConsumer}). The thread will apply the hack
 * previously described to store the JPA proxy identity, and then destroy the
 * wrapper bean. This destruction is what triggers the disposer method to be
 * called (in same thread). When all is done, a report for each job is sent
 * back to this test class which examines the result.<p>
 * 
 * You may ask why I use a entity manager wrapper that is application scoped or
 * why I didn't just lookup the entity manager directly? Reason is that during
 * development, Weld ignored calls to destroy dependent objects.<p>
 * 
 * 
 * 
 * <h4>Note 1</h4>
 * 
 * https://issues.jboss.org/browse/CDI-523
 * 
 * 
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
@RunAsClient
public class OracleTest
{
    @Deployment
    private static Archive<?> buildDeployment() {
        /*
         * WildFly will not pickup OracleProducer if beans.xml file is not
         * provided, which is right. Without beans.xml, only classes that has a
         * bean defining scope is processed by CDI.
         * 
         * CDI 1.2, section "2.5.1. Bean defining annotations":
         * 
         *     "Note that to ensure compatibility with other JSR-330
         *      implementations, all pseudo-scope annotations except @Dependent
         *      are not bean defining annotations."
         * 
         * OracleProducer is annotated javax.inject.Singleton, which in turn is
         * annotated @Scope.
         * 
         * CDI section "6.3. Normal scopes and pseudo-scopes":
         * 
         *     "All pseudo-scopes must be explicitly declared @Scope, to
         *      indicate to the container that no client proxy is required."
         * 
         * But, GlassFish pass this test even without beans.xml provided.
         * TODO: Report GlassFish bug.
         * 
         * For more info about CDI packaging, see package
         * "com.martinandersson.javaee.cdi.packaging".
         */
        WebArchive archive = new DeploymentBuilder(OracleTest.class)
                .addTestPackage()
                .addEmptyBeansXMLFile()
                .addPersistenceXMLFile(SchemaGenerationStrategy.UPDATE)
                .build();
        
        return Deployments.installCDIExtension(archive, EMProducerExtension.class);
    }
    
    @Test
    public void test_OracleCDIEntityManagerExample(@ArquillianResource URL url) {
        OracleTestDriver.Report[] reports = HttpRequests.getObject(url);
        
        Report one = reports[0],
               two = reports[1];
        
        // Producer class is @Singleton:
        assertEquals(1, one.producersCreated);
        assertEquals(1, two.producersCreated);
        
        // One proxy wraps the CDI field, one is the CDI field:
        assertNotEquals(one.cdiProxyId, one.jpaProxyId);
        assertNotEquals(two.cdiProxyId, two.jpaProxyId);
        
        // Closing a container-managed entity manager throw IllegalStateException:
        assertEquals(IllegalStateException.class, one.disposerException.getClass());
        assertEquals(IllegalStateException.class, two.disposerException.getClass());
        
        // ..and both proxies report that they remained open:
        assertTrue(one.cdiProxyRemainedOpen);
        assertTrue(one.jpaProxyRemainedOpen);
        
        assertTrue(two.cdiProxyRemainedOpen);
        assertTrue(two.jpaProxyRemainedOpen);
        
        /*
         * Producer field was only injected once. The Oracle example exposes a
         * entity manager reference to multiple threads despite the reference
         * not being required to be thread-safe:
         */
        assertEquals(one.jpaProxyId, two.jpaProxyId);
    }
}