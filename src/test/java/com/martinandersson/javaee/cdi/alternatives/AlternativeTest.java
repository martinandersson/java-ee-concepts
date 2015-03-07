package com.martinandersson.javaee.cdi.alternatives;

import com.martinandersson.javaee.cdi.alternatives.lib.DefaultUsernameService;
import com.martinandersson.javaee.cdi.alternatives.lib.PermissiveUsernameService;
import com.martinandersson.javaee.cdi.alternatives.lib.UsernameService;
import com.martinandersson.javaee.utils.DeploymentBuilder;
import com.martinandersson.javaee.utils.HttpRequests;
import java.net.URL;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Alternative beans is a way to "short-circuit" the ordinary bean resolution of
 * CDI and use alternatives instead of the beans the application would normally
 * use.<p>
 * 
 * An alternative bean must be selected in order to be picked up and used.
 * Otherwise, the alternative is ignored.<p>
 * 
 * CDI 1.1, section "2.7 Alternatives":
 * <pre>{@code
 * 
 *     An alternative is a bean that must be explicitly selected if it should be
 *     available for lookup, injection or EL resolution.
 * 
 * }</pre>
 * 
 * Prior to CDI 1.1, alternatives were required to be "selected" in the
 * {@code beans.xml} file and such a selection is valid only for the bean
 * archive that the {@code beans.xml} file is put in. CDI 1.1 accept the
 * {@code @Priority} annotation as another way of selecting an
 * alternative<sup>1</sup>.<p>
 * 
 * Also see section "4.3 Specialization".<p>
 * 
 * 
 * 
 * <h3>Note 1</h3>
 * 
 * It is "Common Annotations for the Java Platform 1.2", JSR-250, that
 * defines the @Priority annotation. CDI 1.1 is not the only Java EE
 * specification that use this annotation. Interceptors 1.2, JSR-318, do so to
 * and there might be even more that I am not aware of.
 * 
 * 
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
public class AlternativeTest
{
    @Deployment
    private static WebArchive buildDeployment() {
        return new DeploymentBuilder(AlternativeTest.class)
                .addEmptyBeansXMLFile()
                .add(AlternativeDriver.class,
                     UsernameService.class,
                     DefaultUsernameService.class,
                     PermissiveUsernameService.class)
                .build();
    }
    
    @Test
    @RunAsClient
    public void selectedAlternativeIsUsed(@ArquillianResource URL url) {
        AlternativeDriver.Report<?> report = HttpRequests.getObject(url);
        
        assertEquals("Expected that the selected alternative would be used.",
                PermissiveUsernameService.class, report.beanType);
    }
}