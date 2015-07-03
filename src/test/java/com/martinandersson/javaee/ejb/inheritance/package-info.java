/**
 * 1. Session bean inheritance. Only for code reuse. So what happens if I let
 * session bean A extend session bean B? Which implementation do I get when
 * looking up the no-interface view of A?
 * 
 * 2. EJB section "4.9.2.1 Session Bean Superclasses" says that the view of a
 * superclass is not inherited unless explicitly put in the interface
 * declaration clause by the subclass. So what happens if client to call a
 * superclass method? Is the call not managed, will it not be directed through a
 * proxy? Will it crash?
 * 
 * 2. Using CDI:s @Specializes on EJB:s are permitted (CDI 1.2, section 3.2.4).
 * So must the client code use @Inject? @EJB may find the "vetoed" bean? Note
 * that @Specializes is a CDI construct!
 * FINDING GF: The "vetoed" bean is not discoverable any more even if using
 * @EJB. Test will crash during RUNTIME if the vetoed bean is tried to be called.
 * There will be a NPE deep down in the Weld layer and the client see a
 * EJBException.
 */
package com.martinandersson.javaee.ejb.inheritance;
