package com.martinandersson.javaee.cdi.resolution;

import java.util.stream.LongStream;

/**
 * The {@code SimpleCalculator} does not implement an interface.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
class SimpleCalculator {
    public long sum(long... values) {
        return LongStream.of(values).sum();
    }
}