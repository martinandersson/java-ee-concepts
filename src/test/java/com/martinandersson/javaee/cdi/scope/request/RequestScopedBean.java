package com.martinandersson.javaee.cdi.scope.request;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * A {@code @RequestScoped} bean.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RequestScoped
public class RequestScopedBean
{
    @Inject
    RequestScopedBean nestedBean;
    
    /**
     * The Id is computed using the memory based hash code of the instance;
     * which is the default implementation for Object.hashCode().<p>
     * 
     * However, the hash and therefore this Id is not gauranteed to be unique.
     * The alternative would be for us to use a {@code AtomicInteger} or any
     * other similar construct for generating a true Id. However, these
     * constructs inevitably lower the degree of parallelism which is why I
     * choose the hash based version. If problems arise, lower the amount of
     * beans created, force them to stay in-memory longer, write another Id
     * implementation, or write another bean: for example one that crash if
     * concurrent requests are made (if so, simulate work in-bean).
     * 
     * @return id of the bean
     */
    public int getId() {
        /*
         * So why not use Object.hashCode() or make the TestDriver call that
         * method directly? I actually tried the latter option but discovered
         * that sometimes, the call wasn't routed to the bean by GlassFish
         * (didn't test WildFly) and produced what seemed to be an arbitrary
         * number which unnecessarily failed the test. Hence the explicit
         * naming of the Id-method and this alternative implementation.
         */
        return System.identityHashCode(this);
    }
    
    public int getIdOfNestedRequestedScopedBean() {
        return nestedBean.getId();
    }
}