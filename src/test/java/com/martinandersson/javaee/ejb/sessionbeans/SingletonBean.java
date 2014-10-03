package com.martinandersson.javaee.ejb.sessionbeans;

import javax.inject.Singleton;

/**
 * This bean is annotated {@linkplain Singleton @Singleton} but has no other
 * annotations applied. Implicitly therefore, following default annotations are
 * in effect:
 * 
 * <ol>
 *   <li>{@code @ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)}</li>
 *   <li>{@code @Lock(LockType.WRITE)}</li>
 * </ol>
 * 
 * These settings will make the singleton serialize all access to the bean.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@javax.ejb.Singleton
public class SingletonBean extends AbstractSessionBean {
    // All business methods provided by AbstractSessionBean
}