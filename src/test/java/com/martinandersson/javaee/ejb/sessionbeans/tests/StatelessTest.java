package com.martinandersson.javaee.ejb.sessionbeans.tests;

import com.martinandersson.javaee.ejb.sessionbeans.testdriver.EJBType;
import com.martinandersson.javaee.ejb.sessionbeans.testdriver.Operation;
import com.martinandersson.javaee.ejb.sessionbeans.testdriver.Report;
import org.jboss.arquillian.container.test.api.RunAsClient;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 * Stateless beans are meant to encapsulate transactional business logic. From
 * the client's perspective, the bean does not retain "conversational" state
 * (instance fields populated by the client). The bean is most likely very
 * dependent on the arguments passed in to bean methods.<p>
 * 
 * Because of this programming model, the server is able to pool the bean
 * instances and cherry-pick one for service when the instance is free. What the
 * client see is a reused instance (except the very first invocation of
 * course).<p>
 * 
 * No time lost for setting up new beans translate to increased scalability.
 * Yet the server will throttle the resource consumption and not create new
 * beans uncontrollably.<p>
 * 
 * The stateless bean instance is never used concurrently by multiple clients
 * and the bean may therefore use resources tied to him in form of instance
 * fields. Given that the bean is pooled, resources used by the bean benefit as
 * well from this type of management.<p>
 * 
 * It is extremely important to note that a client that call a method of a
 * stateless bean [proxy-] reference can never be sure which bean instance will
 * be the one that receive and service the call. This even applies when client
 * code call the same bean method in two consecutive statements using the same
 * proxy reference. A bean programmed to retain state from one method call to
 * another, and client code tied to this assumption, is a catastrophe waiting to
 * happen.
 * 
 * 
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class StatelessTest extends AbstractSessionTest
{
    public StatelessTest() {
        super(EJBType.STATELESS);
    }
    
    
    
    /*
     *  -------
     * | TESTS |
     *  -------
     */
    
    /*
     * NOTE! As long as access to a stateless bean happens serially, one cannot
     * test or assume anything about which bean instance will be used to service
     * the call. Therefore, many tests here assert only that one or the other
     * stateless bean reference could be used. To be perfectly clear,
     * 
     * the only time we can assume anything about the bean ids (that they will
     * be different from one another) is when our code concurrently invoke a
     * stateless bean, only then must the calls go to different bean instances.
     */
    
    @Test
    @RunAsClient
    public void canUseBothReferencesOfCourse() {
        Report report = super.run(Operation.CALL_TWO_SERIALLY);
        assertNonNullReportAndPositiveIds(report);
    }
    
    @Test
    @RunAsClient
    public void canUseOneReferenceMultipleTimes() { // <-- quite a pointless test really
        Report report = run(Operation.CALL_ONE_SERIALLY);
        assertNonNullReportAndPositiveIds(report);
    }
    
    @Test
    @RunAsClient
    public void concurrentInvocationsGoToDifferentBeans() {
        Report report = run(Operation.CALL_ONE_CONCURRENTLY);
        
        // NOT equals..
        assertNotEquals("Different @Stateless instances was supposed to be used, ",
                report.beanId1, report.beanId2);
    }
    
    /**
     * EJB 3.2 specification, section "4.10.13 Non-reentrant Instances":
     * <pre>{@code
     * 
     *     The container must ensure that only one thread can be executing a
     *     stateless or stateful session bean instance at any time. Therefore,
     *     stateful and stateless session beans do not have to be coded as
     *     reentrant. One implication of this rule is that an application cannot
     *     make loopback calls to a stateless or stateful session bean instance.
     * 
     * }</pre>
     * 
     * But.. calling same stateless reference as the one used to make the method
     * execute in the first place does not produce a loopback call as this test
     * demonstrate. Both GlassFish and WildFly pass this test. Both servers
     * simply route the call to another bean instance. And that is exactly what
     * the quote said in the first sentence.<p>
     * 
     * So me personally, I cannot understand what "implication this rule" has
     * for stateless beans. Stateful beans is another story.<p>
     * 
     * The real cool thing to note here is that even though there's little value
     * of passing around a stateless bean reference, one can still do so and
     * feel confident that no stateless bean will ever see multiple threads
     * execute him at the same time.
     */
    @Test
    @RunAsClient
    public void loopbackDoesntExist() {
        Report report = run(Operation.SELF_INVOKING_PROXY);
        
        // NOT equals..
        assertNotEquals("Different @Stateless instances was supposed to be used, ",
                report.beanId1, report.beanId2);
    }
    
    
    
    /*
     *  --------------
     * | INTERNAL API |
     *  --------------
     */
    
    private void assertNonNullReportAndPositiveIds(Report report) {
        assertNotNull("Had my hopes high that the test could at least be run, ", report);
        assertEquals(true, report.beanId1 > 0);
        assertEquals(true, report.beanId2 > 0);
    }
}