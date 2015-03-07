package com.martinandersson.javaee.cdi.lookup;

import java.io.Serializable;

public final class IdWrapper implements Serializable
{
    private static final long serialVersionUID = 1;
    
    private final long id;
    
    public IdWrapper(long id) {
        this.id = id;
    }
    
    public long get() {
        return id;
    }
}