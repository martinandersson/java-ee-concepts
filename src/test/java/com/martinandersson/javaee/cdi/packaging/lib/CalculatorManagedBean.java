package com.martinandersson.javaee.cdi.packaging.lib;

import java.util.stream.IntStream;
import javax.annotation.ManagedBean;

/**
 * World's most simplest calculator, supports one function: summation of
 * values.<p>
 * 
 * This class has <u>one</u> provided annotation: {@linkplain
 * javax.annotation.ManagedBean @javax.annotation.ManagedBean}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@ManagedBean
public class CalculatorManagedBean {
    public long sum(int... values) {
        return IntStream.of(values).sum();
    }
}