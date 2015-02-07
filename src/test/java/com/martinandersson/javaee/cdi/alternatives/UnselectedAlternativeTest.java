package com.martinandersson.javaee.cdi.alternatives;

import com.martinandersson.javaee.cdi.alternatives.lib.DefaultUsernameService;
import com.martinandersson.javaee.cdi.alternatives.lib.UnselectedUsernameService;
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
 * CDI 1.1 specification, section "2.7 Alternatives":
 * <pre>{@code
 * 
 *     An alternative is a bean that must be explicitly selected if it should be
 *     available for lookup, injection or EL resolution.
 * 
 * }</pre>
 * 
 * Thus, an alternative bean might be deployed to an archive but still not
 * eligible as an injection target unless it is also selected. See section
 * "5.1.1 Declaring selected alternatives".
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
public class UnselectedAlternativeTest
{
    @Deployment
    public static WebArchive buildDeployment() {
        return new DeploymentBuilder(UnselectedAlternativeTest.class)
                .addEmptyBeansXMLFile()
                .add(AlternativeDriver.class,
                     UsernameService.class,
                     DefaultUsernameService.class,
                     UnselectedUsernameService.class)
                .build();
    }
    
    @Test
    @RunAsClient
    public void unselectedAlternativeIsDeployableButNotUsed(@ArquillianResource URL url) {
        AlternativeDriver.Report<?> report = HttpRequests.getObject(url, AlternativeDriver.class);
        
        assertEquals("Expected that the default username service would be used. The alternative has not been selected!",
                DefaultUsernameService.class, report.beanType);
    }
}