package com.martinandersson.javaee.cdi.lookup;

import com.martinandersson.javaee.utils.DeploymentBuilder;
import com.martinandersson.javaee.utils.HttpRequests;
import java.net.URL;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Demonstrates <tt>CDI.current().select()</tt> and show that using the CDI
 * container programmatically to lookup a bean will provide the client code with
 * a contextual (scoped) instance of the target bean.<p>
 * 
 * Also see:
 * <pre>{@code
 *     http://stackoverflow.com/q/15067650
 *     http://stackoverflow.com/q/24822361
 * }</pre>
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ProgrammaticLookupTest
{
    @Deployment
    private static Archive<?> buildDeployment() {
        return new DeploymentBuilder(ProgrammaticLookupTest.class)
                .add(AbstractId.class,
                     ApplicationScopedBean.class,
                     DependentBean.class,
                     RequestScopedBean.class,
                     ProgrammaticLookupRunner.class)
                .addEmptyBeansXMLFile()
                .build();
    }
    
    static long requestScopedId,
                applicationScopedId,
                dependentId1,
                dependentId2;
    
    @ArquillianResource
    URL url;
    
    @Test
    @InSequence(1)
    public void before() {
        long[] ids = HttpRequests.getObject(url);
        
        requestScopedId = ids[0];
        applicationScopedId = ids[1];
        dependentId1 = ids[2];
        dependentId2 = ids[3];
        
        assertTrue(requestScopedId > 1);
        assertTrue(applicationScopedId > 1);
        assertTrue(dependentId1 > 1);
        assertTrue(dependentId2 > 1);
        
        assertNotEquals(requestScopedId, applicationScopedId);
        assertNotEquals(applicationScopedId, dependentId1);
    }
    
    @Test
    @InSequence(2)
    public void testDependent1MustNotBeDependent2() {
        assertNotEquals("Each new lookup of a @Dependent should provide a new bean, ",
                dependentId1, dependentId2);
    }
    
    @Test
    @InSequence(3)
    public void testBeanIdsOfSecondRequest() {
        long[] ids = HttpRequests.getObject(url);
        
        assertNotEquals("One new @RequestScoped bean per request, ",
                requestScopedId, ids[0]);
        
        assertEquals("@ApplicationScoped bean is a singleton, ",
                applicationScopedId, ids[1]);
        
        assertEquals("@Dependent 1 must not be replaced, ",
                dependentId1, ids[2]);
        
        assertEquals("@Dependent 2 must not be replaced, ",
                dependentId2, ids[3]);
    }
}