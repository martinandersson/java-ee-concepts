package com.martinandersson.javaee.cdi.producers.entitymanager.producer;

import static com.martinandersson.javaee.cdi.producers.entitymanager.producer.Scope.Type.REQUEST;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

/**
 *
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RequestScoped
public class RequestScopedEMProducer
{
    public static final AtomicInteger INSTANCE_COUNTER = new AtomicInteger();
    
    public static final List<RuntimeException> CLOSE_EXCEPTIONS = new ArrayList<>();
    
    
    @PersistenceUnit
    EntityManagerFactory emf;
    
    
    
    @Produces
    @Scope(REQUEST)
    @RequestScoped
    public EntityManager newEntityManager() {
        return emf.createEntityManager();
    }
    
    public void closeEntityManager(@Disposes @Scope(REQUEST) EntityManager em) {
        try {
            em.close();
        }
        catch (RuntimeException e) {
            CLOSE_EXCEPTIONS.add(e);
        }
    }
    
    
    
    @PostConstruct
    private void __incrementCounter() {
        INSTANCE_COUNTER.incrementAndGet();
    }
}