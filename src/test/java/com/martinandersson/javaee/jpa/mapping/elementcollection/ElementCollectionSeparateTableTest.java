package com.martinandersson.javaee.jpa.mapping.elementcollection;

import com.martinandersson.javaee.jpa.mapping.elementcollection.lib.Person;
import com.martinandersson.javaee.jpa.mapping.elementcollection.lib.Repository;
import com.martinandersson.javaee.resources.SchemaGenerationStrategy;
import com.martinandersson.javaee.utils.DeploymentBuilder;
import java.util.List;
import javax.ejb.EJB;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
        return new DeploymentBuilder(ElementCollectionSeparateTableTest.class)
                .addPersistenceXMLFile(SchemaGenerationStrategy.DROP_CREATE)
                .add(Person.class,
                     Repository.class)
                .build();
    }
    
    
    @EJB
    Repository persons;
    
    static long id;
    
    
    @Test
    @InSequence(1)
    public void test_collectionInSeparateTable() {
        Person created = new Person("Some nick", "Another nick");
        persons.persist(created);
        
        id = created.getId();
        assertTrue(id > 0L);
        
        persons.clearCaches();
        
        Person found = persons.findById(Person.class, id);
        
        // This will also do equality check of all the nicknames of Person:
        assertEquals("Person actually created", created, found);
    }
    
    /**
     * GlassFish 4.1 and WildFly 8.2.0 remove nickname orphans from the
     * collection table.
     */
    @Test
    @InSequence(2)
    public void test_orphanRemoval() {
        Person person = persons.findById(Person.class, id);
        assertNotNull(person);
        persons.remove(person);
        persons.clearCaches();
        
        List<String> nicks = persons.apply(em ->
                em.createNativeQuery(
                        "SELECT NICKNAMES FROM JPA_MAPPING_ELEMENTCOLLECTION.PERSON_NICKNAMES")
                        .getResultList());
        
        assertTrue(nicks.isEmpty());
    }
}