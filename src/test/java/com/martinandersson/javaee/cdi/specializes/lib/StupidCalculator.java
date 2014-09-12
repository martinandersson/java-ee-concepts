package com.martinandersson.javaee.cdi.specializes.lib;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Logger;
import java.util.stream.LongStream;
import javax.annotation.PostConstruct;
import javax.enterprise.event.Observes;

/**
 * Stupid calculator is a bad implementation of a calculator that some other
 * department of your company wrote. Now you're faced with all the
 * responsibility of a slow application and your customer phones in, throwing
 * bad words at you. Lack of internal support and lack of source code access
 * together with a packaging nightmare of your application makes it infeasible
 * to replace the class which would be the normal alternative. What to do?<p>
 * 
 * Use specialization.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class StupidCalculator
{
    private static final Logger LOGGER = Logger.getLogger(StupidCalculator.class.getName());
    
    public static final LongAdder OBSERVER_COUNTER = new LongAdder(),
                                  INSTANCE_COUNTER = new LongAdder();
    
    {
        LOGGER.info(() ->
                "instance created " +
                "(hash: " + System.identityHashCode(this) + ", bean class: " + getClass().getSimpleName() + ")");
        
        INSTANCE_COUNTER.increment();
    }
    
    
    
    /**
     * A slow summing function.
     * 
     * @param values to sum
     * @return sum
     */
    public long sum(long... values) {
        try {
            TimeUnit.SECONDS.sleep(1);
        }
        catch (InterruptedException e) {
            ;
        }
        
        return LongStream.of(values).sum();
    }
    
    
    @PostConstruct
    private void __postConstruct() {
        LOGGER.info(() ->
                "__postConstruct() called (hash: " + System.identityHashCode(this) + ", bean class: " + getClass().getSimpleName() + ")");
    }
    
    private void __instanceObserve(@Observes String text) {
        LOGGER.warning(() ->
                "__instanceObserve() called with text: " + text +
                " (hash: " + System.identityHashCode(this) + ", bean class: " + getClass().getSimpleName() + ")");
        
        OBSERVER_COUNTER.increment();
    }
    
    private static void __staticObserve(@Observes String text) {
        LOGGER.warning(() -> "staticObserve() called with text: " + text);
        OBSERVER_COUNTER.increment();
    }
}