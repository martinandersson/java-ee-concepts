package com.martinandersson.javaee.cdi.lookup;

import com.martinandersson.javaee.cdi.lookup.InstanceVersusProviderRunner.CallFirst;
import com.martinandersson.javaee.utils.DeploymentBuilder;
import com.martinandersson.javaee.utils.HttpRequests;
import com.martinandersson.javaee.utils.HttpRequests.RequestParameter;
import java.net.URL;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * The CDI specification does not say a word about the {@code Provider<T>}
 * interface (defined by JSR-330). And the JavaDoc of {@code Provider<T>} is not
 * very helpful either. But this test will prove that {@code Provider<T>} return
 * a contextual (scoped) instance, just like {@code Instance<T>} do.<p>
 * 
 * CDI 1.2, section "5.6.1. The Instance interface":
 * <pre>{@code
 * 
 *     [..] obtain a contextual reference for the bean [..].
 * 
 *     The method destroy() instructs the container to destroy the instance. The
 *     bean instance passed to destroy() should be a dependent scoped bean
 *     instance, or a client proxy for a normal scoped bean. Applications are
 *     encouraged to always call destroy() when they no longer require an
 *     instance obtained from Instance. All built-in normal scoped contexts
 *     support destroying bean instances. An UnsupportedOperationException is
 *     thrown if the active context object for the scope type of the bean does
 *     not support destroying bean instances.
 * }</pre>
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
@RunAsClient
public class InstanceVersusProviderTest
{
    @Deployment
    private static Archive<?> buildDeployment() {
        return new DeploymentBuilder(InstanceVersusProviderTest.class)
                .add(AbstractId.class,
                     RequestScopedBean.class,
                     InstanceVersusProviderRunner.class)
                .build();
    }
    
    @ArquillianResource
    URL url;
    
    static long[] lastIds;
    
    @Test
    public void test_callInstanceFirstThenProvider() {
        long[] Ids = HttpRequests.getObject(url,
                new RequestParameter("callFirst", CallFirst.INSTANCE));
        
        assertEquals(Ids[0], Ids[1]);
        
        if (lastIds == null) {
            lastIds = Ids;
        }
        else {
            assertNotEquals(Ids[0], lastIds[0]);
        }
    }
    
    @Test
    public void test_callProviderFirstThenInstance() {
        long[] Ids = HttpRequests.getObject(url,
                new RequestParameter("callFirst", CallFirst.PROVIDER));
        
        assertEquals(Ids[0], Ids[1]);
        
        if (lastIds == null) {
            lastIds = Ids;
        }
        else {
            assertNotEquals(Ids[0], lastIds[0]);
        }
    }
}