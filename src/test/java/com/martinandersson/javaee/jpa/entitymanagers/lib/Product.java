package com.martinandersson.javaee.jpa.entitymanagers.lib;

import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * A dumb {@code @Entity} what represents a product.<p>
 * 
 * Has only one field of interest: a name.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Entity
@Table(schema = "JPA_ENTITYMANAGERS")
public class Product
{
    public interface Fields {
        String NAME = "name";
    }
    
    @Id
    @GeneratedValue
    private long id;
    
    private String name;
    
    protected Product() {
        // Empty
    }
    
    public Product(String name) {
        setName(name);
    }
    
    public final long getId() {
        return id;
    }
    
    public final String getName() {
        return name;
    }
    
    public final void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        
        if (obj == null) {
            return false;
        }
        
        // Symmetry? Yes please.
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        
        Product that = (Product) obj;
        
        return this.id == that.id &&
               // I would normally be happy with just the id, at least one test need name equality though..
               Objects.equals(this.name, that.name);
    }

    @Override
    public String toString() {
        return new StringBuilder(Product.class.getSimpleName())
                .append("[")
                  .append("id=").append(id)
                  .append(", name=").append(name)
                .append("]")
                .toString();
    }
}