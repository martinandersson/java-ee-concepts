package com.martinandersson.javaee.jpa.entitymanagers.lib;

import com.martinandersson.javaee.jpa.entitymanagers.EntityManagerExposer;
import java.util.function.Function;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;


/**
 * This bean is a {@code @Singleton} EJB that try to use an entity manager of
 * type {@code PersistenceContextType.EXTENDED}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Singleton
@LocalBean
public class SingletonWithExtended implements EntityManagerExposer
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