package com.martinandersson.javaee.jpa.entitymanagers.lib;

import com.martinandersson.javaee.jpa.entitymanagers.EntityManagerExposer;
import java.util.function.Function;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.SynchronizationType;

/**
 * A {@code @Stateless} bean whose entity manager is of {@code
 * SynchronizationType UNSYNCHRONIZED}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Stateless
@LocalBean
public class UnsynchronizedEM implements EntityManagerExposer
{
    @PersistenceContext(synchronization = SynchronizationType.UNSYNCHRONIZED)
    EntityManager em;

    /**
     * {@inheritDoc}
     */
    @Override
    public <R> R apply(Function<EntityManager, R> entityManagerFunction) {
        return entityManagerFunction.apply(em);
    }
}