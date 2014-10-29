package com.martinandersson.javaee.jpa.entitymanagers.lib;

import com.martinandersson.javaee.jpa.entitymanagers.EntityManagerExposer;
import java.util.function.Function;
import java.util.logging.Logger;
import javax.ejb.LocalBean;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

/**
 * A {@code @Stateful} EJB that uses a container-managed entity manager with
 * extended persistence context.<p>
 * 
 * This EJB use default container-managed transactions.<p>
 
 If a public method is invoked and the thread has a transaction bound to it,
 the transaction is used. Otherwise, a new transaction is created upon
 invocation and committed when the method apply end.<p>
 * 
 * The persistence context survive transaction boundaries and will be closed
 * only when the bean is destroyed/removed.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Stateful
@LocalBean
public class StatefulWithExtended1 implements EntityManagerExposer
{
    private static final Logger LOGGER = Logger.getLogger(StatefulWithExtended1.class.getName());
    
    @PersistenceContext(type = PersistenceContextType.EXTENDED)
    EntityManager em;
    
    @Remove
    public void remove() {}

    /**
     * {@inheritDoc}
     */
    @Override
    public <R> R apply(Function<EntityManager, R> entityManagerFunction) {
        return entityManagerFunction.apply(em);
    }
}