package com.martinandersson.javaee.ejb.inheritance;

import com.martinandersson.javaee.utils.DeploymentBuilder;
import com.martinandersson.javaee.utils.Lookup;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.enterprise.inject.spi.CDI;
import javax.naming.NamingException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This test will examine what happens when you extend an EJB, and what happens
 * when you go one step further and {@code @Specializes} one.<p>
 * 
 * For all the details, see the individual test cases. Here's a summary:<p>
 * 
 * If EJB B extends EJB A, both are available and ready to be used by means of
 * the ordinary {@code @EJB} injection mechanism. However, using {@code @Inject}
 * to lookup A will cause ambiguity (crash). This result is the same for
 * GlassFish 4.1 and WildFly 9.0.0.CR1.<p>
 * 
 * The result of the following case is independent of whether @EJB or @Inject is
 * used.<p>
 * 
 * If EJB C extends EJB A, but C also declare the CDI annotation
 * {@code @Specializes}, then C can be looked up. But trying to lookup A will
 * make GlassFish crash (something inside GlassFish has understood that A is
 * supposed to be disabled) while WildFly route the call to an instance of A
 * (ignoring that it was "specialized" by B).<p>
 * 
 * The lessons to be learned is 1) when you write EJB injection points, stick
 * to @EJB. If you use @Inject, then new subclass additions in the future will
 * brake your code. 2) Do not declare {@code @Specializes} on an EJB. It is not
 * possible to properly specialize an EJB bean in GlassFish nor in WildFly.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
public class InheritanceAndSpecializesTest
{
    private static final Logger LOGGER =
            Logger.getLogger(InheritanceAndSpecializesTest.class.getName());
    
    @Deployment(name = "-without-c", order = 1)
    private static Archive<?> buildDeployment1() {
        return new DeploymentBuilder(InheritanceAndSpecializesTest.class, "-without-c")
                .add(A.class,
                     B.class)
                .build();
    }
    
    @Deployment(name = "-with-c", order = 2)
    private static Archive<?> buildDeployment2() {
        return new DeploymentBuilder(InheritanceAndSpecializesTest.class, "-with-c")
                .add(A.class,
                     B.class,
                     C.class,
                     Lookup.class)
                .build();
    }
    
    @EJB
    A a;
    
    @EJB
    B b;
    
    /**
     * This test demonstrates that if EJB B extends EJB A, both are available
     * and ready to be used by means of the ordinary {@code @EJB} injection
     * mechanism.<p>
     * 
     * "@EJB A a;" does not cause ambiguity. Nor does the subclass B cause
     * superclass A to be disabled.
     */
    @Test
    @OperateOnDeployment("-without-c")
    public void test_inheritance_lookupUsingEJB() {
        assertEquals("A", a.simpleName());
        assertEquals("B", b.simpleName());
    }
    
    /**
     * ..but if we use CDI ({@code @Inject}) to lookup B, then everything
     * changes.<p>
     * 
     * Both GlassFish 4.1 and WildFly 9.0.0.CR1 will throw
     * {@code javax.enterprise.inject.AmbiguousResolutionException} when trying
     * to inject A because there is two different bean classes that qualify for
     * bean type A.<p>
     * 
     * Using @Inject instead of @EJB can cause troubles in other scenarios too
     * and it is only necessarily to use CDI when you want to scope the life
     * cycle of a @Stateful EJB. In all other cases, there is no point in using
     * {@literal @}Inject when looking for EJB:s.<p>
     * 
     * Note that {@code CDI.current().select..} is equivalent to {@code @Inject}.
     */
    @Test
    @OperateOnDeployment("-without-c")
    public void test_inheritance_lookupUsingInject() {
        // B works:
        B b = CDI.current().select(B.class).get();
        assertEquals("B", b.simpleName());
        
        // A fails:
        try {
            A a = CDI.current().select(A.class).get();
            fail("Your application server is too good to be true.");
        }
        catch (Throwable t) {
            assertTrue(t instanceof javax.enterprise.inject.AmbiguousResolutionException);
        }
    }
    
    /**
     * What if the subclass B not only extends A, but also declare the CDI
     * annotation {@code @Specializes}?<p>
     * 
     * When specializing a CDI bean, the superclass will become disabled (in
     * theory at least<sup>1</sup>). Does the same hold true when we specialize
     * an EJB? Does it matter to the outcome which lookup method we use (@EJB
     * versus @Inject)?<p>
     * 
     * This test uses @EJB, but the result doesn't change if you use @Inject as
     * is proved by the next test case.<p>
     * 
     * The result is different between GlassFish 4.1 and WildFly 9.0.0.CR1.<p>
     * 
     * In GlassFish, class A is injected with a proxy. But if we try to
     * dereference that proxy, then Weld crash with a NPE deep down the
     * stack<sup>2</sup>.<p>
     * 
     * WildFly inject a proxy to A <strong>and</strong> let us dereference the
     * reference to an instance of A.<p>
     * 
     * It can therefore be said that GlassFish does to a certain extent what is
     * expected by @Specializes (turning A into a disabled bean<sup>3</sup>),
     * alas a bit too late. WildFly seem to reason that @Specializes is a CDI
     * construct and won't bother about disabling A. It is business as usual.<p>
     * 
     * None of the server did what was fully expected of the @Specializes
     * annotation: Disable A <i>and</i> route the call to an instance of B.<p>
     * 
     * As far as I can tell, it is not defined by the CDI- and EJB specification
     * what should happen when specializing EJB:s. Thus both behaviors are
     * accepted by this test.<p>
     * 
     * TODO: File a CDI bug (actually, file an EJB bug and ask them to prune the
     * spec).
     * 
     * <h4>Note 1</h4>
     * See {@linkplain com.martinandersson.javaee.cdi.specializes.SpecializesTest}.
     * 
     * <h4>Note 2</h4>
     * 
     * Here's what the client see:
     * <pre>
     * javax.ejb.EJBException: javax.ejb.EJBException: javax.ejb.CreateException: Could not create stateless EJB
     *     Caused by: javax.ejb.EJBException: javax.ejb.CreateException: Could not create stateless EJB
     *     Caused by: javax.ejb.CreateException: Could not create stateless EJB
     *     Caused by: java.lang.reflect.InvocationTargetException
     *     Caused by: java.lang.NullPointerException
     *         ...
     *         at com.sun.ejb.containers.BaseContainer.createEjbInstanceAndContext(BaseContainer.java:1696)
     * </pre>
     * 
     * <h4>Note 3</h4>
     * In this particular use case. We have not tested what happens to CDI
     * observers and producer methods that could be declared in class A. Even
     * more so, I bet that life cycle callback methods et cetera is called on
     * A (TODO: Examine).
     */
    @Test
    @OperateOnDeployment("-with-c")
    public void test_specializes_lookupUsingEJB() throws NamingException {
        /*
         * C, the bean that @Specializes A, can be used as usual. Using
         * InitialContext.doLookup() like we do here is equivalent to @EJB.
         * 
         * It has been confirmed that the results are the same whether we use
         * JNDI to lookup the bean or @EJB.
         */
        C c = Lookup.globalBean(C.class);
        assertEquals("C", c.simpleName());
        
        // A proxy to A was injected:
        assertNotNull(a);
        
        try {
            // GlassFish crash, WildFly survive:
            LOGGER.info(a.simpleName());
            LOGGER.info("Nothing thrown.");
        }
        catch (EJBException e) {
            LOGGER.log(Level.SEVERE, "Unless it hasn't been logged already, here you go ..", e);
        }
    }
    
    /**
     * Same results as previous test case.
     */
    @Test
    @OperateOnDeployment("-with-c")
    public void test_specializes_lookupUsingInject() {
        // For C, it is business as usual:
        C c = CDI.current().select(C.class).get();
        assertEquals("C", c.simpleName());
        
        // A proxy to A was injected:
        assertNotNull(a);
        
        try {
            // GlassFish crash, WildFly survive:
            LOGGER.info(a.simpleName());
            LOGGER.info("Nothing thrown.");
        }
        catch (EJBException e) {
            LOGGER.log(Level.SEVERE, "Unless it hasn't been logged already, here you go ..", e);
        }
    }
}