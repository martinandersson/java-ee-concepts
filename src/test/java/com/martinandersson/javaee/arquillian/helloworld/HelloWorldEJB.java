package com.martinandersson.javaee.arquillian.helloworld;

import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * This {@code @Stateless} EJB will uppercase a provided String.<p>
 * 
 * This EJB has no reason being an EJB which is an "expensive" component to use.
 * Instead, it would be better suited as a @Dependent CDI bean: meaning no
 * annotations at all (note 1). However, we're using a heavy-duty EJB here only
 * as proof of concept.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.SUPPORTS) // <-- note 2
public class HelloWorldEJB
{
    private final static Logger LOGGER = Logger.getLogger(HelloWorldEJB.class.getName());
    
    
    /**
     * Will uppercase a provided String.
     * 
     * @param what the thing to uppercase
     * 
     * @return {@code what.toUpperCase()}
     */
    public String toUpperCase(String what)
    {
        /*
         * This is a contrived example. Serious code, for example making a
         * username lower- or uppercased before transmission to a database,
         * should use toUpperCase(Locale.ROOT).
         */
        
        return what.toUpperCase();
    }
    
    
    /*
     * Usually, Java EE annotations apply only to business methods. A business
     * method of an EJB must have public access. However, life cycle annotations
     * like the ones below is an exception to the rule.
     */
    
    @PostConstruct
    private void logConstruction() {
        LOGGER.info(() ->
                HelloWorldEJB.class.getSimpleName() + ": Instance was just constructed. My hash: " + this.hashCode() + ".");
    }
    
    @PreDestroy
    private void logDesctruction() {
        LOGGER.info(() ->
                HelloWorldEJB.class.getSimpleName() + ": Instance will be killed. My hash: " + this.hashCode() + ".");
    }
}

/*
 * NOTE 1: Go ahead and try remove the annotations on this bean. There's one
 *         more thing you have to do before the test executes as "normal"
 *         without the provided annotations. Add this line to the @Deployment
 *         annotated method in HelloWorldTest.java:
 *         
 *             jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
 * 
 *         What this line do is to add an empty 0-byte file in the META-INF
 *         directory. Read more here:
 *         
 *             http://docs.oracle.com/javaee/7/tutorial/doc/cdi-adv001.htm
 * 
 * NOTE 2: If this annotation was left out, it would default to:
 *         "@TransactionAttribute(TransactionAttributeType.REQUIRED)". This file
 *         has no purpose of explaining transactions. We do talk about that a
 *         bit in the "persistence"-package.
 */