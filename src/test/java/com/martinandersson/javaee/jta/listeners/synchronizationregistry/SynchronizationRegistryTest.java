package com.martinandersson.javaee.jta.listeners.synchronizationregistry;

import com.martinandersson.javaee.jta.listeners.synchronizationregistry.TestDriver.Report;
import com.martinandersson.javaee.utils.Deployments;
import com.martinandersson.javaee.utils.HttpRequests;
import java.net.URL;
import javax.transaction.Status;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * The {@code TransactionSynchronizationRegistry} offer a low-level transaction
 * API "not meant to be used by application programmers".<p>
 * 
 * But of course, such a statement assume that all servers do what they are
 * supposed to do, which is not always the case. The current state of 1) buggy
 * server software, 2) an insufficient API and 3) the enforcement of programming
 * models upon others, require the developer to get his hands dirty and use the
 * registry from time to time.<p>
 * 
 * TODO: This "test" is currently very incomplete.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
public class SynchronizationRegistryTest
{
    @Deployment
    private static Archive<?> buildDeployment() {
        return Deployments.buildCDIBeanArchive(SynchronizationRegistryTest.class,
                TestDriver.class,
                ManagedBean.class,
                TransactionalManagedBean.class);
    }
    
    
    
    static Report report; // = initialized in __callDriver().
    
    @Test
    @RunAsClient
    @InSequence(1)
    public void __callDriver(@ArquillianResource URL url) {
        report = HttpRequests.getObject(url, TestDriver.class);
    }
    
    
    
    /*
     *  ------------
     * | REAL TESTS |
     *  ------------
     */
    
    @Test
    @RunAsClient
    @InSequence(2)
    public void managedBeanTXStatus() {
        assertEquals("Expected that a non-transaction managed bean has no active transaction. Transaction status",
                Status.STATUS_NO_TRANSACTION /* == 6 */, report.managedBeanTXStatus);
    }
    
    @Test
    @RunAsClient
    @InSequence(2)
    public void txManagedBeanTXStatus() {
        assertEquals("Expected that a @Transactional managed bean uses transactions. Transaction status",
                Status.STATUS_ACTIVE /* == 0 */, report.txManagedBeanTXStatus);
    }
    
    @Test
    @RunAsClient
    @InSequence(2)
    public void txManagedBeanTXStatusBeforeCompletion() {
        assertEquals("Expected that before completion, transaction was active. Transaction status",
                Status.STATUS_ACTIVE /* == 0 */, report.txManagedBeanTXStatusBeforeCompletion);
    }
    
    @Test
    @RunAsClient
    @InSequence(2)
    public void txManagedBeanTXStatusAfterCompletion() {
        assertEquals("Expected that after completion, transaction was committed. Transaction status",
                Status.STATUS_COMMITTED /* == 3 */, report.txManagedBeanTXStatusAfterCompletion);
    }
}