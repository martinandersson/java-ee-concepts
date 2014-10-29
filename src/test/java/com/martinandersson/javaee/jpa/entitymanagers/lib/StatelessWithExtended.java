package com.martinandersson.javaee.jpa.entitymanagers.lib;

import com.martinandersson.javaee.jpa.entitymanagers.EntityManagerExposer;
import java.util.function.Function;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

/**
 * This bean is a {@code @Stateless} EJB that try to use an entity manager of
 * type {@code PersistenceContextType.EXTENDED}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Stateless
@LocalBean
public class StatelessWithExtended implements EntityManagerExposer
{
    @PersistenceContext(type = PersistenceContextType.EXTENDED)
    EntityManager em;
    
    /**
     * {@inheritDoc}
     */
    @Override
    public <R> R apply(Function<EntityManager, R> entityManagerFunction) {
        return entityManagerFunction.apply(em);
    }
}