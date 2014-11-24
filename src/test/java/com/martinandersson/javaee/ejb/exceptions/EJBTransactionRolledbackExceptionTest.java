package com.martinandersson.javaee.ejb.exceptions;

import com.martinandersson.javaee.utils.Deployments;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.EJBTransactionRolledbackException;
import javax.ejb.TransactionRolledbackLocalException;
import javax.transaction.Status;
import javax.transaction.UserTransaction;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
 * crashed) inherited the transaction from his client, a transaction that has
 * been marked to roll back.<p>
 * 
 * Of course {@code EJBTransactionRolledbackException} is a specialized version
 * of {@code EJBException}, so it wouldn't be wrong for the container to throw
 * {@code EJBTransactionRolledbackException} even if the bean did not inherit
 * the transaction used.<p>
 * 
 * 
 * 
 * <h3>Results</h3>
 * 
 * GlassFish and WildFly both throw {@code EJBTransactionRolledbackException}
 * only if the transaction was inherited. However, their cause-hierarchy
 * differs. See
 * {@linkplain #targetInheritTransaction() targetInheritTransaction()}.
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
     * Uses no transaction at all.
     */
    @Test
    public void noTransaction() {
        try {
            bean.crash_neverTx();
            fail("Bean was supposed to throw something!");
        }
        catch (EJBException e) {
            // Note that we don't get a subtype of EJBException!
            assertEquals(EJBException.class, e.getClass());
            
            // Original error set as cause:
            assertEquals(ArithmeticException.class, e.getCause().getClass());
        }
    }
    
    /**
     * Target bean start a new transaction.
     */
    @Test
    public void targetStartTransaction() {
        try {
            bean.crash_requiresNewTx();
            fail("Bean was supposed to throw something!");
        }
        catch (EJBException e) {
            assertEquals(EJBException.class, e.getClass());
            assertEquals(ArithmeticException.class, e.getCause().getClass());
        }
    }
    
    /**
     * Target bean inherit the client's transaction.
     * 
     * @throws Exception on problem with beginning the transaction or query for
     *         its status
     */
    @Test
    public void targetInheritTransaction() throws Exception {
        tx.begin();
        
        try {
            bean.crash_mandatoryTx();
            fail("Bean was supposed to throw something!");
        }
        catch (EJBTransactionRolledbackException e) {
            assertEquals(EJBTransactionRolledbackException.class, e.getClass());
            
            Throwable cause = e.getCause();
            assertNotNull(cause);
            
            /*
             * And hereafter, GlassFish and WildFly differ from each other.
             * 
             * WildFly's cause is ArithmeticException, but GlassFish has added a
             * layer inbetween, namely a TransactionRolledbackLocalException.
             * I personally prefer the WildFly version, but specification doesn't
             * say so both servers shall pass.
             */
            
            if (cause.getClass() == TransactionRolledbackLocalException.class) {
                // GlassFish:
                assertEquals(ArithmeticException.class, cause.getCause().getClass());
            }
            else {
                // WildFly:
                assertEquals(ArithmeticException.class, cause.getClass());
            }
        }
        finally {
            assertEquals(Status.STATUS_MARKED_ROLLBACK, tx.getStatus());
        }
    }
}