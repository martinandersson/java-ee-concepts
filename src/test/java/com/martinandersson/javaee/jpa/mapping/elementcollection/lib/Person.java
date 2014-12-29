package com.martinandersson.javaee.jpa.mapping.elementcollection.lib;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.Table;

/**
 * Is a JPA entity with a {@code Set<String>} attribute annotated
 * {@code @ElementCollection}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Entity
@NamedEntityGraph(attributeNodes = @NamedAttributeNode("nicknames")) // <-- see comment in Repository.findById()
@Table(schema="JPA_MAPPING_ELEMENTCOLLECTION")
public class Person
{
    @Id
    @GeneratedValue
    private long id;
    
    @ElementCollection // <-- make the collection go to a separate table
    @CollectionTable(schema = "JPA_MAPPING_ELEMENTCOLLECTION") // <-- override where to put the table
    private Set<String> nicknames;
    
    protected Person() {
        // Empty
    }
    
    public Person(String... nicknames) {
        this.nicknames = new HashSet<>(Arrays.asList(nicknames));
    }
    
    public long getId() {
        return id;
    }
    
    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof Person ?
                /*
                 * Doing nicknames here only because we're in a test environment.
                 * A good design for Java EE apps would otherwise be to only
                 * rely on id and possibly type too.
                 */
                id == ((Person) obj).id && Objects.equals(nicknames, ((Person) obj).nicknames) :
                false;
    }
}