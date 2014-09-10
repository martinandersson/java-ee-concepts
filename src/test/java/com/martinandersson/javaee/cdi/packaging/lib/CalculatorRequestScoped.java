package com.martinandersson.javaee.cdi.packaging.lib;

import java.util.stream.IntStream;
import javax.enterprise.context.RequestScoped;

/**
 * World's most simplest calculator, supports one function: summation of
 * values.<p>
 * 
 * This class has <u>one</u> provided annotation: {@linkplain
 * javax.enterprise.context.RequestScoped @javax.enterprise.context.RequestScoped}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RequestScoped
public class CalculatorRequestScoped {
    public long sum(int... values) {
        return IntStream.of(values).sum();
    }
}