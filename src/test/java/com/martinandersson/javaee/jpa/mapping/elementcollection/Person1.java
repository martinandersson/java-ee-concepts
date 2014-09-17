package com.martinandersson.javaee.jpa.mapping.elementcollection;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Is a JPA entity with an unannotated {@code Set<String>} field.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Entity
@Table(name = "PERSON_1", schema="JPA_MAPPING_ELEMENTCOLLECTION")
public class Person1
{
    @Id
    @GeneratedValue
    private long id;
    
    private Set<String> nicknames;
    
    protected Person1() {
        
    }
    
    public Person1(String... nicknames) {
        this.nicknames = new HashSet<>(Arrays.asList(nicknames));
    }
}