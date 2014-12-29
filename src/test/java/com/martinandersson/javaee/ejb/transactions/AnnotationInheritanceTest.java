package com.martinandersson.javaee.ejb.transactions;

import com.martinandersson.javaee.utils.Deployments;
import javax.ejb.EJB;
import javax.transaction.Status;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * See Common Annotations for the Java Platform 1.2, JSR-250, section "2.1
 * General Guidelines for Inheritance of Annotations":
 * <pre>{@code
 * 
 *     Member-level annotations on a hidden or overridden member are always
 *     ignored.
 * }</pre>
 *
 * This guideline is exemplified in table 2-1 where "foo() in Derived1" has
 * effective transaction attribute value "REQUIRED". First test case in this
 * class is a replica of the aforementioned specification-provided example.<p>
 * 
 * Overridden members "are always ignored" is not contradicted by the EJB
 * specification. EJB 3.2, section "4.9.2 Session Bean Class" and "4.9.2.1
 * Session Bean Superclasses":
 * <pre>{@code
 * 
 *     The session bean class may have superclasses and/or superinterfaces.
 *     
 *     [..]
 *     
 *     For the purposes of processing a particular session bean class, all
 *     superclass processing is identical regardless of whether the superclasses
 *     are themselves session bean classes. In this regard, the use of session
 *     bean classes as superclasses merely represents a convenient use of
 *     implementation inheritance, but does not have component inheritance
 *     semantics.
 *     
 *     For example, the client views exposed by a particular session bean are
 *     not inherited by a subclass that also happens to define a session bean.
 * }</pre>
 * 
 * Also see EJB 3.2, section "8.3.7.1 Specification of Transaction Attributes
 * with Metadata Annotations":
 * <pre>{@code
 * 
 *     The transaction attributes for the methods of a bean class may be
 *     specified on the class, the business methods of the class, or both.
 * }</pre>
 * 
 * Negating this quote implies that supertype annotations are "always ignored".
 * The section continues:
 * <pre>{@code
 * 
 *     A transaction attribute specified on a superclass S applies to the
 *     business methods defined by S.
 *     
 *     [..]
 *     
 *     If a method M of class S overrides a business method defined by a
 *     superclass of S, the transaction attribute of M is determined by the
 *     above rules as applied to class S.
 * }</pre>
 * 
 * The quotes are a bit way too complex. But my understanding, and what perhaps
 * is a needed simplification is the following: All annotations put on a
 * supertype apply, as long as the subclass does not redeclare the method. If
 * so, all annotations on the supertype is ignored.<p>
 * 
 * 
 * 
 * <h3>Results</h3>
 * 
 * WildFly 8.2.0 pass all tests. GlassFish 4.1 fail bean-test 3, 4 and 7. Bug
 * filed here:
 * <pre>{@code
 * 
 *     https://java.net/jira/browse/GLASSFISH-21280
 * }</pre>
 * 
 * 
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
public class AnnotationInheritanceTest
{
    @Deployment
    private static Archive<?> buildDeployment() {
        return Deployments.buildWARWithPackageFriends(AnnotationInheritanceTest.class);
    }
    
    
    
    @EJB
    Derived1 bean1;
    
    @EJB
    Derived2 bean2;
    
    @EJB
    Derived3 bean3;
    
    @EJB
    Derived4 bean4;
    
    @EJB
    Derived5 bean5;
    
    @EJB
    Derived6 bean6;
    
    @EJB
    Derived7 bean7;
    
    
    
    /**
     * Clone of example provided in JSR-250, see {@linkplain
     * AnnotationInheritanceTest}.<p>
     * 
     * Base class: @TransactionAttribute(REQUIRED)<br>
     * Base foo: @TransactionAttribute(NEVER)<p>
     * 
     * Derived class: nothing<br>
     * Derived foo: nothing<p>
     * 
     * GlassFish 4.1: pass<br>
     * WildFly 8.2.0: pass
     */
    @Test
    public void bean1() {
        assertEquals("NEVER in base should be ignored and default REQUIRED in derived should be applied,",
                Status.STATUS_ACTIVE, bean1.foo());
    }
    
    /**
     * The special thing about this test is that {@code foo()} originate from a
     * non-generic interface.<p>
     * 
     * Base class: @TransactionAttribute(SUPPORTS)<br>
     * Base foo: @TransactionAttribute(MANDATORY)<p>
     * 
     * Derived class: @TransactionAttribute(SUPPORTS)<br>
     * Derived foo: @TransactionAttribute(REQUIRED)<p>
     * 
     * GlassFish 4.1: pass<br>
     * WildFly 8.2.0: pass
     */
    @Test
    public void bean2() {
        assertEquals("MANDATORY in base should be honoured because derived has REQUIRED,",
                Status.STATUS_ACTIVE, bean2.foo());
    }
    
    
    /**
     * Let's make the interface generic with a type parameter. What happens
     * then? Turns out GlassFish stop bother about the annotations put on the
     * derived class, THEY are ignored. Instead, annotations put on the base
     * class goes into effect.<p>
     * 
     * Base class: @TransactionAttribute(SUPPORTS)<br>
     * Base foo: @TransactionAttribute(MANDATORY)<p>
     * 
     * Derived class: @TransactionAttribute(SUPPORTS)<br>
     * Derived foo: @TransactionAttribute(REQUIRED)<p>
     * 
     * GF: javax.ejb.EJBTransactionRequiredException, caused by javax.ejb.TransactionRequiredLocalException<br>
     * WF: pass
     */
    @Test
    public void bean3() {
        assertEquals("MANDATORY in base should implicitly be honoured because derived has REQUIRED,",
                Status.STATUS_ACTIVE, bean3.foo(/* ignored: */ 123));
    }
    
    /**glass
     * As the ultimate proof of the last declared test case, let's make the base
     * class not support transactions at all. GlassFish: call goes through and
     * return Status.STATUS_NO_TRANSACTION.<p>
     * 
     * Base class: @TransactionAttribute(NEVER)<br>
     * Base foo: @TransactionAttribute(NEVER)<p>
     * 
     * Derived class: @TransactionAttribute(SUPPORTS)<br>
     * Derived foo: @TransactionAttribute(REQUIRED)<p>
     * 
     * GF: Assertion error: bean4.foo() return Status.STATUS_NO_TRANSACTION
     * WF: pass
     */
    @Test
    public void bean4() {
        assertEquals("NEVER in base should be ignored because derived has REQUIRED,",
                Status.STATUS_ACTIVE, bean4.foo(/* ignored: */ 123));
    }
    
    /**
     * Previously, we had the type parameter put on the interface type. What if
     * only method foo() is generic? Turns out this fix GlassFish's problems.<p>
     * 
     * Base class: @TransactionAttribute(SUPPORTS)<br>
     * Base foo: @TransactionAttribute(MANDATORY)<p>
     * 
     * Derived class: @TransactionAttribute(SUPPORTS)<br>
     * Derived foo: @TransactionAttribute(REQUIRED)<p>
     * 
     * GF: pass<br>
     * WF: pass
     */
    @Test
    public void bean5() {
        assertEquals("MANDATORY in base should implicitly be honoured because derived has REQUIRED,",
                Status.STATUS_ACTIVE, bean5.foo(/* ignored: */ 123));
    }
    
    /**
     * Going back to a parameterized interface, would providing the type Object
     * instead of Integer solve GlassFish's problems? Yes!<p>
     * 
     * Base class: @TransactionAttribute(SUPPORTS)<br>
     * Base foo: @TransactionAttribute(MANDATORY)<p>
     * 
     * Derived class: @TransactionAttribute(SUPPORTS)<br>
     * Derived foo: @TransactionAttribute(REQUIRED)<p>
     * 
     * GF: pass<br>
     * WF: pass
     */
    @Test
    public void bean6() {
        assertEquals("MANDATORY in base should implicitly be honoured because derived has REQUIRED,",
                Status.STATUS_ACTIVE, bean6.foo(/* ignored: */ new Object()));
    }
    
    /**
     * Do GlassFish still have problems if it is only the superclass that is
     * generic? Yes.<p>
     * 
     * Base class: @TransactionAttribute(SUPPORTS)<br>
     * Base foo: @TransactionAttribute(MANDATORY)<p>
     * 
     * Derived class: @TransactionAttribute(SUPPORTS)<br>
     * Derived foo: @TransactionAttribute(REQUIRED)<p>
     * 
     * GF: javax.ejb.EJBTransactionRequiredException, caused by javax.ejb.TransactionRequiredLocalException<br>
     * WF: pass
     */
    @Test
    public void bean7() {
        assertEquals("MANDATORY in base should implicitly be honoured because derived has REQUIRED,",
                Status.STATUS_ACTIVE, bean7.foo(/* ignored: */ 123));
    }
}