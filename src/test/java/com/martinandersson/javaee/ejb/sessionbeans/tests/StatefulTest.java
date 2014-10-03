package com.martinandersson.javaee.ejb.sessionbeans.tests;

import com.martinandersson.javaee.ejb.sessionbeans.testdriver.EJBType;
import com.martinandersson.javaee.ejb.sessionbeans.testdriver.Operation;
import com.martinandersson.javaee.ejb.sessionbeans.testdriver.Report;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import org.junit.Test;

/**
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class StatefulTest extends AbstractSessionTest
{
    public StatefulTest() {
        super(EJBType.STATEFUL);
    }
    
    
    
    @Test
    @RunAsClient
    public void allReferencesGoToDifferentBeans() {
        Report report = run(Operation.CALL_TWO_SERIALLY);
        
        assertNotEquals("Expected that a non-contextual (i.e. @EJB, not @Inject) @Stateful lookup produces a new bean, ",
                report.beanId1, report.beanId2);
    }
    
    @Test
    @RunAsClient
    public void consecutiveInvocationsGoToSameBean() {
        Report report = run(Operation.CALL_ONE_SERIALLY);
        
        assertEquals("Expected that a @Stateful proxy reference doesn't change bean target, ",
                report.beanId1, report.beanId2);
    }
    
    @Test
    @RunAsClient
    public void simultaneousInvocationsGoToSameBean() {
        Report report = run(Operation.CALL_ONE_CONCURRENTLY);
        
        assertEquals("Expected that a @Stateful proxy reference doesn't change bean target, ",
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
     * For me, this quote isn't that clear. The quote do not say that loopbacks
     * are forbidden. It says that as a result of the thread-safety of stateless
     * and stateful beans, the application cannot "make" such a call.<p>
     * 
     * In the case of stateless beans, that is absolutely true. The call simply
     * go to another bean and isn't a loopback call (see {@linkplain
     * StatelessTest#loopbackDoesntExist()}).<p>
     * 
     * In the case of stateful beans, then I think that a loopback call should
     * be allowed. It is the same thread calling so what's the harm? WildFly
     * agree on this point and let the call through. GlassFish crash and will
     * throw a {@code javax.ejb.IllegalLoopbackException}. This exception is
     * mentioned only once in the EJB 3.2 specification and speak of illegal
     * loopback calls made on singleton beans.<p>
     * 
     * I would be quick to call the occurrence of the exception as a failure of
     * GlassFish and let him fail the test had it not been for one peculiar
     * detail in the JavaDoc of {@code IllegalLoopbackException}:
     * 
     * <pre>{@code
     * 
     *     This exception indicates that an attempt was made to perform an
     *     illegal loopback invocation. One possible cause is a loopback call to
     *     a singleton bean's container-managed concurrency Lock(WRITE) method
     *     where the current thread does not already hold a WRITE lock.
     * 
     * }</pre>
     * 
     * The JavaDoc doesn't say that going from READ to WRITE on a singleton
     * using a loopback call is the only cause for this exception, the JavaDoc
     * say it is <i>one</i> such possible cause and imply that other causes
     * might exist.<p>
     * 
     * Solution for now is to let WildFly and GlassFish both pass this test. The
     * developer must be vary of using loopback calls on stateful beans as it
     * clearly isn't portable.<p>
     * 
     * Also see: http://stackoverflow.com/q/8002848/1268003
     */
    @Test
    @RunAsClient
    @InSequence(99) // <-- EJB crash will make bean reference invalid, so for the benefit of GF and all other tests in this class, execute this one last
    public void loopbackIsNonPortable() {
        Report report = run(Operation.SELF_INVOKING_PROXY);
        
        if (report.exception != null) {
            assertEquals("Illegal loopback exception",
                    javax.ejb.IllegalLoopbackException.class, report.exception.getClass());
        }
        else {
            assertEquals("Expected that a @Stateful reference doesn't change target,",
                    report.beanId1, report.beanId2);
        }
    }
}