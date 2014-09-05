package com.martinandersson.javaee.cdi.packaging.calculators;

import java.util.stream.IntStream;

/**
 * World's most simplest calculator, supports one function: summation of
 * values.<p>
 * 
 * This class uses no annotations.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class CalculatorUnAnnotated {
    public long sum(int... values) {
        return IntStream.of(values).sum();
    }
}