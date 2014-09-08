package com.martinandersson.javaee.cdi.scope.applicationscoped;

import com.martinandersson.javaee.utils.Deployments;
import com.martinandersson.javaee.utils.PhasedExecutorService;
import java.util.concurrent.Callable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.collectingAndThen;
import java.util.stream.IntStream;
import javax.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unlike the EJB specification, the CDI specification doesn't say a thing about
 * multithreading semantics of the CDI singleton.<p>
 * 
 * Results: For WildFly and GlassFish, the CDI singleton proxy do not serialize
 * concurrent calls.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
public class ApplicationScopedConcurrencyTest {
    @Deployment
    public static WebArchive buildDeployment() {
        return Deployments.buildCDIBeanArchive(
                ApplicationScopedConcurrencyTest.class,
                ConcurrentInvocationCounter.class,
                PhasedExecutorService.class);
    }
    
    @Inject
    ConcurrentInvocationCounter bean;
    
    @Test
    public void applicationScopedBeanIsUnsynchronized() {
        
        /*
         * Can't reliably get thread count of ManagedExecutorService and using
         * ManagedThreadFactory with the PhasedExecutorService cause the app
         * to hang. The ManagedThreadFactory want an "application component
         * context" and Arquillian is what it is so I wouldn't expect more.
         * Anyways, whatever works:
         */
        PhasedExecutorService executor = new PhasedExecutorService();
        
        final int threadCount = executor.getThreadCount();
        
        Callable<Void> task = () -> { bean.sleepOneSecond(); return null; };
        
        IntStream.range(0, threadCount).mapToObj(x -> task)
                .collect(collectingAndThen(toList(), executor::invokeAll));
        
        assertEquals("Expected all but one thread to arrive concurrently.",
                threadCount - 1, ConcurrentInvocationCounter.getConcurrentCallsCount());
        
        // TODO: Move to own test:
        
        /*
         * TWO instances!? Containers usually create at least one extra for
         * inspection of the bean, then a another clean instance that is the one
         * receiving client calls. I've seen EJB containers create a LOT MORE
         * than just one extra! Here though, WildFly and GlassFish only create
         * one extra and I dare to hardcode a number 2.
         */
        assertEquals("Expected that two instances was created.",
                2, ConcurrentInvocationCounter.getInstancesCreated());
        
        // .. but the containers must not let the application think there are more than one instance:
        assertEquals("Expected only one @PostConstruct callback.",
                1, ConcurrentInvocationCounter.getLogicalInstancesCreated());
    }
}