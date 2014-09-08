package com.martinandersson.javaee.cdi.qualifiers.caloric;

import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import javax.inject.Qualifier;

/**
 * All healthy food are extinguished using this qualifier.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Qualifier // <-- Note that a qualifier must be annotated @Qualifier
@Retention(RUNTIME)
@Target({METHOD, FIELD, PARAMETER, TYPE})
public @interface Healthy {
    // No members
}