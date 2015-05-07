package com.martinandersson.javaee.jpa.mapping.elementcollection.lib;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.Table;

/**
 * Is a JPA entity with an unannotated {@code Set<String>} attribute.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Entity
@NamedEntityGraph(attributeNodes = @NamedAttributeNode("producers")) // <-- see comment in Repository.findById(Class, long)
@Table(schema="JPA_MAPPING_ELEMENTCOLLECTION")
public class Song
{
    @Id
    @GeneratedValue
    private long id;
    
    private Set<String> producers;
    
    protected Song() {
        
    }
    
    public Song(String... producers) {
        this.producers = new HashSet<>(Arrays.asList(producers));
    }
    
    public long getId() {
        return id;
    }
    
    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof Song ?
                /*
                 * Doing producers here only because we're in a test environment.
                 * A good design for Java EE apps would otherwise be to only
                 * rely on id and possibly type too.
                 */
                id == ((Song) obj).id && Objects.equals(producers, ((Song) obj).producers) :
                false;
    }
}