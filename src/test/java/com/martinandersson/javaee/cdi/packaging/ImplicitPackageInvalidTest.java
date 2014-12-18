package com.martinandersson.javaee.cdi.packaging;

import com.martinandersson.javaee.cdi.packaging.lib.CalculatorUnAnnotated;
import com.martinandersson.javaee.utils.Deployments;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.ShouldThrowException;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This test will not package a {@code beans.xml} descriptor file in the
 * deployment archive, meaning that the archive is an "implicit bean archive" in
 * which only annotated beans are eligible for CDI management.<p>
 * 
 * If we try to use a bean that has no "bean defining annotations", what happens
 * then?<p>
 * 
 * CDI 1.1, section 5.2.2 "Unsatisfied and ambiguous dependencies":
 * <pre>{@code
 *     "If an unsatisfied or unresolvable ambiguous dependency exists, the
 *      container automatically detects the problem and treats it as a
 *      deployment problem."
 * }</pre>
 * 
 * 
 * CDI 1.1, section 2.9 "Problems detected automatically by the container":
 * <pre>{@code
 *      "Deployment problems [..] occur when there are problems resolving
 *       dependencies, or inconsistent specialization, in a particular
 *       deployment. If a deployment problem occurs, the container must throw a
 *       subclass of javax.enterprise.inject.spi.DeploymentException."
 * }</pre>
 * 
 * So the specification is pritty clear. The deployment is not supposed to
 * happen.<p>
 * 
 * 
 * 
 * <h3>The Arquillian @ShouldThrowException dilemma</h3>
 * 
 * Inspecting the server logs after running this test reveal that both WildFly
 * 8.1.0 and GlassFish 4.1 throw this exception (WildFly 8.2.0 won't pass this
 * test):
 * 
 * <pre>{@code
 *     org.jboss.weld.exceptions.DeploymentException.class
 * }</pre>
 * 
 * ..which extend {@code javax.enterprise.inject.spi.DeploymentException} so
 * this behavior are in full accordance with the specification. Therefore, it
 * would be great if we could specify the expected exception class in the value
 * attribute of {@code @ShouldThrowException}. However, doing so make the
 * client, that is Arquillian, go crazy, throwing:
 * <pre>{@code
 *     org.jboss.arquillian.container.spi.client.container.DeploymentException.class
 * }</pre>
 * 
 * ..which do not extend the CDI 1.1 defined DeploymentException. But that
 * doesn't matter. Even if we were to specify the base class "Exception.class",
 * then Arquillian would still fail the test with this message:
 * <pre>{@code
 *     Cannot deploy: ImplicitPackageInvalidTest.war
 * }</pre>
 * 
 * So the problem with the test in this case, according to Arquillian, is that
 * the test which is not supposed to deploy didn't deploy (!).<p>
 * 
 * With that said, because further exception class specification display an
 * Arquillian related problem, having a naked {@code @ShouldThrowException}
 * will do since we know why and the why is exactly as expected.<p>
 * 
 * 
 * 
 * <h3>Results</h3>
 * 
 * Work on GlassFish 4.1 and WildFly 8.1.0, but not WildFly 8.2.0.<p>
 * 
 * TODO: Report a bug.
 * 
 * 
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
public class ImplicitPackageInvalidTest
{
    @Deployment
    @ShouldThrowException // <-- Should be: javax.enterprise.inject.spi.DeploymentException.class
    public static WebArchive buildDeployment() {
        return Deployments.buildWAR(ImplicitPackageInvalidTest.class,
                CalculatorUnAnnotated.class,
                ForceBeanDiscovery.class);
    }
    
    @Test // <-- implicitly @RunAsClient given that @ShouldThrowException is put on @Deployment
    public void implicitBeanArchiveRequireExplicitBeanAnnotations() {
        // Empty
    }
}

@Singleton
@Startup
class ForceBeanDiscovery {
    @Inject CalculatorUnAnnotated unAnnotated;
    
    @PostConstruct
    public void helloWorld() {
        /*
         * GlassFish 4.1 and WildFly 8.1.0 pass this test even without the
         * @Startup annotation or this life cycle callback. WildFly 8.2.0 deploy
         * the archive but won't log this message. Apparently, WildFly 8.2.0
         * won't discover the ForceBeanDiscovery bean. As far as I can tell,
         * EJB 3.2 doesn't require an EJB to have public access.
         */
        Logger.getLogger(ForceBeanDiscovery.class.getName()).severe("Not supposed to be deployed!");
    }
}