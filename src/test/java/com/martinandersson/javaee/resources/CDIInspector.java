package com.martinandersson.javaee.resources;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AfterTypeDiscovery;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBeanAttributes;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.inject.spi.ProcessManagedBean;
import javax.enterprise.inject.spi.ProcessObserverMethod;
import javax.enterprise.inject.spi.ProcessProducer;
import javax.enterprise.inject.spi.ProcessProducerField;
import javax.enterprise.inject.spi.ProcessProducerMethod;
import javax.enterprise.inject.spi.ProcessSessionBean;
import javax.enterprise.inject.spi.Producer;

/**
 * This CDI extension shall inspect the CDI container's life cycle, injection
 * points and injection targets, discovered beans, producer fields- and methods,
 * and finally discovered CDI event observer methods.<p>
 * 
 * The CDI inspector, which may be injected, also offer a utility method for
 * retrieval of the calling thread's active CDI scopes.<p>
 * 
 * If it isn't apparent already, the purpose is for sheer ease of debugging and
 * to get a finer view into the deployment process of a CDI bean archive.<p>
 * 
 * Note that "to inspect" literally mean to print log statements on {@code
 * Level.SEVERE} (probably always in effect) but with a custom level name "____"
 * (to better find the log entries).<p>
 * 
 * Also note that most logging statements apply only for types in the
 * "com.martinandersson" namespace. Most other types are ignored.
 * 
 * @see com.martinandersson.javaee.utils.Deployments#installCDIInspector(Object)
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class CDIInspector implements Extension // <-- only a marker interface
{
    /*
     *  --------
     * | STATIC |
     *  --------
     */
    
    private static final Logger LOGGER = Logger.getLogger("CDI Inspector");
    
    private static final Level CUSTOM = new Level("____", Level.SEVERE.intValue(), null){};
    
    private static final String FILTER = "com.martinandersson";
    
    
    
    /*
     *  --------------
     * | EXTERNAL API |
     *  --------------
     */
    
    /**
     * Get a {@code List} of all the active scopes that the calling thread may
     * use.
     * 
     * @return a list of all active scopes, may be empty
     */
    public List<Class<? extends Annotation>> getAllActiveScopes() {
        Class<? extends Annotation>[] scopes = (Class<? extends Annotation>[]) new Class[]{
            javax.enterprise.context.Dependent.class,
            javax.enterprise.context.RequestScoped.class,
            javax.enterprise.context.SessionScoped.class,
            javax.enterprise.context.ConversationScoped.class,
            javax.enterprise.context.ApplicationScoped.class,
            javax.inject.Singleton.class
        };
        
        final List<Class<? extends Annotation>> list = new ArrayList<>();
        
        BeanManager manager = CDI.current().getBeanManager();
        
        for (Class<? extends Annotation> s : scopes) {
            try {
                manager.getContext(s);
                list.add(s);
            }
            catch (ContextNotActiveException e) {
                // erm.. not active.
            }
        }
        
        return list;
    }
    
    
    
    /*
     *  ----------------
     * | PRE PROCESSING |
     *  ----------------
     */
    
    /**
     * {@linkplain BeforeBeanDiscovery JavaDoc} of BeforeBeanDiscovery says:
     * <pre>{@code 
     *     "This event type is thrown by the container before the bean discovery
     *      process begins."
     * }</pre>
     * 
     * @param ignored the event (provided by the CDI container)
     */
    private void doBeforeBeanDiscovery(@Observes BeforeBeanDiscovery ignored) {
        log("Active CDI provider", CDI.current());
        log("Type discovery process is about to begin.");
    }
    
    
    
    /*
     *  ----------------
     * | TYPE DISCOVERY |
     *  ----------------
     */
    
    /**
     * {@linkplain ProcessAnnotatedType JavaDoc} of ProcessAnnotatedType says:
     * <pre>{@code 
     *     "The container fires an event of this type for each Java class or
     *      interface it discovers in a bean archive, before it reads the
     *      declared annotations."
     * }</pre>
     * 
     * @param <X> class being annotated
     * 
     * @param event the event (provided by the CDI container)
     */
    private <X> void doProcessAnnotatedType(@Observes ProcessAnnotatedType<X> event) {
        applyFilter(event.getAnnotatedType(), type -> {
            log("DISCOVERED annotated type..");
            log("Annotated type", type);
            log("Annotation(s)",  type.getAnnotations());
        });
    }
    
    /**
     * {@linkplain AfterTypeDiscovery JavaDoc} of AfterTypeDiscovery says:
     * <pre>{@code 
     *     "This event type is thrown by the container after type discovery is
     *      complete."
     * }</pre>
     * 
     * @param event the event (provided by the CDI container)
     */
    private void doAfterTypeDiscovery(@Observes AfterTypeDiscovery event) {
        log("Type discovery process finished.");
        log("Enabled alternatives", event.getAlternatives());
        log("Enabled decorators",   event.getDecorators());
        log("Enabled interceptors", event.getInterceptors());
    }
    
    
    
    /*
     *  -----------
     * | INJECTION |
     *  -----------
     */
    
    /**
     * {@linkplain ProcessInjectionPoint JavaDoc} of ProcessInjectionPoint says:
     * <pre>{@code 
     *     "The container fires an event of this type for every injection point
     *      of every Java EE component class supporting injection that may be
     *      instantiated by the container at runtime, including every managed
     *      bean declared using javax.annotation.ManagedBean, EJB session or
     *      message-driven bean, enabled bean, enabled interceptor or enabled
     *      decorator."
     * }</pre>
     * 
     * @param <T> declared type of the injection point
     * @param <X> bean class of the bean that declares the injection point
     * 
     * @param event the event (provided by the CDI container)
     */
    private <T, X> void doProcessInjectionPoint(@Observes ProcessInjectionPoint<T, X> event) { // <-- also grabbing EventMetadata makes WildFly crash
        final InjectionPoint point = event.getInjectionPoint();
        
        Bean<?> bean = point.getBean();
        Annotated annotated = point.getAnnotated();
        Member member = point.getMember();
        
        applyFilter(() -> {
            log("DISCOVERED an injection point..");
            log("Qualifier(s)", point.getQualifiers());
            log("Bean",         bean);
            log("Annotated",    annotated); // <-- Java EE thing
            log("Member",       member);    // <-- Java SE thing
        }, bean, annotated, member);
    }
    
    /**
     * {@linkplain ProcessInjectionTarget JavaDoc} of ProcessInjectionTarget
     * says:
     * <pre>{@code 
     *     "The container fires an event of this type for every Java EE
     *      component class supporting injection that may be instantiated by the
     *      container at runtime, including every managed bean declared using
     *      javax.annotation.ManagedBean, EJB session or message-driven bean,
     *      enabled bean, enabled interceptor or enabled decorator."
     * }</pre>
     * 
     * @param <X> managed bean class, session bean class or Java EE component
     *        class supporting injection
     * 
     * @param event the event (provided by the CDI container)
     */
    private <X> void doProcessInjectionTarget(@Observes ProcessInjectionTarget<X> event) {
        final AnnotatedType<X> annotated = event.getAnnotatedType();
        
        applyFilter(() -> {
            log("DISCOVERED an injection target..");
            log("Annotated type", annotated);
            log("Annotation(s)",  annotated.getAnnotations());
        }, annotated);
    }
    
    
    
    /*
     *  -----------------
     * | BEAN PROCESSING |
     *  -----------------
     */
    
    /**
     * {@linkplain ProcessProducer JavaDoc} of ProcessProducer says:
     * <pre>{@code 
     *     "The container fires an event of this type for each producer method
     *      or field of each enabled bean, including resources."
     * }</pre>
     * 
     * @param <T> the bean class of the bean that declares the producer method or field
     * @param <X> the return type of the producer method or the type of the producer field
     * 
     * @param event the event (provided by the CDI container)
     */
    private <T, X> void doProcessProducer(@Observes ProcessProducer<T, X> event) {
        AnnotatedMember<T> member = event.getAnnotatedMember();
        Producer<X> producer = event.getProducer();
        
        applyFilter(() -> {
            log("DISCOVERED producer method- or field..");
            log("Member",                 member);
            log("Member's annotation(s)", member.getAnnotations());
            log("Producer",               producer);
        }, member, producer);
    }
    
    /**
     * {@linkplain ProcessBeanAttributes JavaDoc} of ProcessBeanAttributes says:
     * <pre>{@code 
     *     "The container fires an event of this type for each enabled bean,
     *      interceptor or decorator deployed in a bean archive before
     *      registering the Bean object."
     * }</pre>
     * 
     * @param <T> class of the bean
     * 
     * @param event the event (provided by the CDI container)
     */
    private <T> void doProcessBeanAttributes(@Observes ProcessBeanAttributes<T> event) {
        final Annotated annotated = event.getAnnotated();
        final BeanAttributes<T> attributes = event.getBeanAttributes();
        
        applyFilter(() -> {
            log("About to REGISTER a managed bean..");
            log("Annotated",      annotated);
            log("Annotation(s)",  annotated.getAnnotations());
            log("Name",           attributes.getName());
            log("Type(s)",        attributes.getTypes());
            log("Scope",          attributes.getScope());
            log("Qualifier(s)",   attributes.getQualifiers());
            log("Stereotype(s)",  attributes.getStereotypes()); // <-- Stereotype is an aggregation of other annotations
            log("Is alternative", attributes.isAlternative());
        }, annotated, attributes);
    }
    
    /**
     * {@linkplain ProcessManagedBean JavaDoc} of ProcessManagedBean says:
     * <pre>{@code 
     *     "The container fires an event of this type for each enabled managed
     *      bean, before registering the Bean object."
     * }</pre>
     * 
     * @param <X> class of the bean
     * 
     * @param event the event (provided by the CDI container)
     */
    private <X> void doProcessManagedBean(@Observes ProcessManagedBean<X> event) {
        // no-op (effectively inspected in doProcessBeanAttributes)
    }
    
    /**
     * {@linkplain ProcessSessionBean JavaDoc} of ProcessSessionBean says:
     * <pre>{@code 
     *     "The container fires an event of this type for each enabled session
     *      bean, before registering the Bean object."
     * }</pre>
     * 
     * @param <X> class of the bean
     * 
     * @param event the event (provided by the CDI container)
     */
    private <X> void doProcessSessionBean(@Observes ProcessSessionBean<X> event) {
        // no-op (effectively inspected in doProcessBeanAttributes)
    }
    
    /**
     * {@linkplain ProcessProducerMethod JavaDoc} of ProcessProducerMethod says:
     * <pre>{@code 
     *     "The container fires an event of this type for each enabled producer
     *      method, before registering the Bean object."
     * }</pre>
     * 
     * @param <T> return type of the producer method
     * @param <X> class of the bean declaring the producer method
     * 
     * @param event the event (provided by the CDI container)
     */
    private <T, X> void doProcessProducerMethod(@Observes ProcessProducerMethod<T, X> event) {
        applyFilter(() -> {
            log("Warning. Inspection of a Producer Method hasn't been written yet.".toUpperCase());
        }, event.getAnnotated());
    }
    
    /**
     * {@linkplain ProcessProducerField JavaDoc} of ProcessProducerField says:
     * <pre>{@code 
     *     "The container fires an event of this type for each enabled producer
     *      field, before registering the Bean object. Resources are considered
     *      to be producer fields."
     * }</pre>
     * 
     * @param <T> type of the producer field
     * @param <X> class of the bean declaring the producer field
     * 
     * @param event the event (provided by the CDI container)
     */
    private <T, X> void doProcessProducerField(@Observes ProcessProducerField<T, X> event) {
        applyFilter(() -> {
            log("Warning. Inspection of a Producer Field hasn't been written yet.".toUpperCase());
        }, event.getAnnotated());
    }
    
    
    
    /*
     *  -----------------
     * | POST PROCESSING |
     *  -----------------
     */
    
    /**
     * {@linkplain AfterBeanDiscovery JavaDoc} of AfterBeanDiscovery says:
     * <pre>{@code 
     *     "The event type of the second event fired by the container when it
     *      has fully completed the bean discovery process, validated that there
     *      are no definition errors relating to the discovered beans, and
     *      registered Bean and ObserverMethod objects for the discovered beans,
     *      but before detecting deployment problems."
     * }</pre>
     * 
     * @param event the event (provided by the CDI container)
     */
    private void doAfterBeanDiscovery(@Observes AfterBeanDiscovery event) {
        log("Bean discovery finished.");
    }
    
    /**
     * {@linkplain AfterDeploymentValidation JavaDoc} of
     * AfterDeploymentValidation says:
     * <pre>{@code 
     *     "The event type of the third event fired by the container after it
     *      has validated that there are no deployment problems and before
     *      creating contexts or processing requests."
     * }</pre>
     * 
     * @param event the event (provided by the CDI container)
     */
    private void doAfterDeploymentValidation(@Observes AfterDeploymentValidation event) {
        log("Deployed archive has no apparent problems. Ready to setup contexts.");
    }
    
    
    /**
     * {@linkplain BeforeShutdown JavaDoc} of BeforeShutdown says:
     * <pre>{@code 
     *     "The type of the final event the container fires after it has
     *      finished processing requests and destroyed all contexts."
     * }</pre>
     * 
     * @param event the event (provided by the CDI container)
     */
    private void doBeforeShutdown(@Observes BeforeShutdown event) {
        log("All contexts destroyed. R.I.P.");
    }
    
    
    
    /*
     *  ------------------
     * | OBSERVER METHODS |
     *  ------------------
     */
    
    /**
     * {@linkplain ProcessObserverMethod JavaDoc} of ProcessObserverMethod says:
     * <pre>{@code 
     *     "The container fires an event of this type for each observer method
     *      of each enabled bean, before registering the ObserverMethod object."
     * }</pre>
     * 
     * @param <T> type of the [CDI] event being observed
     * @param <X> bean type containing the observer method
     * 
     * @param event the event (provided by the CDI container)
     */
    private <T, X> void doProcessObserverMethod(@Observes ProcessObserverMethod<T, X> event) {
        applyFilter(() -> {
            log("Warning. Inspection of an Observer Method hasn't been written yet.".toUpperCase());
        }, event.getAnnotatedMethod().getDeclaringType());
    }
    
    
    
    /*
     *  --------------
     * | INTERNAL API |
     *  --------------
     */
    
    // TODO: Make all these shitty implementations become good implementations:
    
    private void applyFilter(Runnable logic, Object... args) {
        if (args.length > 0 &&
            Stream.of(args).map(String::valueOf).anyMatch(s -> s.contains(FILTER)))
            logic.run();
    }
    
    private void applyFilter(AnnotatedType<?> type, Consumer<Class<?>> consume) {
        Class<?> __type = type.getJavaClass();
        if (__type.getPackage().getName().startsWith(FILTER))
            consume.accept(__type);
    }
    
    private void log(String entry) {
        LOGGER.log(CUSTOM, entry);
        
    }
    
    private void log(String... entry) {
        Stream.of(entry).forEach(this::log);
    }
    
    private void log(Object entry) {
        log(String.valueOf(entry));
    }
    
    private void log(Supplier<String> entry) {
        LOGGER.log(CUSTOM, entry);
    }
    
    private void log(String prefix, Object item) {
        log(() -> prefix + ": " + item);
    }
    
    private <T> void log(String prefix, T... items) {
        log(prefix, Arrays.asList(items));
    }
    
    /**
     * Coming in year 2024: JavaDoc.
     */
    private void log(String prefix, Collection<?> items) {
        log(() -> {
            final String s;
            
            if (items.isEmpty()) {
                s = prefix + ": none :'(";
            }
            else if (items.size() == 1) {
                s = prefix + ": " + items.stream().findAny().get();
            }
            else {
                s = prefix + " (" + items.size() + "):\n\t" +
                        items.stream().map(Object::toString).collect(Collectors.joining("\n\t"));
            }
            
            return s;
        });
    }
}