package com.martinandersson.javaee.jpa.mapping.orphanremoval;

import java.util.function.Consumer;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Stateless
public class Repository
{
    @PersistenceContext
    EntityManager em;
    
    public void applyWithEM(Consumer<EntityManager> accept) {
        accept.accept(em);
    }
    
    public void persist(Object entity) {
        em.persist(entity);
    }
    
    public <T> T merge(T entity) {
        return em.merge(entity);
    }
    
    public <T> T find(Class<? extends T> type, Object id) {
        return em.find(type, id);
    }
    
    /**
     * Remove an entity of the provided type and id.
     * 
     * @param type entity type
     * @param id entity id
     * 
     * @throws javax.persistence.EntityNotFoundException if asked to remove an
     *         entity that can not be found
     */
    public void remove(Class<?> type, Object id) {
        Object entity = em.getReference(type, id);
        em.remove(entity);
    }
}