package com.martinandersson.javaee.ejb.sessionbeans.tests;

import com.martinandersson.javaee.ejb.sessionbeans.SingletonBean;
import com.martinandersson.javaee.ejb.sessionbeans.testdriver.EJBType;
import com.martinandersson.javaee.ejb.sessionbeans.testdriver.Operation;
import com.martinandersson.javaee.ejb.sessionbeans.testdriver.Report;
import org.jboss.arquillian.container.test.api.RunAsClient;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * A {@code Singleton} is not pooled. Only one such instance exist that the
 * application may use.<p>
 * 
 * As usual, don't expect that only one instance will ever be created.
 * Technically, many instances may be created by the EJB container for runtime
 * inspection. All client calls go to the same instance though and
 * {@code @PostConstruct} is called just once on the "real" instance used.<p>
 * 
 * Read more in the {@linkplain SingletonBean JavaDoc of SingletonBean}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class SingletonTest extends AbstractSessionTest
{
    public SingletonTest() {
        super(EJBType.SINGLETON);
    }
    
    
    
    @Test
    @RunAsClient
    public void bothReferencesGoToSameBean() {
        Report report = super.run(Operation.CALL_TWO_SERIALLY);
        
        assertEquals("Only one @Singleton instance was supposed to be used, ",
                report.beanId1, report.beanId2);
    }
    
    @Test
    @RunAsClient
    public void sameReferenceGoToSameBean() {
        Report report = run(Operation.CALL_ONE_SERIALLY);
        
        assertEquals("Only one @Singleton instance was supposed to be used, ",
                report.beanId1, report.beanId2);
    }
    
    @Test
    @RunAsClient
    public void concurrentInvocationsGoToSameBean() {
        Report report = run(Operation.CALL_ONE_CONCURRENTLY);
        
        assertEquals("Only one @Singleton instance was supposed to be used, ",
                report.beanId1, report.beanId2);
    }
    
    /**
     * Given that clients only have one @Singleton bean instance to use, the
     * self invoking pattern will cause a loopback call, which is allowed for
     * singletons.<p>
     * 
     * EJB 3.2 specification, section "4.8.5 Singleton Session Bean Concurrency":
     * <pre>{@code
     * 
     *     Singleton session beans support reentrant calls, i.e., where an
     *     outbound call from a singleton session bean method results in a
     *     loopback call to the singleton session bean on the same thread.
     *     Reentrant singleton session beans should be programmed and used with
     *     caution.
     * 
     * }</pre>
     * 
     * EJB 3.2 specification, section "4.8.5.1.1 Reentrant Locking Behavior":
     * <pre>{@code
     * 
     *     If a loopback call occurs on a singleton session bean that already
     *     holds a write lock on the same thread:
     *     
     *          If the target of the loopback call is a write method, the call
     *          must proceed immediately, without releasing the original write
     *          lock.
     * 
     * }</pre>
     */
    @Test
    @RunAsClient
    public void loopbackToWriteLockFromWriteLockIsLegal() {
        Report report = run(Operation.SELF_INVOKING_PROXY);
        
        assertEquals("Only one @Singleton instance was supposed to be used, ",
                report.beanId1, report.beanId2);
    }
}