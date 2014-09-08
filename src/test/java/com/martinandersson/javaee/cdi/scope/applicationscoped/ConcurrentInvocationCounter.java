package com.martinandersson.javaee.cdi.scope.applicationscoped;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

/**
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@ApplicationScoped
public class ConcurrentInvocationCounter
{
    private static final LongAdder instances = new LongAdder(),
                                   logicalInstances = new LongAdder(),
                                   concurrentCalls = new LongAdder();
    
    private static final AtomicBoolean isSleeping = new AtomicBoolean();
    
    
    
    public static long getInstancesCreated() {
        return instances.sum();
    }
    
    public static long getLogicalInstancesCreated() {
        return logicalInstances.sum();
    }
    
    public static long getConcurrentCallsCount() {
        return concurrentCalls.sum();
    }
    
    
    
    public void sleepOneSecond()
    {
        if (isSleeping.getAndSet(true)) {
            concurrentCalls.increment();
            return;
        }
        
        try {
            TimeUnit.SECONDS.sleep(1);
        }
        catch (InterruptedException e) {
            Logger.getLogger(ConcurrentInvocationCounter.class.getName())
                    .warning("Interrupted from sleep.");
        }
        finally {
            isSleeping.set(false);
        }
    }
    
    
    
    {
        instances.increment();
    }
    
    @PostConstruct
    private void __postConstruct() {
        logicalInstances.increment();
    }
}