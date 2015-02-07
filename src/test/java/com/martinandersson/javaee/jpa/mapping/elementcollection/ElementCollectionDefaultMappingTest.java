package com.martinandersson.javaee.jpa.mapping.elementcollection;

import com.martinandersson.javaee.jpa.mapping.elementcollection.lib.Repository;
import com.martinandersson.javaee.jpa.mapping.elementcollection.lib.Song;
import com.martinandersson.javaee.resources.SchemaGenerationStrategy;
import com.martinandersson.javaee.utils.DeploymentBuilder;
import javax.ejb.EJB;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Fails on WildFly/Hibernate (8.1.0 and 8.2.0).
 * 
 * @see com.martinandersson.javaee.jpa.mapping.elementcollection
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
public class ElementCollectionDefaultMappingTest
{    
    @Deployment
    private static Archive<?> buildArchive() {
        return new DeploymentBuilder(ElementCollectionDefaultMappingTest.class)
                .addPersistenceXMLFile(SchemaGenerationStrategy.DROP_CREATE)
                .add(Song.class, Repository.class)
                .build();
    }
    
    @EJB
    Repository songs;
    
    @Test
    public void elementCollectionOptional() {
        Song created = new Song("Michael Jackson", "Madonna Louise Ciccone");
        songs.persist(created);
        
        final long id = created.getId();
        assertTrue(id > 0L);
        
        songs.clearCaches();
        
        Song found = songs.findById(Song.class, id);
        
        // This will also do equality check of all the producers of Song:
        assertEquals("Song actually created",
                created, found);
    }
}