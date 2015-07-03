package com.martinandersson.javaee.ejb.inheritance;

import javax.ejb.Stateless;
import javax.enterprise.inject.Specializes;

/**
 *
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Stateless
@Specializes
public class C extends A {
    @Override public String simpleName() {
        return "C";
    }
}