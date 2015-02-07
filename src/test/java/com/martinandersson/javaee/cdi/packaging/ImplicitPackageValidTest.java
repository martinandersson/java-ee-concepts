package com.martinandersson.javaee.cdi.packaging;

import com.martinandersson.javaee.cdi.packaging.lib.CalculatorRequestScoped;
import com.martinandersson.javaee.utils.DeploymentBuilder;
import javax.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This test will not package a {@code beans.xml} descriptor file in the
 * deployment archive, meaning that the archive is an "implicit bean archive" in
 * which only annotated beans are eligible for CDI management.<p>
 * 
 * 
 * 
 * <h3>GlassFish bug #1</h3>
 * 
 * Scope types are "bean defining annotations". WildFly and my understanding of
 * all related Java EE specifications also make
 * {@code @javax.annotation.ManagedBean} a bean defining annotation. GlassFish
 * disagree. I posted a question about it on
 * <a href="http://stackoverflow.com/questions/25327057">Stackoverflow.com</a>
 * but until the final verdict arrive, I leave out injection of a
 * {@code @ManagedBean} from this test as it will crash GlassFish.<p>
 * 
 * 
 * 
 * <h3>GlassFish bug #2</h3>
 * 
 * No GlassFish version that I've tried (4.0.1-b08 and 4.1) doesn't want to
 * pickup the CDI inspector. GF 4.0.1-b08 pass the test with the CDI inspector
 * installed. GlassFish 4.1 however doesn't ({@code requestScoped == null}).
 * Looking in the log reveal a suspicious message: "BeanManager not found".
 * Adding the {@code beans.xml} file would solve this problem (still not pick up
 * the CDI inspector) but that would make this archive an explicit one. For now,
 * I'll leave the CDI inspector out of the game.<p>
 * 
 * TODO: File a bug.
 * 
 * 
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
public class ImplicitPackageValidTest
{
    @Deployment
    public static WebArchive buildDeployment() {
        return new DeploymentBuilder(ImplicitPackageValidTest.class)
                .add(CalculatorRequestScoped.class)
                .build();
        
        // Note: adding CDI inspector causes GF 4.1 to fail the test.
    }

    @Inject
    CalculatorRequestScoped requestScoped;
    
    @Test
    public void canInjectAnnotatedCalculator() {
        assertEquals(2, requestScoped.sum(1, 1));
    }
}