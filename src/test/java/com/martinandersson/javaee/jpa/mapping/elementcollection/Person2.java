package com.martinandersson.javaee.jpa.mapping.elementcollection;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Is a JPA entity with a {@code Set<String>} field annotated
 * {@code @ElementCollection}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Entity
@Table(name = "PERSON_2", schema="JPA_MAPPING_ELEMENTCOLLECTION")
public class Person2
{
    @Id
    @GeneratedValue
    private long id;
    
    // Make the collection go to a separate table:
    @ElementCollection
    // Override table name and where to put the table:
    @CollectionTable(name= "PERSON_2_NICKNAMES", schema = "JPA_MAPPING_ELEMENTCOLLECTION")
    private Set<String> nicknames;
    
    protected Person2() {
        
    }
    
    public Person2(String... nicknames) {
        this.nicknames = new HashSet<>(Arrays.asList(nicknames));
    }
}