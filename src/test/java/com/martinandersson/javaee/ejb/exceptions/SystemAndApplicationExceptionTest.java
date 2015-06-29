package com.martinandersson.javaee.ejb.exceptions;

import com.martinandersson.javaee.utils.DeploymentBuilder;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * EJB 3.2 specification chapter 9, define two "types" of exceptions: "system"-
 * and "application" exceptions.<p>
 * 
 * 
 * 
 * <h3>Exception definitions</h3>
 * 
 * EJB 3.2, section 9.2.2 System Exceptions:
 * <pre>{@code
 *     A system exception is an exception that is a java.rmi.RemoteException (or
 *     one of its subclasses) or a RuntimeException that is not an application
 *     exception.
 * }</pre>
 * 
 * EJB 3.2, section 9.2.1 Application Exceptions:
 * <pre>{@code
 *     Application exception that is a checked exception is defined as such by
 *     being listed in the throws clause of a method on the bean’s business
 *     interface [..]. An application exception that is an unchecked exception
 *     is defined as an application exception by annotating it with the
 *     ApplicationException metadata annotation [..].
 * }</pre>
 * 
 * 
 * 
 * <h3>The failure of EJB exception handling</h3>
 * 
 * Basically, the EJB specification has a rather pessimistic world view and
 * treat all runtime exceptions (not annotated with
 * {@code @ApplicationException}) as something clients don't want to, or rather
 * - should not, handle. For example, "the transaction in which the bean method
 * participated will be rolled back"<sup>1</sup>. Once a transaction has been
 * marked to rollback, there's no way to undo that.<p>
 * 
 * These so called "system-level exceptions", are not even thrown as is. For
 * some reason, they are wrapped in a non-intuitive {@code EJBException} and the
 * specification does not even require the container to keep the cause in the
 * exception stack except for {@code @MessageDriven} beans<sup>1</sup>.
 * 
 * The current design of system-level exceptions contradicts the outspoken goal
 * of EJB to provide clients a "safe" way to handle "unexpected
 * exceptions"<sup>2</sup>. The unexpected exception will definitely screw up
 * your transaction, translate into another exception type and the original
 * exception might forever be lost in cyber space!<p>
 * 
 * To be fair, the EJB specification do require of the container that he log the
 * exception before throwing the EJBException<sup>1</sup> (meaning that the
 * original cause will technically speaking never be "lost in cyber space" -
 * only lost in the log file).<p>
 * 
 * The wrapping is unfortunate because many other places in the EJB
 * specification mandate the container to throw the EJBException as well,
 * causing the bean developer one huge headache trying to differentiate
 * problems. Although not fully applicable in this context, one such place is
 * calling {@code @Asynchronous} methods. If the container cannot allocate a
 * thread to run the job, an EJBException is thrown - not a {@code
 * RejectedExecutionException} which would normally be the exception to look for
 * in a Java SE environment.<p>
 * 
 * So, there you go. What a super safe way to handle unexpected exceptions! Just
 * log them. That's all you ever want to do with exceptions right? Well no.<p>
 * 
 * One system-level exception is the {@code OptimisticLockException} defined in
 * the Java Persistence API. This is just the type of exception any normal Java
 * EE application would love to catch. But it is impossible to do so in a
 * portable way outside the EJB container. Even if you catch the exception
 * within the EJB code, do note that the JPA 2.1 specification require that the
 * {@code EntityManager} mark the transaction to roll back<sup>3</sup>.<p>
 * 
 * In this particular case, the only portable way to handle an
 * OptimisticLockException is to flush the entity manager within the EJB
 * component and translate the OptimisticLockException to an application
 * exception that the client outside the EJB container and hopefully, outside
 * the transaction, catch as-is. Handling exceptions from a
 * {@code @Transactional} CDI bean is even worse<sup>4</sup>.<p>
 * 
 * But the EJB container more or less keep his hands in the pocket and won't
 * fuck you up if you use application exceptions instead, i.e, checked
 * exceptions or runtime exceptions annotated @ApplicationException.<p>
 * 
 * 
 * 
 * <h3>What this test class does</h3>
 * 
 * The specification as it has been described previously is quite straight
 * forward and should not have any surprises in store as long as you have a
 * solid understanding of the EJB exception framework. However, the
 * specification does not comment what happens if you invoke a colocated
 * {@code @Remote} bean. This test class shows that for GlassFish 4.1 and
 * WildFly 9.0.0.CR1, there is no difference.<p>
 * 
 * 
 * 
 * 
 * <h4>Note 1</h4>
 * 
 * EJB 3.2, section "9.2.2 System Exceptions".
 * 
 * 
 * <h4>Note 2</h4>
 * 
 * EJB 3.2, section "9.1.2 Goals for Exception Handling".
 * 
 * 
 * <h4>Note 3</h4>
 * 
 * JPA 2.1, section "3.4.1 Optimistic Locking" and
 * "3.4.5 OptimisticLockException". 
 * 
 * 
 * <h4>Note 4</h4>
 * 
 * {@code
 * https://github.com/MartinanderssonDotcom/java-ee-concepts/blob/master/src/test/java/com/martinandersson/javaee/cdi/transactional/ExceptionCauseTest.java
 * }
 * 
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
public class SystemAndApplicationExceptionTest
{
    @Deployment
    private static Archive<?> buildDeployment() {
        return new DeploymentBuilder(SystemAndApplicationExceptionTest.class)
                .add(RemoteCrashingBean.class,
                     CrashingBean2.class,
                     CustomApplicationException.class)
                .build();
    }
    
    
    
    @EJB
    RemoteCrashingBean remote;
    
    @EJB
    CrashingBean2 local;
    
    
    
    /*
     *  -------
     * | LOCAL |
     *  -------
     */
    
    /**
     * GlassFish 4.1 and WildFly 9.0.0.CR1 both throw:
     * <pre>
     *   javax.ejb.EJBException, Caused by: java.lang.RuntimeException: 123456
     * </pre>
     */
    @Test
    public void test_local_uncheckedSystemException() {
        try {
            local.uncheckedSystemException();
            fail("No exception thrown.");
        }
        catch (EJBException e) {
            assertEquals(EJBException.class, e.getClass());
            
            Throwable cause = e.getCause();
            assertEquals(RuntimeException.class, cause.getClass());
            assertEquals("123456", cause.getMessage());
        }
    }
    
    /**
     * GlassFish 4.1 and WildFly 9.0.0.CR1 both throw:
     * <pre>
     *   javax.ejb.EJBException, Caused by: java.lang.RuntimeException: 123456
     * </pre>
     * 
     * Note that having the exception declared in the method throw clause does
     * not change the "system exception" into an "application exception". I.e.,
     * the exception will continue to be wrapped in a EJBException.
     */
    @Test
    public void test_local_unchekedSystemException_declared() {
        try {
            local.uncheckedSystemException_declared();
            fail("No exception thrown.");
        }
        catch (EJBException e) {
            assertEquals(EJBException.class, e.getClass());
            
            Throwable cause = e.getCause();
            assertEquals(RuntimeException.class, cause.getClass());
            assertEquals("123456", cause.getMessage());
        }
    }
    
    /**
     * GlassFish 4.1 and WildFly 9.0.0.CR1 both throw:
     * <pre>
     *   com.martinandersson.javaee.ejb.exceptions.CustomApplicationException: 123456
     * </pre>
     */
    @Test
    public void test_local_uncheckedApplicationException() {
        try {
            local.uncheckedApplicationException();
            fail("No exception thrown.");
        }
        catch (CustomApplicationException e) {
            assertEquals("123456", e.getMessage());
        }
    }
    
    /**
     * GlassFish 4.1 and WildFly 9.0.0.CR1 both throw:
     * <pre>
     *   java.lang.Exception: 123456
     * </pre>
     */
    @Test
    public void test_local_checkedException() {
        try {
            local.checkedException();
            fail("No exception thrown.");
        }
        catch (Exception e) {
            assertEquals(Exception.class, e.getClass());
            assertEquals("123456", e.getMessage());
        }
    }
    
    
    
    /*
     *  -------
     * | REMOTE |
     *  -------
     */
    
    /**
     * GlassFish 4.1 and WildFly 9.0.0.CR1 both throw:
     * <pre>
     *   javax.ejb.EJBException, Caused by: java.lang.RuntimeException: 123456
     * </pre>
     */
    @Test
    public void test_remote_uncheckedSystemException() {
        try {
            remote.uncheckedSystemException();
            fail("No exception thrown.");
        }
        catch (EJBException e) {
            assertEquals(EJBException.class, e.getClass());
            
            Throwable cause = e.getCause();
            assertEquals(RuntimeException.class, cause.getClass());
            assertEquals("123456", cause.getMessage());
        }
    }
    
    /**
     * GlassFish 4.1 and WildFly 9.0.0.CR1 both throw:
     * <pre>
     *   javax.ejb.EJBException, Caused by: java.lang.RuntimeException: 123456
     * </pre>
     * 
     * Note that having the exception declared in the method throw clause does
     * not change the "system exception" into an "application exception". I.e.,
     * the exception will be wrapped in a EJBException.
     */
    @Test
    public void test_remote_uncheckedSystemException_declared() {
        try {
            remote.uncheckedSystemException_declared();
            fail("No exception thrown.");
        }
        catch (EJBException e) {
            assertEquals(EJBException.class, e.getClass());
            
            Throwable cause = e.getCause();
            assertEquals(RuntimeException.class, cause.getClass());
            assertEquals("123456", cause.getMessage());
        }
    }
    
    /**
     * GlassFish 4.1 and WildFly 9.0.0.CR1 both throw:
     * <pre>
     *   com.martinandersson.javaee.ejb.exceptions.CustomApplicationException: 123456
     * </pre>
     */
    @Test
    public void test_remote_uncheckedApplicationException() {
        try {
            remote.uncheckedApplicationException();
            fail("No exception thrown.");
        }
        catch (CustomApplicationException e) {
            assertEquals("123456", e.getMessage());
        }
    }
    
    /**
     * GlassFish 4.1 and WildFly 9.0.0.CR1 both throw:
     * <pre>
     *   java.lang.Exception: 123456
     * </pre>
     */
    @Test
    public void test_remote_checkedException() {
        try {
            remote.checkedException();
            fail("No exception thrown.");
        }
        catch (Exception e) {
            assertEquals(Exception.class, e.getClass());
            assertEquals("123456", e.getMessage());
        }
    }
}