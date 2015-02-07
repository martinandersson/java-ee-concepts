package com.martinandersson.javaee.cdi.scope.request;

import com.martinandersson.javaee.utils.DeploymentBuilder;
import com.martinandersson.javaee.utils.HttpRequests.RequestParameter;
import static com.martinandersson.javaee.utils.HttpRequests.getObject;
import com.martinandersson.javaee.utils.PhasedExecutorService;
import java.net.URL;
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
 * Tests in this class:
 * 
 * <ol>
 *   <li>{@linkplain #resuseOfRequestScoped() resuseOfRequestScoped()}</li>
 *   <li>{@linkplain #newRequestNewRequestScoped() newRequestNewRequestScoped()}</li>
 *   <li>{@linkplain #contextDoesNotPropagateAcrossAsynchronousEJB() contextDoesNotPropagateAcrossAsynchronousEJB()}</li>
 *   <li>{@linkplain #clientProxyConcurrency() clientProxyConcurrency()}</li>
 * </ol>
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
        return new DeploymentBuilder(RequestScopedTest.class)
                .addTestPackage()
                .build();
    }
    
    
    
    @ArquillianResource
    URL url;
    
    
    
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
     */
    @Test
    @RunAsClient
    @InSequence(1)
    public void resuseOfRequestScoped() {
        final TestDriver1.Report report = getObject(url, TestDriver1.class);
        assertTestReport(report);
        firstRequestScopedBeanId = OptionalInt.of(report.servletInjectedRequestScopedId);
    }
    
    /**
     * For each new HTTP request, a new @RequestScoped bean will be used.
     */
    @Test
    @RunAsClient
    @InSequence(2)
    public void newRequestNewRequestScoped() {
        
        final int firstRequestScopedBeanId = this.firstRequestScopedBeanId // <-- in this context (hiding a field), I really do feel that this.staticField instead of Type.staticField increase readability
                .orElseThrow(AssertionError::new);
        
        final TestDriver1.Report report = getObject(url, TestDriver1.class);
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
     */
    @Test
    @RunAsClient
    @InSequence(3)
    public void contextDoesNotPropagateAcrossAsynchronousEJB() {
        final TestDriver2.Report report = getObject(url, TestDriver2.class);
        
        assertNotEquals("Expected that an asynchronous EJB call equals a new @RequestScoped bean.",
                report.servletInjectedRequestScopedId, report.statelessOwnedRequestScopedId);
    }
    
    /**
     * Only one instance of the Servlet exists. The CDI client proxy must have
     * no issues dispatching concurrent calls to different @RequestScoped
     * instances.<p>
     * 
     * This is effectively a redo of the last test, only executed concurrently.
     */
    @Test
    @RunAsClient
    @InSequence(4)
    public void clientProxyConcurrency() {
        
        final PhasedExecutorService executor = new PhasedExecutorService();
        
        final RequestParameter sleep = new RequestParameter("sleep", "true");
        Callable<TestDriver1.Report> task = () -> getObject(url, TestDriver1.class, sleep);
        
        List<TestDriver1.Report> reports = executor.invokeManyTimes(task, executor.getThreadCount()).stream()
                .map(future -> {
                    try {
                        return future.get(); }
                    catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e); }})
                .collect(Collectors.toList());
        
        // Assert each report
        reports.stream().forEach(this::assertTestReport);
        
        // Make sure each Id was unique
        Set<Integer> uniqueIds = reports.stream()
                .map(report -> report.servletInjectedRequestScopedId)
                .collect(Collectors.toSet());
        
        assertEquals("All @RequestScoped beans must be unique.", executor.getThreadCount(), uniqueIds.size());
    }
    
    
    
    /*
     *  --------------
     * | INTERNAL API |
     *  --------------
     */
    
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