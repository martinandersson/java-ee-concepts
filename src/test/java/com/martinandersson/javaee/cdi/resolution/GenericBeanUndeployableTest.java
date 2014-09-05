package com.martinandersson.javaee.cdi.resolution;

import com.martinandersson.javaee.utils.Deployments;
import javax.enterprise.context.RequestScoped;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.ShouldThrowException;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A parameterized bean class may effectively be "any type", strictly meaning
 * that type resolution is impossible. For example, say that such bean exists:
 * 
 * <pre>
 * 
 *   &#064;SessionScoped
 *   public class MyPreference&lt;T> {
 *       public T getPreference() {
 *           // ...
 *       }
 *   }
 * 
 * </pre>
 * 
 * Which bean should these two injection points resolve to?
 * 
 * <pre>
 * 
 *   &#064;Inject
 *   MyPreference&lt;String> stringPreference;
 *   
 *   &#064;Inject
 *   MyPreference&lt;Integer> integerPreference;
 * 
 * </pre>
 * 
 * Therefore, a bean must not be parameterized if it has a normal scope.
 * {@code @Dependent} beans however are allowed to be parameterized because they
 * are created one new for each injection point. The container can simply
 * instantiate a new raw bean of the @Dependent scope and inject that.<p>
 * 
 * Source (CDI 1.1 specification, section "3.1 Managed Beans"):
 * 
 * <pre>{@code
 * 
 *   If the managed bean class is a generic type, it must have scope @Dependent.
 *   If a managed bean with a parameterized bean class declares any scope other
 *   than @Dependent, the container automatically detects the problem and treats
 *   it as a definition error.
 * 
 * }</pre>
 * 
 * 
 * 
 * Any solution? Redesign your bean. For example:
 * 
 * <pre>
 * 
 *   &#064;SessionScoped
 *   abstract class MyPreference&lt;T> {
 *       abstract T getPreference();
 *   }
 * 
 *   public class StringPreference extends MyPreference&lt;String> {
 *       &#064;Override public String getPreference() {
 *           // ...
 *       }
 *   }
 *   
 *   public class IntegerPreference extends MyPreference&lt;Integer> {
 *       &#064;Override public Integer getPreference() {
 *           // ...
 *       }
 *   }
 * 
 * </pre>
 * 
 * ..haven't tested the code or anything. But it should do the trick.<p>
 * 
 * <strong>TODO:</strong> GlassFish fail this test. Report bug.
 * 
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
public class GenericBeanUndeployableTest {
    
    @Deployment
    @ShouldThrowException // Should be: javax.enterprise.inject.spi.DefinitionException.class
    public static WebArchive buildDeployment() {
        return Deployments.buildCDIBeanArchive(
                GenericBeanUndeployableTest.class,
                GenericBeanRequestScoped.class);
    }
    
    @Test // <-- implicitly @RunAsClient given that @ShouldThrowException is put on @Deployment
    public void genericBeanMustBeDependent() { }
}

/**
 * A parameterized bean class with a normal scope equals definition error.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RequestScoped
class GenericBeanRequestScoped<T extends String> {
    T repeat(T any) {
        return any;
    }
}