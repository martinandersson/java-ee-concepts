package com.martinandersson.javaee.cdi.producers.entitymanager.producer;

import static com.martinandersson.javaee.cdi.producers.entitymanager.producer.Scope.Type.DEPENDENT;
import static com.martinandersson.javaee.cdi.producers.entitymanager.producer.Scope.Type.REQUEST;
import com.martinandersson.javaee.cdi.producers.entitymanager.producer.TestDriver.Report;
import com.martinandersson.javaee.resources.SchemaGenerationStrategy;
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
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Will "prove" that having a {@code @Dependent} producer class make the
 * container reinstantiate the producer when later calling his {@code @Disposes}
 * method.<p>
 * 
 * CDI 1.2, section "6.4. Dependent pseudo-scope":
 * <pre>{@code
 *     Any instance of the bean that receives a producer method, producer field,
 *     disposer method or observer method invocation exists to service that
 *     invocation only.
 * }</pre>
 * 
 * 
 * CDI 1.2, section "6.4.2. Destruction of objects with scope @Dependent":
 * <pre>{@code
 *     [..] any @Dependent scoped contextual instance created to receive a
 *     producer method, producer field, disposer method or observer method
 *     invocation is destroyed when the invocation completes [.]
 * }</pre>
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ProducerClassTest
{
    @Deployment
    private static Archive<?> buildDeployment() {
        return new DeploymentBuilder(ProducerClassTest.class)
                .addTestPackage()
                .addPersistenceXMLFile(SchemaGenerationStrategy.UPDATE)
                .build();
    }
    
    
    @ArquillianResource
    URL url;
    
    
    @Test
    @InSequence(1)
    public void test_firstRequest() {
        Report r = HttpRequests.getObject(url);
        
        // During first request.. one producer class of each scope is intantiated..
        
        assertEquals("Count of @Dependent producer classes instantiated ",
                (Integer) 1, r.instancesCreated.get(DEPENDENT));
        
        assertEquals("Count of @RequestScoped producer classes instantiated ",
                (Integer) 1, r.instancesCreated.get(REQUEST));
        
        // Just for the fun of it:
        assertTrue(r.closeExceptions.get(REQUEST).isEmpty());
        assertTrue(r.closeExceptions.get(DEPENDENT).isEmpty());
    }
    
    @Test
    @InSequence(2)
    public void test_secondRequest() throws InterruptedException {
        Report r = HttpRequests.getObject(url);
        
        /*
         * ..but after the first request, only the dependent producer class was
         * reinstantiated when disposing the entity manager. The container
         * reused the request scoped producer when calling his disposer.
         * 
         * So at this point in time, 3 dependent producer instances has been
         * instantiated (2 for each lookup + 1 dispose) but only 2 request
         * scoped producer instances has been created (2 for each lookup).
         */
        
        assertEquals("Count of @Dependent producer classes instantiated ",
                (Integer) 3, r.instancesCreated.get(DEPENDENT));
        
        assertEquals("Count of @RequestScoped producer classes instantiated ",
                (Integer) 2, r.instancesCreated.get(REQUEST));
        
        assertTrue(r.closeExceptions.get(REQUEST).isEmpty());
        assertTrue(r.closeExceptions.get(DEPENDENT).isEmpty());
    }
}