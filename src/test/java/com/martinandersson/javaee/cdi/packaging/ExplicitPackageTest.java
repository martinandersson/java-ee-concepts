package com.martinandersson.javaee.cdi.packaging;

import com.martinandersson.javaee.cdi.packaging.lib.CalculatorManagedBean;
import com.martinandersson.javaee.cdi.packaging.lib.CalculatorRequestScoped;
import com.martinandersson.javaee.cdi.packaging.lib.CalculatorUnAnnotated;
import com.martinandersson.javaee.utils.DeploymentBuilder;
import com.martinandersson.javaee.utils.Deployments;
import javax.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This test will package an empty {@code beans.xml} descriptor file in the
 * deployment archive, meaning that the archive is an "explicit bean archive" in
 * which all classes are eligible for CDI management.<p>
 * 
 * This is the most used approach and is "backward compatible" with CDI 1.0
 * (Java EE 6).<p>
 * 
 * See JavaDoc of {@linkplain ImplicitPackageValidTest}. GlassFish pass this
 * test, but remember that it is an "explicit bean archive" that is deployed.
 * Therefore, it is no surprise that injection of {@code @ManagedBean} work for
 * this archive even for GlassFish.
 *  
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
public class ExplicitPackageTest
{
    @Deployment
    public static WebArchive buildDeployment() {
        WebArchive war = new DeploymentBuilder(ExplicitPackageTest.class)
                .addEmptyBeansXMLFile()
                .add(CalculatorUnAnnotated.class,
                     CalculatorManagedBean.class,
                     CalculatorRequestScoped.class)
                .build();
        
        return Deployments.installCDIInspector(war);
    }
    
    
    
    // In this example. All injected beans here are "managed". Read more:
    //    ./com/martinandersson/javaee/cdi/package-info.java
    
    @Inject
    CalculatorUnAnnotated unAnnotated;
    
    @Inject
    CalculatorManagedBean usingManagedAnnotation;
    
    @Inject
    CalculatorRequestScoped usingScopeAnnotation;
    
    
    
    @Test
    public void canInjectUnAnnotatedCalculator() {
        assertEquals(2, unAnnotated.sum(1, 1));
    }
    
    @Test
    public void canInjectBeanAnnotatedCalculator() {
        assertEquals(2, usingManagedAnnotation.sum(1, 1));
    }
    
    @Test
    public void canInjectScopeAnnotatedCalculator() {
        assertEquals(2, usingScopeAnnotation.sum(1, 1));
    }
}