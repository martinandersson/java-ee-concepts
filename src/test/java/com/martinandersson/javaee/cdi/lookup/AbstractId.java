package com.martinandersson.javaee.cdi.lookup;

import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractId
{
    private static final AtomicLong SEQ = new AtomicLong();
    
    private final long id;
    
    public AbstractId() {
        id = SEQ.incrementAndGet();
    }
    
    public long getId() {
        return id;
    }
}