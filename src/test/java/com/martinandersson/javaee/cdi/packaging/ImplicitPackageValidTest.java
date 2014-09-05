package com.martinandersson.javaee.cdi.packaging;

import com.martinandersson.javaee.cdi.packaging.calculators.CalculatorRequestScoped;
import com.martinandersson.javaee.utils.Deployments;
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
 * Scope types are "bean defining annotations". WildFly and my understanding of
 * all related Java EE specifications also make
 * {@code @javax.annotation.ManagedBean} a bean defining annotation. GlassFish
 * disagree. I posted a question about it on
 * <a href="http://stackoverflow.com/questions/25327057">Stackoverflow.com</a>
 * but until the final verdict arrive, I leave out injection of a
 * {@code @ManagedBean} from this test as it will crash GlassFish.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
public class ImplicitPackageValidTest
{
    @Deployment
    public static WebArchive buildDeployment() {
        WebArchive war = Deployments.buildWAR(ImplicitPackageValidTest.class, CalculatorRequestScoped.class);
        return Deployments.installCDIInspector(war);
    }

    @Inject
    CalculatorRequestScoped requestScoped;
    
    @Test
    public void canInjectAnnotatedCalculator() {
        assertEquals(2, requestScoped.sum(1, 1));
    }
}