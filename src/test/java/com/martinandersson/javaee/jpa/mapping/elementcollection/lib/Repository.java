package com.martinandersson.javaee.jpa.mapping.elementcollection.lib;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.REQUIRED;
import static javax.ejb.TransactionAttributeType.SUPPORTS;
import javax.persistence.EntityGraph;
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
    
    @TransactionAttribute(SUPPORTS)
    public <T> T findById(Class<? extends T> type, long id) {
        Map<String, Object> properties = Collections.EMPTY_MAP;
        try {
            
            /*
             * Entity graph is used by Person and Song only for the benefit of
             * WildFly/Hibernate.
             * 
             * The state of WildFly's JPA entities won't survive transaction
             * boundaries (note 1) so our test will crash when Object.equals()
             * uses the collection attribute (Hibernate complains about "no
             * session"). So, the entity graph is only used to alleviate this
             * problem.
             * 
             * Another option would be to eagerly fetch the collection without
             * an entity graph, adding FetchType.EAGER to @ElementCollection.
             * There's just one problem. Song doesn't use @ElementCollection
             * which is the whole purpose of the test that uses the Song entity.
             * 
             * Yet another option would be to make the client code start/commit
             * a single transaction that he uses throughout the test. That would
             * make the entity managed and thus the collection attribute would
             * become traversable. However, the tests want to make sure that the
             * entity mapping configuration can not only survive a persist call,
             * but also that the entity mapping configuration can survive
             * flush, clear and finally retrieve.
             * 
             * GlassFish/EclipseLink doesn't have this problem. State of his
             * JPA entities survive transaction boundaries.
             */
            
            EntityGraph<?> graph = em.getEntityGraph(type.getSimpleName());
            properties = new HashMap<>();
            properties.put("javax.persistence.loadgraph", graph);
        }
        catch (IllegalArgumentException e) {
            // No named graph found.
        }
        
        return em.find(type, id, properties);
    }
    
    @TransactionAttribute(SUPPORTS)
    public void clearCaches() {
        em.clear(); // <-- unnecessarily without a transaction (EM is not extended, all entities on the outside of a TX is already detached)
        em.getEntityManagerFactory().getCache().evictAll();
    }
    
    @TransactionAttribute(REQUIRED)
    public void remove(Object entity) {
        if (!em.contains(entity)) {
            entity = em.merge(entity);
        }
        
        em.remove(entity);
    }
    
    @TransactionAttribute(REQUIRED)
    public <T> T apply(Function<EntityManager, T> function) {
        return function.apply(em);
    }
}

/*
 * NOTE 1: JPA 2.1 specification, section "3.2.7 Detached Entities" says about
 *         the state of detached entities:
 * 
 *             "Their state is no longer guaranteed to be synchronized with the
 *              database state."
 * 
 *          Hence by no means do the specification forbid JPA providers to fetch
 *          state of an entity even after it has been detached. Fact is that
 *          FetchType.LAZY is only a "hint" to the provider, who is always free
 *          to eagerly read data from the database.
 */