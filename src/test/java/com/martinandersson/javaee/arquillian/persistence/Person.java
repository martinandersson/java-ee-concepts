package com.martinandersson.javaee.arquillian.persistence;

import java.util.Objects;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * World's most simplest JPA @Entity class.<p>
 * 
 * Current persistence providers (and the byte code tooling they use) are having
 * severe problems parsing Java 1.8 syntax. This is a problem with EclipseLink
 * 2.5.2 and Hibernate 4.3.5. The JDK 8 library can still be used in JPA
 * entities. Java 1.8 syntax and latest GlassFish/WildFly servers have no
 * problem with Java 1.8 syntax in application components such as EJB:s (GF
 * 4.0.0 break).
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Entity
@Table(schema = "ARQUILLIAN_PERSISTENCE")
public class Person // note 1
{
    @Id
    @GeneratedValue
    private long id;
    
    @Version
    private long modCount;
    
    // Is implicitly: @Basic
    private String name;
    
    @Embedded
    private Address address;
    
    
    
    /**
     * No-arg constructor required by JPA. Application code should have no
     * business using this constructor.
     */
    protected Person() {}
    
    public Person(String name) {
        this.name = Objects.requireNonNull(name, "name is null");
    }
    
    public long getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public Address getAddress() {
        return address;
    }
    
    public Person setAddress(Address address) {
        this.address = address;
        return this; // <-- for chaining calls
    }
    
    
    
    /*
     *  ------------------
     * | OBJECT OVERRIDES |
     *  ------------------
     */
    
    @Override
    public String toString()
    {
        return new StringBuilder(Person.class.getSimpleName())
                .append("[")
                  .append("id=").append(id)
                  .append(", name=").append(name)
                  .append(", address=").append(address)
                .append("]")
                .toString();
    }
}

/*
 * NOTE 1: It is always a good idea to make the entity implement Serializable.
 *         However, it is not required until the day comes when the entity needs
 *         to be serialized. JPA 2.1 specification, section 2.1 ("The Entity
 *         Class"):
 *         
 *             "If an entity instance is to be passed by value as a detached
 *              object (e.g., through a remote interface), the entity class must
 *              implement the Serializable interface."
 */