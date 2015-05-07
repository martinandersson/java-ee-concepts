package com.martinandersson.javaee.cdi.producers.entitymanager.unsafe;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.inject.spi.CDI;
import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@WebServlet("")
public class OracleTestDriver extends HttpServlet
{
    private static final Logger LOGGER = Logger.getLogger("TEST");
    
    
    
    public static void log(String prefix, EntityManager em) {
        LOGGER.info(() ->
                Thread.currentThread().getName() + " " + prefix +": " + identify(em));
    }
    
    private static String identify(EntityManager em) {
        return em.toString() + " #" + System.identityHashCode(em);
    }
    
    
    
    @PostConstruct
    private void __assertProducerWasIntercepted() {
        assertTrue(EMProducerExtension.producerIntercepted);
    }
    
    
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        Report r1 = run("(T1)");
        Report r2 = run("(T2)");
        
        new ObjectOutputStream(resp.getOutputStream()).writeObject(
                new Report[]{ r1, r2 });
    }
    
    /**
     * Lookup a {@code EntityManagerConsumer}, inspect the wrapped entity
     * manager and then dispose the bean.<p>
     * 
     * All happen in a new unmanaged thread. I prefer not to use {@code
     * ManagedThreadFactory} so that we get as "close to the metal" as possible.
     * 
     * @param threadName thread name
     * 
     * @return a report
     */
    private Report run(String threadName) {
        BlockingQueue<Report> reportHandoff = new SynchronousQueue<>();
        
        Thread t = new Thread(() -> {
            EntityManagerConsumer wrapper = CDI.current().select(EntityManagerConsumer.class).get();
            EntityManager cdiProxy = wrapper.getEntityManager();
            
            // Force CDI proxy to acquire the entity manager (the proxy is probably lazy):
            assertTrue(cdiProxy.isOpen());
            
            log("RUNNER CDI PROXY", cdiProxy);
            String cdiProxyId = identify(cdiProxy);
            
            // This call is possible because of class EMProducerExtension:
            EntityManager jpaProxy = cdiProxy.unwrap(EntityManager.class);
            
            log("RUNNER JPA PROXY", jpaProxy);
            String jpaProxyId = identify(jpaProxy);
            
            CDI.current().destroy(wrapper);
            
            Report report = new Report(
                    cdiProxyId, jpaProxyId,
                    OracleProducer.numberOfInstancesCreated(),
                    OracleProducer.consumeDisposeException(),
                    cdiProxy.isOpen(), jpaProxy.isOpen());
            
            try {
                reportHandoff.offer(report, 3, TimeUnit.SECONDS);
            }
            catch (InterruptedException e) {
                LOGGER.warning(() -> threadName + " failed to pass report to test runner.");
            }
        });
        
        t.setName(threadName);
        t.start();
        
        try {
            return reportHandoff.poll(3, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            throw new RuntimeException("Someone doesn't want the report obviously.", e);
        }
    }
    
    
    
    static class Report implements Serializable
    {
        final String cdiProxyId,
                     jpaProxyId;
        
        final int producersCreated;
        
        final RuntimeException disposerException;
        
        final boolean cdiProxyRemainedOpen,
                      jpaProxyRemainedOpen;
        
        Report(String cdiProxyId,
               String jpaProxyId,
               int producersCreated,
               RuntimeException disposerException,
               boolean cdiProxyRemainedOpen,
               boolean jpaProxyRemainedOpen)
        {
            this.cdiProxyId = cdiProxyId;
            this.jpaProxyId = jpaProxyId;
            this.producersCreated = producersCreated;
            this.disposerException = disposerException;
            this.cdiProxyRemainedOpen = cdiProxyRemainedOpen;
            this.jpaProxyRemainedOpen = jpaProxyRemainedOpen;
        }
    }
}