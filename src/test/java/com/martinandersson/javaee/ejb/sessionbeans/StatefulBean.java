package com.martinandersson.javaee.ejb.sessionbeans;

import javax.ejb.Remove;

/**
 * For more info about {@code @Stateful} session beans, see JavaDoc of
 * {@linkplain #remove()} and a source-code comment written in the client code
 * of {@code TestDriver.__removeStatefulBeans()}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@javax.ejb.Stateful
public class StatefulBean extends AbstractSessionBean
{
    /**
     * JNDI lookup and @EJB injection causes a new @Stateful bean instance to be
     * created. The bean reference may be passed around in the system if need be
     * (for example put in the {@code HttpSession} object). Finally, when the
     * client is done using the @Stateful, he must tell the container so by
     * invoking a {@code @Remove} annotated method, such as this one. When this
     * method completes, the bean will be discarded.<p>
     * 
     * If the client never invoke this method, then the life of an idle bean
     * will linger on until a timeout happen which causes the server to discard
     * the bean. How long before that timeout happen is vendor-specific.<p>
     * 
     * Read more in a source-code comment written in the client code of {@code
     * TestDriver.__removeStatefulBeans()}.
     */
    @Remove
    public void remove() {
        // Work done.
    }
}