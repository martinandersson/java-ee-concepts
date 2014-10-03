package com.martinandersson.javaee.ejb.sessionbeans;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * This is a superclass of all session beans used in tests of the beans.<p>
 * 
 * EJB 3.2 specification, section "4.9.2.1 Session Bean Superclasses":
 * <pre>{@code
 * 
 *     For the purposes of processing a particular session bean class, all
 *     superclass processing is identical regardless of whether the superclasses
 *     are themselves session bean classes. In this regard, the use of session
 *     bean classes as superclasses merely represents a convenient use of
 *     implementation inheritance, but does not have component inheritance
 *     semantics.
 * 
 * }</pre>
 * 
 * EJB 3.2 specification, section "3.4.4 Session Bean's No-Interface View":
 * <pre>{@code
 * 
 *     Only public methods of the bean class and of any superclasses except
 *     java.lang.Object may be invoked through the no-interface view. Attempted
 *     invocations of methods with any other access modifiers via the
 *     no-interface view reference must result in the javax.ejb.EJBException.
 * 
 * }</pre>
 * 
 * EJB 3.2 specification, section "4.9.8 Session Bean's No-Interface View":
 * <pre>{@code
 * 
 *     All non-static public methods of the bean class and of any superclasses
 *     except java.lang.Object are exposed as business methods through the
 *     no-interface view.
 *     
 *     Note: This includes callback methods. [..] Therefore, it is recommended
 *     that all non-business methods be assigned an access type other than
 *     public.
 *     
 *     [..]
 *     
 *     Only private methods of the bean class and any superclasses except
 *     java.lang.Object may be declared final.
 * 
 * }</pre>
 * 
 * Note that one must not mark a method final, unless it is a private method.
 * But private methods are non-inherited members of a class. So marking a
 * private method final has no effect.<p>
 * 
 * As a summary then, all methods of a session bean must not be final. Business
 * methods of the bean which clients use must be public. Callback methods meant
 * to be used by the container only should not be public.<p>
 * 
 * The previous quotes are all located in sections that explicitly talk about
 * the "no-interface view", i.e, when the bean class has no implements clause or
 * the bean class implement one or more business interfaces but the bean class
 * is explicitly annotated {@code @LocalBean} (see sections 4.9.7 and 4.9.8).
 * The no-interface view makes it possible for us to inject the bean using the
 * class type only.<p>
 * 
 * I picked these quotes only because they explicitly speak of "superclasses",
 * and therefore how these rules relate to {@code AbstractSessionBean}. Do note
 * however that these rules are the same for bean classes that has no superclass
 * (except {@code Object} of course). See section "4.9.6 Business Methods".
 * 
 * 
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public abstract class AbstractSessionBean {
    
    private static final Logger LOGGER = Logger.getLogger(AbstractSessionBean.class.getName());
    
    private final int ID = System.identityHashCode(this);
    
    private final AtomicBoolean isExecuting = new AtomicBoolean();
    
    
    /*
     *  ------------------
     * | BUSINESS METHODS |
     *  ------------------
     */
    
    public int getId() {
        return ID;
    }
    
    public int[] getIdAndThatOf(AbstractSessionBean bean) {
        return new int[]{ ID, bean.getId() };
    }
    
    /**
     * This method sleep 100 milliseconds before retrieving the ID of this bean.
     * If concurrent access is detected, a {@code ConcurrentAccessException} is
     * thrown.<p>
     * 
     * EJB proxy references allow concurrent invocations but serialize all calls
     * to the real EJB instance with one minor exception: singletons that use
     * {@code ConcurrencyManagementType.BEAN}.
     * 
     * @throws InterruptedException if interrupted during sleep
     */
    public int sleepAndGetId() throws InterruptedException {
        if (isExecuting.getAndSet(true)) {
            throw new javax.ejb.ConcurrentAccessException("We assume no @Singleton with READ lock or bean managed concurrency exist.");
        }
        
        try {
            TimeUnit.MILLISECONDS.sleep(100);
            return ID;
        }
        finally {
            isExecuting.set(false);
        }
    }
    
    
    
    /*
     *  ------------
     * | LIFE CYCLE |
     *  ------------
     */
    
    @PostConstruct
    private void __postConstruct() {
        LOGGER.info(() -> getClass().getSimpleName() + " constructed. Id: " + ID);
    }
    
    @PreDestroy
    private void __preDestroy() {
        LOGGER.info(() -> getClass().getSimpleName() + " going down. Id: " + ID);
    }
}