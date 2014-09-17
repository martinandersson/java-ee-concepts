package com.martinandersson.javaee.jpa.mapping.elementcollection;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * {@code @Stateless} repository with default container-managed transactions.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Stateless
public class Repository
{
    @PersistenceContext
    EntityManager em;
    
    public void persist(Object anything) {
        em.persist(anything);
    }
}