package com.martinandersson.javaee.jpa.entitymanagers;

import javax.ejb.EJBException;
import javax.persistence.EntityManager;
import org.junit.Test;

/**
 * This class has all tests that is applicable to entity managers that use JTA
 * transactions, whether they be container-managed or application-managed.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public abstract class AbstractJTAEntityManagerTest
{
    /**
     * Returns the target bean that offer an exposed {@code EntityManager}.<p>
     * 
     * One must not assume anything about the state of the entity manager's
     * persistence context after this method has been called.
     * 
     * @return the target bean that offer an exposed {@code EntityManager}
     */
    protected abstract EntityManagerExposer getBeanBeingTested();
    
    /**
     * Entity manager is a "JTA entity manager". Use of {@code
     * EntityManager.getTransaction()} is forbidden.
     * 
     * @throws Throwable hopefully
     */
    @Test(expected = IllegalStateException.class)
    public void JTAEntityManagerMustNotUseResourceLocalTransaction() throws Throwable {
        try {
            getBeanBeingTested().accept(EntityManager::getTransaction);
        }
        catch (EJBException e) {
            throw e.getCause();
        }
    }
}