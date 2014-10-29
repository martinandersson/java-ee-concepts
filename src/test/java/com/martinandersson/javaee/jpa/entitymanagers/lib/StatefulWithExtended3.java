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
 * No methods in this EJB use transactions. If client has one when calling this
 * EJB, the transaction will be suspended.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@Stateful
@LocalBean
public class StatefulWithExtended3 implements EntityManagerExposer
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
    
    @Remove
    public void remove() {}
}