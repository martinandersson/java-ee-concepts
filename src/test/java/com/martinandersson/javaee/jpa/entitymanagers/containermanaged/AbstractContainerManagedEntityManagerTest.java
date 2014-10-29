package com.martinandersson.javaee.jpa.entitymanagers.containermanaged;

import com.martinandersson.javaee.jpa.entitymanagers.AbstractJTAEntityManagerTest;
import javax.ejb.EJBException;
import javax.persistence.EntityManager;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This class has all tests that is applicable to container-managed entity
 * managers, independent of the persistence context's type of synchronization or
 * type of scope.<p>
 * 
 * A container-managed entity manager uses JTA transactions. Therefore, this
 * class extends {@linkplain AbstractJTAEntityManagerTest}.<p>
 * 
 * JPA 2.1, section "7.5 Controlling Transactions":
 * <pre>{@code
 * 
 *     A container-managed entity manager must be a JTA entity manager.
 * 
 * }</pre>
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
public abstract class AbstractContainerManagedEntityManagerTest extends AbstractJTAEntityManagerTest
{   
    /**
     * JPA 2.1, section "7.9.1 Container Responsibilities":
     * <pre>{@code
     * 
     *     The container must throw the IllegalStateException if the application
     *     calls EntityManager.close on a container-managed entity manager.
     * 
     * }</pre>
     * 
     * Makes sense. The container is the manager of the entity manager's life
     * cycle!<p>
     * 
     * Note that it makes no difference if the persistence context has been
     * initialized or not.
     * 
     * @throws Throwable hopefully
     */
    @Test(expected = IllegalStateException.class)
    public void containerManagedEntityManagerCanNotBeClosedByApplication() throws Throwable {
        try {
            getBeanBeingTested().accept(EntityManager::close);
        }
        catch (EJBException e) {
            throw e.getCause();
        }
    }
}