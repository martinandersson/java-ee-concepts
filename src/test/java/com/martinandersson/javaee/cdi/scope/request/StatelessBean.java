package com.martinandersson.javaee.cdi.scope.request;

import java.util.concurrent.Future;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * A {@code @Stateless} bean.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Stateless
public class StatelessBean
{
    /*
     * Although possible without any logical errors, does it make sense to
     * inject client-specific state into a stateless bean here? I don't know.
     */
    @Inject
    RequestScopedBean bean;
    
    public int getIdOfNestedRequestedScopedBean() {
        return bean.getId();
    }
    
    @Asynchronous
    public Future<Integer> getIdOfNestedRequestedScopedBeanAsynchronously() {
        return new AsyncResult<>(bean.getId());
    }
}