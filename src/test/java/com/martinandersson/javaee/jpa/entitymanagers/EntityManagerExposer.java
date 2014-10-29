package com.martinandersson.javaee.jpa.entitymanagers;

import java.util.function.Consumer;
import java.util.function.Function;
import javax.persistence.EntityManager;

/**
 * A bean that implements this interface has an entity manager ready to be
 * exposed for some serious testing =)<p>
 * 
 * Yes, I originally called this type {@code EntityManagerPimp}.<p>
 * 
 * 
 * 
 * <h3>Is this interface used as an EJB business interface?</h3>
 * 
 * By default in Java EE, an EJB that implements an interface (except
 * {@code Serializable} and something more?) will loose his no-interface view
 * and client code will become unable to declare and use a dependency of the EJB
 * bean type alone.<p>
 * 
 * The implemented interface is being looked at as a "business interface" (a
 * {@code @Local} one to be more precise) and it is the interface type that must
 * be used as the type of the injection point.<p>
 * 
 * If the bean whish to implement this interface and keep exposing a
 * no-interface view to client code, then the bean must be annotated
 * {@code @LocalBean}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public interface EntityManagerExposer
{
    /**
     * Will execute the provided test logic on the inside of the bean using the
     * bean's entity manager.<p>
     * 
     * Default implementation uses {@link #apply(Function)}.
     * 
     * @param entityManagerConsumer logic to be applied using the bean's entity
     *        manager
     */
    default void accept(Consumer<EntityManager> entityManagerConsumer) {
        apply(em -> { entityManagerConsumer.accept(em); return null; });
    }
    
    /**
     * Will execute the provided test logic on the inside of the bean using the
     * bean's entity manager.
     * 
     * @param <R> type of return value
     * @param entityManagerFunction logic to be applied using the bean's entity
     *        manager
     * 
     * @return whatever the function return
     * 
     */
    <R> R apply(Function<EntityManager, R> entityManagerFunction);
}