package com.martinandersson.javaee.ejb.exceptions;

import com.martinandersson.javaee.utils.Deployments;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.EJBTransactionRolledbackException;
import javax.ejb.TransactionRolledbackLocalException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.UserTransaction;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * When does the client receive a {@code EJBTransactionRolledbackException}?<p>
 * 
 * Looking at the JavaDoc won't reveal much.<p>
 * 
 * Table 7 in EJB 3.2 section "9.3.1 Exceptions from a Session Bean's Business
 * Interface Methods and No-Interface View Methods" says that
 * {@code EJBTransactionRolledbackException} is thrown if the target bean (that
 * crashed) inherited the transaction from his client, a transaction that the
 * bean marked to roll back.<p>
 * 
 * {@code EJBTransactionRolledbackException} is a specialized version of {@code
 * EJBException}, so it isn't wrong for the container to throw
 * {@code EJBTransactionRolledbackException} even if the bean did not inherit
 * the transaction used (see
 * {@linkplain #targetStartTransaction_txCommitCrash() targetStartTransaction_txCommitCrash()}).<p>
 * 
 * 
 * 
 * <h3>Results</h3>
 * 
 * GlassFish and WildFly behave coherently enough as to not break the
 * specification. If the target bean inherited the transaction, then sure
 * enough, both servers will throw a {@code
 * EJBTransactionRolledbackException}.<p>
 * 
 * Client code trying to find the real cause behind the problem must always dig
 * through all causes in the exception hierarchy until he find the furthermost
 * cause set. That will always be an {@code ArithmeticException} in this case.
 * Please see the individual test methods and comments therein.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
public class EJBTransactionRolledbackExceptionTest
{
    @Deployment
    private static Archive<?> buildDeployment() {
        return Deployments.buildWARWithPackageFriends(EJBTransactionRolledbackExceptionTest.class);
    }
    
    @EJB
    CrashingBean bean;
    
    @Resource
    UserTransaction tx;
    
    /**
     * Uses no transaction at all.<p>
     * 
     * Both GlassFish and WildFly throw a {@code EJBException} and not a
     * subclass thereof.
     */
    @Test
    public void noTransaction() {
        try {
            bean.crash_neverTx();
            fail("Bean was supposed to throw something!");
        }
        catch (EJBException e) {
            // Note that we don't get a subtype of EJBException!
            assertSame(EJBException.class, e.getClass());
            
            // Original error set as cause:
            assertSame(ArithmeticException.class, e.getCause().getClass());
        }
    }
    
    /**
     * Target bean start a new transaction but crash immediately.<p>
     * 
     * Both GlassFish and WildFly throw a {@code EJBException} and not a
     * subclass thereof. The cause is set properly.
     */
    @Test
    public void targetStartTransaction_beanCrash() {
        try {
            bean.crash_requiresNewTx(false);
            fail("Bean was supposed to throw something!");
        }
        catch (EJBException e) {
            /*
            
            GF: 
            
            javax.ejb.EJBException
              Caused by: java.lang.ArithmeticException: / by zero
            
            WF: 
            
            javax.ejb.EJBException: java.lang.ArithmeticException: / by zero
              Caused by: java.lang.ArithmeticException: / by zero
            
            */
            
            assertEquals(EJBException.class, e.getClass());
            assertEquals(ArithmeticException.class, e.getCause().getClass());
        }
    }
    
    /**
     * Target bean start a new transaction that will crash during commit.<p>
     * 
     * GlassFish stick to throwing a {@code EJBException} and not a subclass
     * thereof. WildFly will throw a {@code EJBTransactionRolledbackException}.<p>
     * 
     * In both cases, client code must dig to the root cause in order to find
     * the {@code ArithmeticException}.<p>
     * 
     * Note that it isn't wrong for WildFly to throw the specialized {@code
     * EJBTransactionRolledbackException} even though the client didn't start
     * the transaction.
     */
    @Test
    public void targetStartTransaction_txCommitCrash() {
        try {
            bean.crash_requiresNewTx(true);
            fail("Bean was supposed to throw something!");
        }
        catch (EJBException e) {
            /*
            
            GF:
            
            javax.ejb.EJBException: Transaction aborted
              Caused by: javax.transaction.RollbackException: Transaction marked for rollback.
              Caused by: java.lang.ArithmeticException: / by zero
            
            WF:
            
            javax.ejb.EJBTransactionRolledbackException: Transaction rolled back
              Caused by: javax.transaction.RollbackException: ARJUNA016053: Could not commit transaction.
              Caused by: java.lang.ArithmeticException: / by zero
            
            */
            
            assertSame(RollbackException.class, e.getCause().getClass());
            assertSame(ArithmeticException.class, e.getCause().getCause().getClass());
        }
    }
    
    /**
     * Target bean inherit the client's transaction and then crash
     * immediately.<p>
     * 
     * Because the transaction was started by client, the container must throw
     * a {@code EJBTransactionRolledbackException} which both servers do.
     * However, their cause stack differs and client code can only rely on
     * finding the root cause if he dig through the entire hierarchy.
     * 
     * @throws Exception on problem with beginning the transaction or query for
     *         its status
     */
    @Test
    public void targetInheritTransaction_beanCrash() throws Exception {
        tx.begin();
        
        try {
            bean.crash_mandatoryTx(false);
            fail("Bean was supposed to throw something!");
        }
        catch (EJBTransactionRolledbackException e) {
            /*
            
            GF:
            
            javax.ejb.EJBTransactionRolledbackException
              Caused by: javax.ejb.TransactionRolledbackLocalException: Exception thrown from bean
              Caused by: java.lang.ArithmeticException: / by zero
            
            WF:
            
            javax.ejb.EJBTransactionRolledbackException: / by zero
              Caused by: java.lang.ArithmeticException: / by zero
            
            */
            
            assertSame(EJBTransactionRolledbackException.class, e.getClass());
            
            Throwable cause = e.getCause();
            assertNotNull(cause);
            
            /*
             * Hereafter, GlassFish and WildFly differ from each other.
             * 
             * WildFly's cause is ArithmeticException, but GlassFish has added a
             * layer inbetween, namely a TransactionRolledbackLocalException.
             * I personally prefer the WildFly version, but specification
             * doesn't say so both servers shall pass.
             */
            
            if (cause.getClass() == TransactionRolledbackLocalException.class) {
                // GlassFish:
                assertSame(ArithmeticException.class, cause.getCause().getClass());
            }
            else {
                // WildFly:
                assertSame(ArithmeticException.class, cause.getClass());
            }
        }
        finally {
            assertEquals(Status.STATUS_MARKED_ROLLBACK, tx.getStatus());
            tx.rollback();
        }
    }
    
    /**
     * Target bean inherit the client's transaction but defer the crash until
     * transaction commit.<p>
     * 
     * Indeed the transaction is started by client, but the crash won't happen
     * on the bean side this time. Hence the client will discover a {@code
     * RollbackException} once he himself commit the transaction.
     * 
     * @throws Exception on problem with beginning the transaction or query for
     *         its status
     */
    @Test
    public void targetInheritTransaction_txCommitCrash() throws Exception {
        tx.begin();
        
        try {
            bean.crash_mandatoryTx(true);
            tx.commit(); // <-- this is the statement that cause an exception to be thrown!
            fail("Bean was supposed to throw something!");
        }
        catch (RollbackException e) {
            /*
            
            GF:
            
            javax.transaction.RollbackException: Transaction marked for rollback.
              Caused by: java.lang.ArithmeticException: / by zero
            
            WF:
            
            javax.transaction.RollbackException: ARJUNA016053: Could not commit transaction.
              Caused by: java.lang.ArithmeticException: / by zero
            
            */
            
            assertSame(RollbackException.class, e.getClass());
            assertSame(ArithmeticException.class, e.getCause().getClass());
        }
        finally {
            assertEquals(Status.STATUS_NO_TRANSACTION, tx.getStatus());
        }
    }
}