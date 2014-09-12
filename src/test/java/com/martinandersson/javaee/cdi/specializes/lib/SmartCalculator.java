package com.martinandersson.javaee.cdi.specializes.lib;

import java.util.stream.LongStream;
import javax.enterprise.inject.Specializes;

/**
 * A better alternative to {@linkplain StupidCalculator}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Specializes
public class SmartCalculator extends StupidCalculator
{
    /**
     * A fast summing function.
     * 
     * @param values to sum
     * @return sum
     */
    @Override
    public long sum(long... values) {
        return LongStream.of(values).sum();
    }
}