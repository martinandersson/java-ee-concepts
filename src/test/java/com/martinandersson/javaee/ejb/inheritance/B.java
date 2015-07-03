package com.martinandersson.javaee.ejb.inheritance;

import javax.ejb.Stateless;

/**
 *
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Stateless
public class B extends A {
    @Override public String simpleName() {
        return "B";
    }
}