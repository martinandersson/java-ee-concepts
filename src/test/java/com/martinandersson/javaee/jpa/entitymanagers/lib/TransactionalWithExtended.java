package com.martinandersson.javaee.jpa.entitymanagers.lib;

import com.martinandersson.javaee.jpa.entitymanagers.EntityManagerExposer;
import java.util.function.Function;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.transaction.Transactional;

/**
 * This bean is a {@code @Transactional} CDI bean that try to use an entity
 * manager of type {@code PersistenceContextType.EXTENDED}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Transactional
public class TransactionalWithExtended implements EntityManagerExposer
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