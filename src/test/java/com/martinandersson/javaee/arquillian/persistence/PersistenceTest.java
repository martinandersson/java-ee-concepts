package com.martinandersson.javaee.arquillian.persistence;

import com.martinandersson.javaee.resources.SchemaGenerationStrategy;
import com.martinandersson.javaee.utils.Deployments;
import java.util.List;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This test case use a real database. Every time the test is executed, person
 * "Donald Duck" will be created if he does not already exist, and he will be
 * removed if he do exist. Therefore, you can see the actual result of these
 * operations after each run directly in your NetBeans IDE.<p>
 * 
 * 
 * 
 * <h2>The database we use</h2>
 * 
 * This project use Java DB (also known by its legacy name "Derby") which is
 * included with the JDK. You'll probably find the database here (on Windows):
 * <pre>{@code
 * 
 *     "C:\Program Files\Java\jdk1.8.0\db"
 * 
 * }</pre>
 * 
 * NetBeans already know about this database and you may manage the database
 * using the "Services" tab.<p>
 * 
 * Note that GlassFish is bundled with his own Java DB. When you start GlassFish
 * using the console ("asadmin start-domain"), GlassFish won't automatically
 * start a database. You may force him to ("asadmin start-database") but that
 * will make GlassFish fire up a database instance of the bundled one, not the
 * JDK provided database that is integrated with NetBeans. The database
 * connections we use in our tests are using the "localhost" address, so it
 * doesn't matter which Java DB you use. But it is important to know which
 * database you're actually using when you want to go look for the data.<p>
 * 
 * Also note that when you start GlassFish using NetBeans, then NetBeans will
 * automatically start the JDK bundled Java DB too. When using NetBeans, try to
 * prefer using the IDE provided tooling for management of databases and servers
 * (for Eclipse, it is the other way around).<p>
 * 
 * 
 * 
 * <h2>How to execute</h2>
 * 
 * Begin by having the GlassFish or WildFly server running (if you haven't
 * already, read {@code HelloWorldTest.java} in the "helloworld" package).<p>
 * 
 * Make sure your Java database is running. Then execute this test.<p>
 * 
 * The rest applies to NetBeans only.<p>
 * 
 * After you have executed the test for the first time and it successfully
 * completed, go to the "Services" tab, expand "Databases" -> "Java DB". If
 * you're asked for user credentials, type in username "app" and password "app".
 * If that don't work, try "APP"/"APP".<p>
 * 
 * You should see that a new database has appeared: "arquillian-test-db". If not,
 * try to refresh the screen by restarting your entire IDE (and submit a feature
 * request to the plugin developers that they add a menu refresh item). Once you
 * see the database, right-click on it and select "connect". A new connection
 * leaf should pop-up a bit further down on the screen. Something like:
 * <pre>{@code
 * 
 *     "jdbc:derby://localhost:1527/arquillian-test-db [ on Default schema]"
 * 
 * }</pre>
 * 
 * Expand the connection leaf and continue expanding "Other schemas" ->
 * "ARQUILLIAN_PERSISTENCE" -> "Tables". Right-click table "PERSON" and select
 * "View Data...". You should see Donald somewhere in that table.<p>
 * 
 * The schema "ARQUILLIAN_PERSISTENCE" has been specified using the
 * {@code @Table} annotation on the JPA entity class {@linkplain Person}.<p>
 * 
 * 
 * 
 * <h2>How it works</h2>
 * 
 * The following is a short and compact rundown of how persistence configuration
 * is done in Java EE. Some details are commented all over the files used in
 * this package, but in no way is this test supposed to be a complete guide to
 * entity configuration and management.<p>
 * 
 * 
 * 
 * <h3>Persistence Unit</h3>
 * 
 * Using a database require that we configure a persistence unit, which is
 * provided in the file "persistence.xml". The persistence unit is a unit of
 * entity configuration that says how entities are persisted. This file can
 * contain multiple such configurations, we [and most applications] settle with
 * just one. Two important questions answered by our configuration unit is:
 * 1) Which classes are JPA entities? These classes must be scanned and
 * processed by the persistence provider; the implementation of the Java
 * Persistence API. 2) Which data source will the provider use?<p>
 * 
 * 
 * 
 * <h3>Data Source and JTA</h3>
 * 
 * According to the JavaDoc of {@code javax.sql.DataSource}, a data source
 * is<sup>1</sup>:
 * 
 * <pre>{@code
 * 
 *     A factory for connections to the physical data source that this
 *     DataSource object represents. [A] DataSource object is the preferred
 *     means of getting a connection. [..] The DataSource interface is
 *     implemented by a driver vendor.
 * 
 * }</pre>
 * 
 * The persistence unit configuration must point to a named data source that the
 * persistence provider is supposed to use.<p>
 * 
 * The data source configuration in turn must point to a JDBC compliant driver
 * which is the actual component/class used by the persistence provider for his
 * database access. The data source configuration must also provide things such
 * as a username and a password. It is not hard to understand that when the
 * application is executed, the driver must be present on the server's
 * classpath.<p>
 * 
 * JDBC drivers are said to come in two different flavors: "XA" drivers and
 * non-XA drivers (in the real world though, there will be <i>one</i> driver).
 * The only difference is whether the driver support distributed transactions or
 * not. XA is a popular protocol for distributed transaction processing (DTP)
 * and does not originate from the Java world. XA is the protocol used for
 * distributed transactions in Java EE. The Java Transaction API (JTA) is
 * basically a collection of interfaces that all Java-side parties use to make
 * DTP/XA come true for Java EE applications. You'll hear "JTA" or see it
 * written everywhere so let me clear one minor confusion that inevitably will
 * arise if it hasn't already:<p>
 * 
 * Distributed computing in a cluster or data grid (difference being network
 * locality; cluster = LAN, grid = WAN) is currently not part of Java EE and is
 * vendor-specific. It might become standardized in Java EE 8 (JSR-107 and
 * JSR-347). Yet we keep hearing about "distributed transactions" and probably
 * every Java EE application you've seen, including this test, use this thing
 * called "JTA".<p>
 * 
 * JTA is a specification for "distributed transactions" and the specification
 * itself often refer to a "global transaction". But the transaction doesn't
 * necessarily have to involve two or more machines. Indeed for the vast
 * majority of homebuilt Java EE applications, the active JTA transaction is
 * never "distributed" across the network. The transaction is "distributed" in
 * the sense that it should involve more than just one resource. In our case, we
 * enlist only one transaction-aware resource; the database. So we could manage
 * without JTA. Had we used more resources than so, say a JMS queue or a second
 * database, then we must use a JTA transaction. However, since we want the
 * application server to manage transaction boundaries for us, then we are
 * required to use a JTA transaction anyways<sup>2</sup>. So JTA transactions is
 * the normal case even though most applications technically speaking don't need
 * one.<p>
 * 
 * 
 * 
 * <h3>persistence.xml</h3>
 * 
 * If you read the "persistence.xml" file, you'll find a comment that says Java
 * EE 7 compliant servers provide a "default data source" so that we don't have
 * to define one ourselves. Using a default data source, which is implicitly
 * inferred if the data source reference is left out, make it possible for a
 * Java EE developer to shrink the size of the "persistence.xml" file down to a
 * minimum. But there is no "default persistence unit". So if our application
 * use persistence, then we must bundle a "persistence.xml" file with our
 * deployed archive and in it we must define at least one persistence unit.<p>
 * 
 * 
 * 
 * <h3>Data source definition</h3>
 * 
 * We know how to define our persistence unit in the "persistence.xml" file and
 * we know what goes into the configuration of a data source. But one minor
 * detail is missing: Where do we define the data source, how does it come to
 * life?<p>
 * 
 * A Java SE application that use JPA can put data source configuration such
 * as a username and a password as properties in the "persistence.xml" file.
 * However, it isn't as easy for Java EE applications. The data source
 * reference, if provided, must be put in the "persistence.xml" file. But unlike
 * Java SE applications, we must configure the data source elsewhere.<p>
 * 
 * For Java EE applications, a data source has historically been viewed as a
 * server side resource. Before Java EE 6, defining a data source was a real
 * hassle. One was limited to use application server GUI:s- and consoles, or use
 * vendor-specific XML files such as "glassfish-resources.xml" for GlassFish, or
 * "*-ds.xml" for JBoss. In reality, the developer spent an enourmous amount of
 * time getting things to work. And when the time came for the developer to
 * switch to another application server, he'll find himself right back where he
 * started. The only thing that was portable was "configuration madness".
 * Furthermore, test code that needed to temporarily define a data source in a
 * flexible way had no ways of doing so.<p>
 * 
 * Java EE 6 (JSR-250 version 1.1, section 2.13 [2.14 in version 1.2])
 * introduced the {@code @DataSourceDefinition} annotation to alleviate these
 * problems. Moreover, the umbrella specification (JSR-316 version 6.0, section
 * EE.5.17 [EE.5.18.3 in version 7.0]) said that a data source configuration may
 * also be put in the archive deployment descriptors ("ejb-jar.xml" and
 * "web.xml", provided for your convenience and reference).<p>
 * 
 * Note that how all this relates to the JPA bootstrap process has been left out
 * from the JPA specification. And go figure, all the popular application
 * servers didn't bother to look for a data source definition before actually
 * parsing the "persistence.xml" file. Instead they crashed big-time,
 * complaining about missing resources. Therefore the cool and new features of
 * deployable data source definitions had no effect and was totally useless.
 * Only the latest releases of GlassFish and WildFly has finally made the age
 * old {@code @DataSourceDefinition} actually work as intended (well.. almost,
 * see "ArquillianDS.java"). It is important to know that before you waste time
 * trying to get things to work in older environments.<p>
 * 
 * 
 * 
 * <h3>Use Arquillian and ShrinkWrap to explore!</h3>
 * 
 * If it isn't obvious by now; configuration of a Java EE application server and
 * the packaging of deployment archives is perhaps harder than figuring out how
 * to design and write application code. Use Arquillian and ShrinkWrap to
 * explore this wasteland. Comment statements out, change them, add annotations,
 * do whatever you want to and then run the test again. Simple as that. Make use
 * of this possibility.<p>
 * 
 * 
 * 
 * <h3>Note 1</h3>
 * 
 * For your future reference, the JDBC-, JTA- and JTS specifications all refer
 * to the driver as a "resource adapter" for the "resource manager" which is the
 * actual RDBMS server.
 * 
 * 
 * <h3>Note 2</h3>
 * If you want to know more, google "container-managed transactions" or "CMT"
 * for short which is what the {@code PersonRepository} bean use. Also note that
 * JTA is required for the {@code UserTransaction} interface as well (or BMT;
 * bean-managed transactions). Only the {@code EntityTransaction} interface and
 * resource-local entity managers may skip JTA completely.<p>
 * 
 * One might not feel so great about the fact that we are forced to use
 * expensive DTP/XA given that we use only one database resource. But in such
 * case, modern optimizations employed by application servers can vastly reduce
 * the cost (google "Logging Last Resource" (LLR) or "Last Agent Optimization"
 * (LAO)).
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
public class PersistenceTest
{
    private static final Logger LOGGER = Logger.getLogger(PersistenceTest.class.getName());
    
    @Deployment
    private static WebArchive buildDeployment() {
        return Deployments.buildPersistenceArchive(
                SchemaGenerationStrategy.UPDATE,
                PersistenceTest.class,
                PersonRepository.class,
                Person.class,
                Address.class);
    }
    
    @Inject
    PersonRepository persons;
    
    /**
     * If no Donald Duck was found, one will be created. Otherwise, first Donald
     * Duck returned from database will be massacred.
     */
    @Test
    public void createOrDeleteDonaldDuck() {
        final String donaldDuck = "Donald Duck";
        
        List<Person> donalds = persons.findByName(donaldDuck);
        
        if (donalds.isEmpty()) {
            // Create Donald Duck
            
            LOGGER.info(() -> "Didn't find " + donaldDuck +  ". Will create him.");
            
            Person donald = new Person(donaldDuck);
            
            Address duckburg = new Address()
                    .setStreet("Homeless")
                    .setCity("Duckburg");
            
            donald.setAddress(duckburg);
            
            persons.persist(donald);
            
            LOGGER.info(() -> donaldDuck +  " after having been persisted: " + donald);
            
            assertEquals(true, donald.getId() > 0L); // <-- if you want a readable output when things go wrong, prefer assertEquals() over assertTrue()
            assertEquals(true, persons.exists(donaldDuck));
            assertEquals(1, persons.findByName(donaldDuck).size());
            assertEquals(1, persons.findByAddress(duckburg).size());
        }
        
        else {
            // Delete Donald Duck
            
            LOGGER.info(() -> "Found Donald Ducks (" + donalds.size() + "): " + donalds);
            
            Person donald = donalds.get(0);
            
            persons.delete(donald);
            assertNull(persons.findById(donald.getId()));
        }
    }
}