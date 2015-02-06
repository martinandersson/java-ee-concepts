package com.martinandersson.javaee.cdi.scope.application;

import com.martinandersson.javaee.utils.Deployments;
import com.martinandersson.javaee.utils.PhasedExecutorService;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedThreadFactory;
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
 * concurrent calls.<p>
 * 
 * <strong>Warning!</strong> GlassFish takes about a full minute on my machine
 * to execute this test. WildFly 8.1.0 take 5 seconds =)
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
    
    @Resource
    ManagedThreadFactory threadFactory;
    
    @Test
    public void applicationScopedBeanIsUnsynchronized() {
        
        PhasedExecutorService executor = new PhasedExecutorService(threadFactory);
        final int threadCount = executor.getThreadCount();
        
        executor.invokeManyTimes(bean::sleepOneSecond, threadCount);
        
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