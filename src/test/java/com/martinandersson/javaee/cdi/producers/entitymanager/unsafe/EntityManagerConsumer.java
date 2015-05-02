package com.martinandersson.javaee.cdi.producers.entitymanager.unsafe;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

/**
 *
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@ApplicationScoped
public class EntityManagerConsumer
{
    @Inject
    @UserDatabase
    EntityManager em;
    
    
    @PostConstruct
    private void __logEntityManager() {
        OracleTestDriver.log("CONSUMER-POSTCONSTRUCT CDI PROXY", em);
    }
    
    
    public EntityManager getEntityManager() {
        return em;
    }
}
