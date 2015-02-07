package com.martinandersson.javaee.cdi.transactional;

import com.martinandersson.javaee.utils.DeploymentBuilder;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.enterprise.inject.spi.CDI;
import javax.transaction.NotSupportedException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.After;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

/**
 * This test has been put together as a comment and further elaboration of a
 * GlassFish bug filed here:<p>
 * 
 * https://java.net/jira/browse/GLASSFISH-21172<p>
 * 
 * 
 * 
 * <h2>The problem</h2>
 * 
 * <strong>If a {@code @Transactional} CDI bean fail, which exception can the
 * client expect to see?</strong><p>
 * 
 * There are a couple of variants of the target bean and of the transactional
 * context active when the bean is executed:
 * 
 * <ol>
 *   <li>The bean might throw a checked or unchecked exception. If the bean
 *       throw a unchecked exception, then the exception can optionally be
 *       present in the bean's throws clause.</li><br>
 * 
 *   <li>The caller invoking the transactional CDI bean can be using a
 *       transaction inherited by the target bean.</li>
 * </ol><p>
 * 
 * 
 * 
 * <h2>Actual results</h2>
 * 
 * <h3>Transaction rollback</h3>
 * 
 * Here, WildFly 8.1.0 and GlassFish 4.1 behave precisely the same.<p>
 * 
 * If the exception thrown is an unchecked exception, then the transaction is
 * <strong>always</strong> set to rollback. This result is independent of
 * whether or not the exception is listed in the bean's throws clause and
 * independent of whether or not the bean inherited a transaction from his
 * caller.<p>
 * 
 * If the exception thrown is a checked exception, then the transaction is
 * <strong>never</strong> set to rollback independent of transaction
 * inheritance.<p>
 * 
 * This behavior comply with the JTA specification which we return to
 * shortly.<p>
 * 
 * 
 * 
 * <h3>Exception type thrown to client</h3>
 * 
 * Here is where the servers differ from each other given one tiny environment
 * variable. Let's begin with how WildFly behaves, because that is so freakin
 * easy.<p>
 * 
 * WildFly always throw the exception thrown by the bean - no strings attached.
 * In fact, WildFly won't even log anything about it. This result is independent
 * of exception type (checked/unchecked) and transaction inheritance.<p>
 * 
 * Same goes for GlassFish if the exception is checked or if the exception is
 * unchecked and the transaction is inherited. <strong>But</strong> if the
 * exception is unchecked and the transaction is not inherited, then all hell
 * break loose. This is demonstrated by the first two test cases you find in
 * this test class.<p>
 * 
 * So what do GlassFish do if the bean start a transaction and then throw an
 * unchecked exception? GlassFish will throw a {@code
 * javax.transaction.TransactionalException} that is caused by a {@code
 * javax.transaction.RollbackException}. No traces of the original unchecked
 * exception can be found. Not in the log, and unfortunately not in the
 * exception cause-chain either.<p>
 * 
 * Is it really a problem? For starters, not having the ability to track the
 * real cause make application handling impossible - so yes. But also, {@code
 * TransactionalException} is only supposed to be thrown if the client make the
 * call using an illegal transaction context and thus "violate" the bean's
 * transaction requirements ({@code TxType.MANDATORY} or
 * {@code TxType.NEVER})<sup>1</sup>. Such a violation has not happened in this
 * test class so the thrown exception type is erroneous.<p>
 * 
 * What about that other exception, RollbackException? JavaDoc of
 * RollbackException says:
 * <pre>{@code
 * 
 *     This is a local exception thrown by methods in the UserTransaction,
 *     Transaction, and TransactionManager interfaces.
 * 
 * }</pre>
 * 
 * My gut feeling is that a RollbackException is not what the client expect,
 * because one can not assume that the client is the one handling the
 * transaction demarcation. If he do or otherwise has an interest in knowing if
 * the transaction rolled back, then the client can simply catch a {@code
 * RuntimeException} because all those rollback the exception.<p>
 * 
 * Next, I'll dig through the specifications a bit to see what they have to say
 * before reaching an almighty final verdict.<p>
 * 
 * 
 * 
 * <h2>Specifications</h2>
 * 
 * <h3>CDI 1.1 specification (JSR 346)</h3>
 * 
 * This specification doesn't really say anything about {@code @Transactional}
 * or exceptions thrown by container services. It do say that (section "2.9
 * Problems detected automatically by the container"):
 * 
 * <pre>{@code
 * 
 *     All exceptions defined by this specification may be safely caught and
 *     handled by the application.
 * 
 * }</pre>
 * 
 * ..meaning that the intent of the exception architecture is to provide a way
 * for applications to handle them.<p>
 * 
 * 
 * 
 * <h3>JTA 1.2 (JSR 907)</h3>
 * 
 * JTA define the {@code @Transactional} annotation in section "3.7
 * Transactional Annotation". This section says:
 * 
 * <pre>{@code
 * 
 *     By default checked exceptions do not result in the transactional
 *     interceptor marking the transaction for rollback and instances of
 *     RuntimeException and its subclasses do.
 * 
 * }</pre>
 * 
 * This is exactly how both WildFly and GlassFish behave so no surprise
 * there.<p>
 * 
 * The semantics provided with this design is on par with how EJB 3.2 (JSR 345)
 * define "application exceptions". Unfortunately for us, it doesn't say what
 * exception the client may except on transaction failure.<p>
 * 
 * An analogy can be made from section "3.4.7 Local and Global Transactions". In
 * this section, it is said that if the "resource adapter" (i.e, a JDBC driver)
 * throw a {@code java.sql.SQLException}, then that is what is thrown to the
 * client.<p>
 * 
 * {@code @Transactional} support "is provided via an implementation of CDI
 * interceptors" (section 3.7).<p>
 * 
 * 
 * 
 * <h3>Interceptors 1.2 (JSR 318)</h3>
 * 
 * Interceptors 1.2 (JSR-318), section 2.4 Exceptions:
 * <pre>{@code
 * 
 *     Interceptor methods are allowed to throw runtime exceptions or any
 *     checked exceptions that the associated target method allows within its
 *     throws clause.
 * 
 * }</pre>
 * 
 * Negating the previous quote give us that the interceptor method is not
 * allowed to throw <i>anything else</i> than what the target allows within its
 * throws clause. Of course, that sounds crazy too given that many target
 * methods might not have a throws clause. If so, is the interceptor method not
 * allowed to throw anything at all in the event of a failure? Who knows.<p>
 * 
 * The specification continue:
 * 
 * <pre>{@code
 * 
 *     Interceptor methods are allowed to catch and suppress exceptions and to
 *     recover by calling the InvocationContext.proceed method.
 * 
 * }</pre>
 * 
 * My interpretation of what has just been quoted is that the specification says
 * an interceptor is allowed to suppress exceptions <strong>only</strong> if
 * that is a necessity for recovery.<p>
 * 
 * The specification goes on:
 * <pre>{@code
 * 
 *     The invocation of the InvocationContext.proceed method will throw the
 *     same exception as any thrown by the associated target method unless an
 *     interceptor further down the Java call stack has caught it and thrown a
 *     different exception.
 * 
 * }</pre>
 * 
 * So InvocationContext.proceed throw the original exception if not another
 * interceptor has thrown a "different exception". Clearly,
 * InvocationContext.proceed make a best effort to not let clients be surprised
 * by unknown exception types. But, the quote admits there is a possibility
 * another interceptor has already shredded that intent to pieces. Speaking of
 * this possibility is not the same as saying the specification call such a
 * behavior legit. But without an explicit condemnent, one is left to think
 * that the specification do legitimize the practice of throwing other exception
 * types.<p>
 * 
 * And that is all.<p>
 * 
 * 
 * 
 * 
 * <h2>Verdict</h2>
 * 
 * All specifications are mostly concerned with trouble-free situations and all
 * lack details when it comes to exceptions. All specifications seem to share
 * the goal that exceptions is something that the client should be able to
 * handle and recover from.<p>
 * 
 * EJB 3.2 is in this context the most mature specification. EJB define
 * "application exceptions" that may or may not mark a transaction for rollback.
 * Application exceptions are all checked exceptions or unchecked exceptions
 * annotated {@code @ApplicationException}. These are thrown to the client
 * as-is and by default they do not rollback the transaction nor do application
 * exceptions invalidate the bean instance which is otherwise the case (except
 * for {@code @Singleton}).<p>
 * 
 * If the exception is not an application exception, then it is a "system-level
 * exception" and all the client may see is a {@code EJBException}. The
 * specification do not require the container to set the cause except for
 * {@code @MessageDriven} beans<sup>2</sup>.<p>
 * 
 * But, there's a twist to the tale. EJB 3.2 say that if the caller was in a
 * transaction context that the target EJB inherited, then the container must
 * not throw a {@code EJBException}. Instead, the container must throw a
 * {@code EJBTransactionRolledbackException}<sup>3</sup>. Why? Because the
 * container want the client to know that continuing the transaction is
 * "fruitless".<p>
 * 
 * One could argue - having the EJB specification in mind - that the CDI
 * container managing transactional CDI beans ought to throw some kind of
 * "poison" exception type to the client, letting him know that continuing the
 * transaction is fruitless. I find that adding such poison pills to the mix
 * only increase complexity. The client has a myriad of ways to find out whether
 * or not the transaction has been set to rollback. In case of
 * {@code @Transactional}, all unchecked exceptions rollback the transaction so
 * that's pritty obvious and easy to catch.<p>
 * 
 * GlassFish has erased all traces of the original cause, and that is not a good
 * thing. Also, GlassFish can not possibly think that throwing any of the
 * exceptions the client currently see, provide the right kind of semantics.
 * According to the specifications and the exception's JavaDoc, they don't.
 * Furthermore, GlassFish throw these exceptions only when the transaction was
 * <strong>not</strong> inherited.<p>
 * 
 * Hence, I think GlassFish should behave just like GlassFish otherwise do and
 * just like WildFly always do: throw the original cause. Removing the trace of
 * the cause and throwing the types currently thrown is hard to motivate.<p>
 * 
 * 
 * 
 * <h4>Note 1</h4>
 * JTA 1.2, section "3.7 Transactional Annotation".
 * 
 * <h4>Note 2</h4>
 * EJB 3.2, section "9.2.2 System Exceptions".
 * 
 * <h4>Note 3</h4>
 * See EJB 3.2, table 7 in section "9.3.1 Exceptions from
 * a Session Bean's Business Interface Methods and No-Interface View Methods".
 * Also see {@linkplain
 * com.martinandersson.javaee.ejb.exceptions.EJBTransactionRolledbackExceptionTest}.
 * 
 * 
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
public class ExceptionCauseTest
{
    private static final Logger LOGGER = Logger.getLogger(ExceptionCauseTest.class.getName());
    
    @Deployment
    private static Archive<?> buildDeployment() {
        return new DeploymentBuilder(ExceptionCauseTest.class)
                .addEmptyBeansXMLFile()
                .add(CrashingBean.class)
                .build();
    }
    
    @Rule
    public TestName name = new TestName();
    
    @Before
    public void __lookup() {
        testee = CDI.current().select(CrashingBean.class).get();
    }
    
    @After
    public void __destroy() {
        if (testee != null) {
            CDI.current().destroy(testee);
            testee = null;
        }
    }
    
    
    
    @Resource
    UserTransaction tx;
    
    @Resource(lookup = "java:comp/TransactionSynchronizationRegistry")
    TransactionSynchronizationRegistry reg;
    
    CrashingBean testee;
    
    
    
    /*
     *  -------
     * | TESTS |
     *  -------
     */
    
    
    
    /*
     * TODO: Current "conclusion" is that inheriting a transaction from client
     *       code is the solution for GlassFish. I think the problem really is
     *       whenever GlassFish must start a new transaction in combination of
     *       the bean throwing a RuntimeException. Thus, a test needs to be
     *       added that do invoke the bean from a transactional context, but the
     *       bean method has transactional attribute TxType.REQUIRES_NEW.
     */
    
    
    
    /**
     * Make target bean start a transaction and then throw a
     * NullPointerException from a method that doesn't have a throws clause.<p>
     * 
     * WildFly: Client see java.lang.NullPointerException.<p>
     * 
     * GlassFish: Client see javax.transaction.TransactionalException caused by
     * a javax.transaction.RollbackException that in turn has no cause. The
     * thrown NPE is completely lost; not even logged.<p>
     * 
     * Both servers rollback the transaction.
     */
    @Test(expected = NullPointerException.class)
    public void throwNPE_txNotInherited_noThrowsClause() {
        try {
            testee.throwNPE_noThrowsClause();
        }
        catch (RuntimeException e) {
            assertTransaction(true, false);
            logAndThrow(e);
        }
    }
    
    /**
     * Make target bean start a transaction and then throw a
     * NullPointerException from a method that declares the exception in its
     * throws clause.<p>
     * 
     * WildFly: Client see java.lang.NullPointerException.<p>
     * 
     * GlassFish: Client see javax.transaction.TransactionalException caused by
     * a javax.transaction.RollbackException that in turn has no cause. The
     * thrown NPE is completely lost; not even logged.<p>
     * 
     * Both servers rollback the transaction.
     */
    @Test(expected = NullPointerException.class)
    public void throwNPE_txNotInherited_hasThrowsClause() {
        try {
            testee.throwNPE_hasThrowsClause();
        }
        catch (RuntimeException e) {
            assertTransaction(true, false);
            logAndThrow(e);
        }
    }
    
    /**
     * Make target bean inherit a transaction from client and then throw a
     * NullPointerException from a method that doesn't have a throws clause.<p>
     * 
     * Both servers throw NullPointerException and rollback the transaction.
     */
    @Test(expected = NullPointerException.class)
    public void throwNPE_txInherited_noThrowsClause() {
        startTransaction();
        
        try {
            testee.throwNPE_noThrowsClause(reg.getTransactionKey());
        }
        catch (RuntimeException e) {
            assertTransaction(true, true);
            logAndThrow(e);
        }
        finally {
            closeTransaction();
        }
    }
    
    /**
     * Make target bean inherit a transaction from client and then throw a
     * NullPointerException from a method that declares the exception in its
     * throws clause.
     * 
     * Both servers throw NullPointerException and rollback the transaction.
     */
    @Test(expected = NullPointerException.class)
    public void throwNPE_txInherited_hasThrowsClause() {
        startTransaction();
        
        try {
            testee.throwNPE_hasThrowsClause(reg.getTransactionKey());
        }
        catch (RuntimeException e) {
            assertTransaction(true, true);
            logAndThrow(e);
        }
        finally {
            closeTransaction();
        }
    }
    
    /**
     * Make target bean start a transaction and then throw an IOException.<p>
     * 
     * Both servers throw IOException and does not rollback the transaction.
     * 
     * @throws Exception should be IOException
     */
    @Test(expected = IOException.class)
    public void throwIOException_txNotInherited() throws Exception {
        try {
            testee.throwIOException();
        }
        catch (Exception e) {
            assertTransaction(false, false);
            logAndThrow(e);
        }
    }
    
    /**
     * Make target bean inherit a transaction from client and then throw an
     * IOException.<p>
     * 
     * Both servers throw IOException and does not rollback the transaction.
     * 
     * @throws Exception should be IOException
     */
    @Test(expected = IOException.class)
    public void throwIOException_txInherited() throws Exception {
        startTransaction();
        try {
            testee.throwIOException(reg.getTransactionKey());
        }
        catch (Exception e) {
            assertTransaction(false, true);
            logAndThrow(e);
        }
        finally {
            closeTransaction();
        }
    }
    
    
    
    /*
     *  --------------
     * | INTERNAL API |
     *  --------------
     */
    
    private void assertTransaction(boolean expectRollback, boolean inherited) {
        if (inherited) {
            try {
                int expected = expectRollback ?
                        Status.STATUS_MARKED_ROLLBACK :
                        Status.STATUS_ACTIVE;
                
                assertSame(expected, tx.getStatus()); // or: reg.getRollbackOnly()
            } catch (SystemException e) {
                fail("Can not query status of transaction.");
            }
        }
        else {
            assertSame(expectRollback, CrashingBean.transactionRolledBack());
        }
    }
    
    private <E extends Exception> void logAndThrow(E exception) throws E {
        LOGGER.log(Level.WARNING, name.getMethodName() + "() caught: ", exception);
        throw exception;
    }
    
    private void startTransaction() {
        try {
            tx.begin();
        }
        catch (NotSupportedException | SystemException e) {
            fail("Test is unrunnable, failed to start transaction.");
        }
    }
    
    private void closeTransaction() {
        try {
            if (tx.getStatus() != Status.STATUS_MARKED_ROLLBACK)
                tx.rollback();
        }
        catch (SystemException | IllegalStateException | SecurityException e) {
            // Don't bother
        }
    }
}