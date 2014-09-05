package com.martinandersson.javaee.cdi.scope.request;

import com.martinandersson.javaee.utils.PhasedExecutorService;
import com.martinandersson.javaee.utils.Deployments;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * <h2>Results using WildFly 8.1.0</h2>
 * 
 * Work flawlessly. Sometimes though, first run might crash, complaining about
 * deployment failure, or the deployment/test just hangs indefinitely.
 * Restarting the test solve the problem.<p>
 * 
 * The time if takes for WildFly on my machine is about 28 seconds.
 * 
 * 
 * 
 * <h2>Results using GlassFish 4.0.1-b08-m1</h2>
 * 
 * Works for most of the time, but sometimes - in particular so when redoing the
 * test a consecutive second time - fail throwing a {@code
 * java.net.SocketException} with the message:
 * <pre>{@code
 *     No buffer space available (maximum connections reached?): connect
 * }</pre>
 * 
 * It seem to be that GlassFish doesn't close the sockets fast enough. One fix,
 * not currently applied, is to make each request sleep for 10 milliseconds
 * before making the next request.
 * 
 * The time if takes for GlassFish on my machine is about 20 seconds.
 * 
 * 
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
public class RequestScopedTest
{
    private static final Logger LOGGER = Logger.getLogger(RequestScopedTest.class.getName());
    
    private static OptionalInt firstRequestScopedBeanId = OptionalInt.empty();
    
    
    
    @Deployment
    public static WebArchive buildDeployment() {
        // No beans.xml provided makes this deployment a "implicit bean archive":
        return Deployments.buildWAR(
                RequestScopedTest.class,
                TestDriver1.class,
                TestDriver2.class,
                RequestScopedBean.class,
                ApplicationScopedBean.class,
                StatelessBean.class);
    }
    
    
    
    /**
     * We expect that..
     * 
     * <ol>
     *   <li>A nested @RequestScoped bean, on the inside of another @RequestScoped
     *       bean, is the same instance as his container.
     *   </li><br />
     *   
     *   <li>The only @RequestScoped bean created, is also the same bean used
     *       by the @ApplicationScoped bean.</li>
     * </ol>
     * 
     * How can the @RequestScoped bean have a dependency on itself without
     * causing the container to hit a StackOverflowError? How can (as will
     * ultimately be proved in the last test case) a client-specific @RequestScoped
     * bean be injected into a shared @ApplicationScoped bean?<p>
     * 
     * Please review the package description of
     * {@code com.martinandersson.javaee.cdi.scope}, section "Client proxies".
     * 
     * @param url deployed application URL, provided by Arquillian
     */
    @Test
    @RunAsClient
    @InSequence(1)
    public void resuseOfRequestScoped(@ArquillianResource URL url) {
        final TestDriver1.Report report = makeRequest(url, TestDriver1.class);
        assertTestReport(report);
        firstRequestScopedBeanId = OptionalInt.of(report.servletInjectedRequestScopedId);
    }
    
    /**
     * For each new HTTP request, a new @RequestScoped bean will be used.
     * 
     * @param url deployed application URL, provided by Arquillian
     */
    @Test
    @RunAsClient
    @InSequence(2)
    public void newRequestNewRequestScoped(@ArquillianResource URL url) {
        
        final int firstRequestScopedBeanId = this.firstRequestScopedBeanId
                .orElseThrow(AssertionError::new);
        
        final TestDriver1.Report report = makeRequest(url, TestDriver1.class);
        assertTestReport(report);
        
        // The real deal:
        assertNotEquals("Expected that a new HTTP request equals a new @RequestScoped bean.",
                firstRequestScopedBeanId, report.servletInjectedRequestScopedId);
    }
    
    /**
     * The {@code @RequestScoped} bound context is <strong>not</strong>
     * propagated across {@code @Asynchronous} method calls. It is only
     * "active". Invoking an {@code @Asynchronous} method is like making a new
     * HTTP request from scratch again.<p>
     * 
     * CDI 1.1, section "6.7 Context management for built-in scopes":
     * <pre>{@code
     * 
     *     The context associated with a built-in normal scope propagates
     *     across local, synchronous Java method calls, including invocation of
     *     EJB local business methods. The context does not propagate across
     *     remote method invocations or to asynchronous processes such as JMS
     *     message listeners or EJB timer service timeouts.
     * 
     * }</pre>
     * 
     * CDI 1.1 section 6.7.1 "Request context lifecycle":
     * <pre>{@code
     *     The request scope is active [..] during any asynchronous method
     *     invocation of any EJB [..].
     * }</pre>
     * 
     * Also see {@code ./com/martinandersson/javaee/cdi/scope/package-info.java}.
     * 
     * @param url deployed application URL, provided by Arquillian
     */
    @Test
    @RunAsClient
    @InSequence(3)
    public void contextDoesNotPropagateAcrossAsynchronousEJB(@ArquillianResource URL url) {
        final TestDriver2.Report report = makeRequest(url, TestDriver2.class);
        
        assertNotEquals("Expected that an asynchronous EJB call equals a new @RequestScoped bean.",
                report.servletInjectedRequestScopedId, report.statelessOwnedRequestScopedId);
    }
    
    /**
     * Only one instance of the Servlet exists. The CDI client proxy must have
     * no issues dispatching concurrent calls to different @RequestScoped
     * instances.<p>
     * 
     * This is effectively a redo of the last test, only executed concurrently.
     * 
     * @param url deployed application URL, provided by Arquillian
     */
    @Test
    @RunAsClient
    @InSequence(4)
    public void clientProxyConcurrency(@ArquillianResource URL url) {
        
        // Amount of threads to use (I deliberately want contention):
        final int N = Runtime.getRuntime().availableProcessors() * 20;
        
        // Amount of HTTP requests:
        final int M = N * 100;
        
        Callable<TestDriver1.Report> task = () -> makeRequest(url, TestDriver1.class);
        
        List<Callable<TestDriver1.Report>> tasks = new ArrayList<>(M);
        
        for (int m = M; m > 0; --m)
            tasks.add(task);
        
        PhasedExecutorService executor = new PhasedExecutorService(N);
        
        List<TestDriver1.Report> reports = executor.invokeAll(tasks).stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());
        
        // Assert each report
        reports.stream().forEach(this::assertTestReport);
        
        // Make sure each Id was unique
        Set<Integer> uniqueIds = reports.stream()
                .map(report -> report.servletInjectedRequestScopedId)
                .collect(Collectors.toSet());
        
        /*
         * If this test fails, it doesn't have to be wrong with the server. In
         * theory, it will fail from time to time. See JavaDoc of
         * RequestScopedBean.getId().
         */
        assertEquals("All @RequestScoped beans must be unique.", M, uniqueIds.size());
    }
    
    
    
    /*
     *  --------------
     * | INTERNAL API |
     *  --------------
     */
    
    /**
     * Will make a HTTP GET-request to the {@code TestDriver}.
     * 
     * @param url deployed application URL, provided by Arquillian
     * 
     * @return the test report, as provided by {@code TestDriver}
     */
    private <T> T makeRequest(URL url, Class<?> testDriverType)
    {
        final URL testDriver;
        final URLConnection conn;
        
        try {
            testDriver = new URL(url, testDriverType.getSimpleName());
            conn = testDriver.openConnection();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        
        conn.setRequestProperty("Connection", "close");
        
        try (ObjectInputStream in = new ObjectInputStream(conn.getInputStream());) {
            return (T) in.readObject();
        }
        catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Will assert that all @RequestScoped bean Id:s are the same, independently
     * of where the bean was injected.
     * 
     * @param report the test report, as provided by {@code TestDriver1}
     */
    private void assertTestReport(TestDriver1.Report report) {
        assertEquals("Expected same @RequestScoped bean injected in Servlet as the one nested inside.",
                report.servletInjectedRequestScopedId, report.selfNestedRequestScopedId);
        
        assertEquals("Expected same bean also used by @ApplicationScoped bean.",
                report.selfNestedRequestScopedId, report.singletonOwnedRequestScopedId);
        
        assertEquals("Expected same bean also used by @Stateless bean.",
                report.singletonOwnedRequestScopedId, report.statelessOwnedRequestScopedId);
    }
}