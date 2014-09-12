package com.martinandersson.javaee.cdi.specializes;

import com.martinandersson.javaee.cdi.specializes.lib.StupidCalculator;
import com.martinandersson.javaee.cdi.specializes.lib.SmartCalculator;
import com.martinandersson.javaee.cdi.specializes.SpecializesDriver.Report;
import com.martinandersson.javaee.utils.Deployments;
import com.martinandersson.javaee.utils.HttpRequests;
import java.net.URL;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Specialization in Java SE programming is realized using inheritance. The
 * subclass is a specialized version of the more generic superclass. So too can
 * a CDI bean specialize another bean. For the CDI container to always select
 * the specialized bean as the injection target, the bean class must be
 * annotated with
 * {@linkplain javax.enterprise.inject.Specializes @Specializes}.<p>
 * 
 * Although all examples online use a specialized {@code @Alternative} bean, the
 * specification says not a word that a specialized bean must also be an
 * {@code @Alternative}.<p>
 * 
 * A specialized CDI bean fully replaces his counterpart and the developer
 * doesn't have to reconfigure injection points that originally saw the
 * generalized bean.<p>
 * 
 * 
 * 
 * <h3>Warning</h3>
 * 
 * The replaced bean is not used at all, it is "disabled". Here's the quotes on
 * that..<p>
 * 
 * CDI 1.1 specification, section "4.3 Specialization":
 * <pre>{@code
 * 
 *     When an enabled bean, as defined in Section 5.1.2, specializes a second
 *     bean, we can be certain that the second bean is never instantiated or
 *     called by the container. Even if the second bean defines a producer or
 *     observer method, the method will never be called.
 * 
 * }</pre>
 * 
* 
* JavaDoc of {@linkplain javax.enterprise.inject.Specializes @Specializes}:
* <pre>{@code
* 
*     If a bean is specialized by any enabled bean, the first bean is disabled.
* 
 * }</pre>
 * 
 * <strong>However</strong>, WELD do call the observer methods of our replaced
 * bean, both static and non-static. So currently, the test
 * "stupidCalculatorObserversNotCalled" in this class fail in both GlassFish and
 * WildFly.
 * 
 * Bug filed <a href="https://issues.jboss.org/browse/WELD-1741">here<a/>.
 * 
 * 
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
public class SpecializesTest
{
    @Deployment
    public static WebArchive buildDeployment() {
        return Deployments.buildCDIBeanArchive(
                SpecializesTest.class,
                SpecializesDriver.class,
                StupidCalculator.class,
                SmartCalculator.class);
    }
    
    static Report report; // = initialized in __callDriver().
    
    @Test
    @RunAsClient
    @InSequence(1)
    public void  __callDriver(@ArquillianResource URL url) {
        report = HttpRequests.getObject(url, SpecializesDriver.class);
    }
    
    @Test
    @RunAsClient
    @InSequence(2)
    public void injectedSpecializedCalculator() {
        assertEquals("Expected that @Specializes " + SmartCalculator.class.getSimpleName() + " was the injection target,",
                SmartCalculator.class, report.stupidCalculatorType);
    }

    @Test
    @InSequence(2)
    public void stupidCalculatorObserversNotCalled() {
        assertEquals("Expected that no observer methods of a \"disabled\" bean is called,",
                0, StupidCalculator.OBSERVER_COUNTER.sum());
    }
}