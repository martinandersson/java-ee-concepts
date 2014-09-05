package com.martinandersson.javaee.cdi.scope.request;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * An {@code @ApplicationScoped} bean.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@ApplicationScoped
public class ApplicationScopedBean
{
    @Inject
    RequestScopedBean bean;
    
    public int getIdOfNestedRequestedScopedBean() {
        return bean.getId();
    }
}