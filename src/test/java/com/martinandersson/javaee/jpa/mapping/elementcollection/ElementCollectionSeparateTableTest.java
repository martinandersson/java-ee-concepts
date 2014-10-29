package com.martinandersson.javaee.jpa.mapping.elementcollection;

import com.martinandersson.javaee.jpa.mapping.elementcollection.lib.Person;
import com.martinandersson.javaee.jpa.mapping.elementcollection.lib.Repository;
import com.martinandersson.javaee.resources.SchemaGenerationStrategy;
import com.martinandersson.javaee.utils.Deployments;
import javax.ejb.EJB;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @see com.martinandersson.javaee.jpa.mapping.elementcollection
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
public class ElementCollectionSeparateTableTest
{
    @Deployment
    private static Archive<?> buildArchive() {
        return Deployments.buildPersistenceArchive(
                SchemaGenerationStrategy.DROP_CREATE,
                ElementCollectionSeparateTableTest.class,
                Person.class,
                Repository.class);
    }
    
    @EJB
    Repository persons;
    
    @Test
    public void collectionInSeparateTable() {
        Person created = new Person("Some nick", "Another nick");
        persons.persist(created);
        
        final long id = created.getId();
        assertTrue(id > 0L);
        
        persons.clearCaches();
        
        Person found = persons.findById(Person.class, id);
        
        // This will also do equality check of all the nicknames of Person:
        assertEquals("Person actually created",
                created, found);
    }
}