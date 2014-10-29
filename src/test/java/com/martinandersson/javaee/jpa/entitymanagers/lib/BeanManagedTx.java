package com.martinandersson.javaee.jpa.entitymanagers.lib;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

/**
 * A {@code @Stateless} bean that uses bean-managed transactions and a
 * persistence context of default type {@code
 * PersistenceContextType.TRANSACTION}.<p>
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class BeanManagedTx
{
    private static final Logger LOGGER = Logger.getLogger(BeanManagedTx.class.getName());
    
    @PersistenceContext
    EntityManager em;
    
    @Resource
    UserTransaction tx;
    
    public <R> R applyWithTransaction(Function<EntityManager, R> function) throws
            // thrown by tx.begin():
            NotSupportedException, SystemException,
            // thrown by tx.commit():
            RollbackException, HeuristicMixedException, HeuristicRollbackException
    {
        
        tx.begin();
        
        final R result;
        
        try {
            result = function.apply(em);
        }
        catch (Exception e1) {
            try {
                tx.rollback();
            }
            catch (IllegalStateException | SecurityException | SystemException e2) {
                LOGGER.log(Level.WARNING, "Caught and consumed an exception while trying to rollback transaction:", e2);
            }
            throw e1;
        }
        
        tx.commit();
        
        return result;
    }
    
    public void acceptWithoutTransaction(Consumer<EntityManager> consumer) {
        consumer.accept(em);
    }
    
    public <R> R applyWithoutTransaction(Function<EntityManager, R> function) {
        return function.apply(em);
    }
}