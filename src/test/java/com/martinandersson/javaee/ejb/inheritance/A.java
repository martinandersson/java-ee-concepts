package com.martinandersson.javaee.ejb.inheritance;

import javax.ejb.Stateless;

/**
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Stateless
public class A {
    public String simpleName() {
        return "A";
    }
}