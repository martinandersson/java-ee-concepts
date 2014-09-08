package com.martinandersson.javaee.cdi.qualifiers.caloric;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import javax.inject.Qualifier;

/**
 * All unhealthy food are extinguished using this qualifier.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Qualifier // <-- Note that a qualifier must be annotated @Qualifier
@Retention(RUNTIME)
@Target({METHOD, FIELD, PARAMETER, TYPE})
public @interface Unhealthy {
    // No members
}