package com.martinandersson.javaee.ejb.transactions;

import javax.ejb.Stateless;

/**
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Stateless
public class Derived1 extends Base1
{
    @Override
    public int foo() {
        return super.foo();
    }
}