package com.martinandersson.javaee.cdi.specializes;

import com.martinandersson.javaee.cdi.specializes.lib.DefaultUserSettings;
import com.martinandersson.javaee.cdi.specializes.lib.SpecializedUserSettings;
import com.martinandersson.javaee.cdi.specializes.lib.UserSettings;
import com.martinandersson.javaee.utils.Deployments;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.ShouldThrowException;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This undeployable deployment prove that specialization require subtyping.<p>
 * 
 * To continue on the example provided in
 * {@linkplain com.martinandersson.javaee.cdi.specializes.lib.StupidCalculator StupidCalculator},
 * had the other corporate division been obsessed with marking classes as final,
 * then good luck to you.<p>
 * 
 * CDI 1.1 "3.1.4 Specializing a managed bean":
 * <pre>{@code
 * 
 *     If a bean class of a managed bean X is annotated @Specializes, then the
 *     bean class of X must directly extend the bean class of another managed
 *     bean Y. [..] If the bean class of X does not directly extend the bean
 *     class of another managed bean, the container automatically detects the
 *     problem and treats it as a definition error.
 * 
 * }</pre>
 * 
 * CDI 1.1 "4.3.1 Direct and indirect specialization":
 * <pre>{@code
 * 
 *     Furthermore, X must have all the bean types of Y. If X does not have some
 *     bean type of Y, the container automatically detects the problem and
 *     treats it as a definition error.
 * 
 * }</pre>
 * 
 * 
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
public class UndeployableSpecializationTest {
    
    @Deployment
    @ShouldThrowException
    public static WebArchive buildDeployment() {
        return Deployments.buildCDIBeanArchive(
                UndeployableSpecializationTest.class,
                UserSettings.class,
                DefaultUserSettings.class,
                SpecializedUserSettings.class);
    }
    
    @Test
    public void specializedBeanMustExtend() {}
}