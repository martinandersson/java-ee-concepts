package com.martinandersson.javaee.jpa.mapping.orphanremoval;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 *
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@MappedSuperclass
public abstract class AbstractId
{
    @Id
    @GeneratedValue
    private long id;
    
    public long getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public boolean equals(Object other) {
        return this == other ||
                other instanceof AbstractId && this.id == ((AbstractId) other).id;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[id=" + id + "]";
    }
}