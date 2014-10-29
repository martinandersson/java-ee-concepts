package com.martinandersson.javaee.jpa.entitymanagers.lib;

import com.martinandersson.javaee.jpa.entitymanagers.EntityManagerExposer;
import java.util.function.Function;
import javax.ejb.LocalBean;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

/**
 * A {@code @Stateful} EJB that uses a container-managed entity manager with
 * extended persistence context.<p>
 * 
 * Only the {@code @Remove} method in this EJB will use a transaction.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Stateful
@LocalBean
public class StatefulWithExtended2 implements EntityManagerExposer
{
    @PersistenceContext(type = PersistenceContextType.EXTENDED)
    EntityManager em;
    
    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @Override
    public <R> R apply(Function<EntityManager, R> entityManagerFunction) {
        return entityManagerFunction.apply(em);
    }
    
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Remove
    public void remove() {}
}