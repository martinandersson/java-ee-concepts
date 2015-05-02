package com.martinandersson.javaee.cdi.producers.entitymanager.unsafe;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import javax.inject.Qualifier;

/**
 *
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Qualifier
@Retention(RUNTIME)
@Target({METHOD, FIELD, PARAMETER, TYPE})
public @interface UserDatabase
{
    // Empty
}