package com.martinandersson.javaee.cdi.producers.entitymanager.unsafe;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.logging.Logger;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessProducer;
import javax.enterprise.inject.spi.Producer;
import javax.persistence.EntityManager;

/**
 *
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class EMProducerExtension implements Extension
{
    public static boolean producerIntercepted;
    
    /**
     * Will intercept the container's producer such that what the producer
     * return is a custom EntityManager proxy that in turn allow us to get a
     * stored reference to the underlying reference that
     * {@code @PersistenceContext} once injected.<p>
     * 
     * To get the underlying reference, call the entity manager like so:
     * <pre>{@code
     * 
     *    em.unwrap(EntityManager.class);
     * }</pre>
     * 
     * @implNote
     * An observer method is allowed to be 1) private, 2) static. As far as the
     * CDI spec goes, an observer method on a CDI extension is no exception to
     * the rule.<p>
     * 
     * If this method is made public static or private static, then both
     * GlassFish 4.1 and WildFly 8.2.0 will crash during deployment with the
     * following Exception:
     * <pre>{@code
     * 
     *     org.jboss.weld.exceptions.IllegalStateException: WELD-000143:
     *         Container lifecycle event method invoked outside of extension
     *         observer method invocation.
     * }</pre>
     * 
     * TDOO: Report Weld bug.
     * 
     * @param event provided by CDI container
     */
    private void onProcessProducer(@Observes ProcessProducer<OracleProducer, EntityManager> event) {
        Producer<EntityManager> delegate = event.getProducer();
        
        event.setProducer(new Producer<EntityManager>(){
            @Override public EntityManager produce(CreationalContext<EntityManager> ctx) {
                EntityManager jpaProxy = delegate.produce(ctx);
                OracleTestDriver.log("EXTENSION JPA PROXY", jpaProxy);
                
                InvocationHandler handler = (self, method, args) -> {
                    if (method.getName().equals("unwrap") &&
                        args.length == 1 && args[0] == EntityManager.class)
                    {
                        Logger.getLogger("TEST").info(() ->
                                Thread.currentThread().getName() + " *** \"unwrapping\" real JPA proxy ***");
                        
                        return jpaProxy;
                    }
                    else {
                        try {
                            return method.invoke(jpaProxy, args);
                        }
                        catch (InvocationTargetException e) {
                            throw e.getCause();
                        }
                    }
                };
                
                EntityManager hacked = (EntityManager) Proxy.newProxyInstance(
                        getClass().getClassLoader(), new Class[]{EntityManager.class}, handler);
                
                OracleTestDriver.log("EXTENSION HACKED PROXY", hacked);
                return hacked;
            }

            @Override public void dispose(EntityManager instance) {
                delegate.dispose(instance);
            }

            @Override public Set<InjectionPoint> getInjectionPoints() {
                return delegate.getInjectionPoints();
            }
        });
        
        producerIntercepted = true;
    }
}