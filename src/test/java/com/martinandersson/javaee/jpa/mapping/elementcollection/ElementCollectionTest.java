package com.martinandersson.javaee.jpa.mapping.elementcollection;

import com.martinandersson.javaee.utils.Deployments;
import javax.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * JPA 2.1, section "2.6 Collections of Embeddable Classes and Basic Types":
 * <pre>{@code
 * 
 *     A persistent field or property of an entity or embeddable class may
 *     correspond to a collection of a basic type or embeddable class (“element
 *     collection”). Such a collection, when specified as such by the
 *     ElementCollection annotation, is mapped by means of a collection table,
 *     as defined in Section 11.1.8. If the ElementCollection annotation (or XML
 *     equivalent) is not specified for the collection-valued field or property,
 *     the rules of Section 2.8 apply.
 * 
 * }</pre>
 * 
 * 
 * Section 2.8 doesn't list the field type {@code Set}. Section "11.1.14
 * ElementCollection Annotation" says:
 * <pre>{@code
 * 
 *     The ElementCollection annotation (or equivalent XML element) must be
 *     specified if the collection is to be mapped by means of a collection
 *     table.
 * 
 * }</pre>
 * 
 * My understanding of section 2.6 and 11.1.14 is that {@code @ElementCollection}
 * is optional and may be left out. Only if added shall the collection be mapped
 * to his own table. GlassFish and EclipseLink work in this way, WildFly and
 * Hibernate crash upon deployment.<p>
 * 
 * In order to make this test work for WildFly, you must not deploy {@code
 * Person1.class} and therefore you cannot execute test
 * {@code elementCollectionOptional()}.
 * 
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
public class ElementCollectionTest
{
    @Deployment
    public static WebArchive buildArchive() {
        return Deployments.buildPersistenceArchive(
                ElementCollectionTest.class,
                Person1.class,
                Person2.class,
                Repository.class);
    }
    
    @Inject
    Repository persons;
    
    @Test
    public void elementCollectionOptional() {
        persons.persist(new Person1("nick1", "nick2"));
    }
    
    @Test
    public void elementCollectionInSeparateTable() {
        persons.persist(new Person2("nick1", "nick2"));
    }
}