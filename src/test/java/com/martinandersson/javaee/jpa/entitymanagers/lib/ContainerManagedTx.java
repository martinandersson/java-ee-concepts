package com.martinandersson.javaee.jpa.entitymanagers.lib;

import com.martinandersson.javaee.jpa.entitymanagers.EntityManagerExposer;
import java.util.function.Function;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * A {@code @Stateless} bean that uses default container-managed transactions
 * and a persistence context of default type {@code
 * PersistenceContextType.TRANSACTION}.<p>
 * 
 * This bean represents the most "default" setup one can get =)
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Stateless
@LocalBean
public class ContainerManagedTx implements EntityManagerExposer
{
    @PersistenceContext
    EntityManager em;
    
    /**
     * {@inheritDoc}
     */
    @Override
    public <R> R apply(Function<EntityManager, R> entityManagerFunction) {
        return entityManagerFunction.apply(em);
    }
    
    /**
     * Equivalent to {@linkplain #apply(Function)}, difference being that this
     * method will not use a transaction and possibly suspend a transaction if
     * one is active when this method is invoked.
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public <R> R applyWithoutTransaction(Function<EntityManager, R> entityManagerFunction) {
        return entityManagerFunction.apply(em);
    }
}