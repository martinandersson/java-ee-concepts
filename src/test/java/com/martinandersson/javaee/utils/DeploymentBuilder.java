package com.martinandersson.javaee.utils;

import com.martinandersson.javaee.resources.ArquillianDS;
import com.martinandersson.javaee.resources.SchemaGenerationStrategy;
import java.lang.reflect.Array;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

/**
 * Builder of deployable archives.<p>
 * 
 * Currently, the only outcome of this builder is a {@code WebArchive}
 * (.war).<p>
 * 
 * The builder is not reusable. {@linkplain #build()} crash if called twice.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class DeploymentBuilder
{
    private static final Logger LOGGER = Logger.getLogger(DeploymentBuilder.class.getName());
    
    /*
     * Next initialization uses Maven resolver to download the Java DB client.
     * WildFly doesn't have this driver so we need to package the driver
     * together with the deployments to make them work with Java DB.
     * 
     * In the future, you might also want to add more libraries such as
     * "org.mockito:mockito-all:1.9.5" (server-side mocking! (note 1)). That
     * would change the following code snippet to:
     * 
     *     JavaArchive[] libs = Maven.resolver().resolve(
     *             "org.apache.derby:derbyclient:10.10.2.0",
     *             "org.mockito:mockito-all:1.9.5")
     *             .withTransitivity().as(JavaArchive.class);
     * 
     *     war.addAsLibraries(libs);
     * 
     * Read more: https://github.com/shrinkwrap/resolver
     */
    private static final JavaArchive DERBY_DRIVER = Maven.resolver()
            .resolve("org.apache.derby:derbyclient:10.10.2.0")
            .withTransitivity()
            .asSingle(JavaArchive.class);
    
    
    
    private final Instant then;
    
    private final Class<?> test;
    
    private final Set<Class<?>> types;
    
    private final Set<Package> packages;
    
    private boolean beans;
    
    private SchemaGenerationStrategy persistence;
    
    private boolean built;
    
    /**
     * Initializes a new deployment builder.<p>
     * 
     * The archive's name will become the simple name of the specified test
     * class.
     * 
     * @param testClass test class using this builder
     */
    public DeploymentBuilder(Class<?> testClass) {
        then = Instant.now();
        test = Objects.requireNonNull(testClass);
        types = new HashSet<>();
        packages = new HashSet<>();
    }
    
    /**
     * Add the specified types to the archive.
     * 
     * @param first any type
     * @param more more types
     * 
     * @return this builder
     */
    public DeploymentBuilder add(Class<?> first, Class<?>... more) {
        types.add(first);
        Collections.addAll(types, more);
        return this;
    }
    
    /**
     * Add all types discovered in the specified package to the archive.
     * 
     * @param first a Java package
     * @param more some more Java packages
     * 
     * @return this builder
     */
    public DeploymentBuilder add(Package first, Package... more) {
        packages.add(first);
        Collections.addAll(packages, more);
        return this;
    }
    
    /**
     * Add the specified types to the archive.<p>
     * 
     * If {@code withPackageFriends} is {@code true}, then all types discovered
     * to be collocated with the specified types are included as well.
     * 
     * @param withPackageFriends include package friends or not
     * @param first any Java type
     * @param more some more Java types
     * 
     * @return this builder
     */
    public DeploymentBuilder add(boolean withPackageFriends, Class<?> first, Class<?>... more) {
        add(first, more);
        
        if (withPackageFriends) {
            Stream.of(merge(first, more))
                    .map(Class::getPackage)
                    .distinct()
                    .forEach(this::add);
        }
        
        return this;
    }
    
    /**
     * Add all types discovered in the same package as the test class using this
     * builder.
     * 
     * @return this builder
     */
    public DeploymentBuilder addTestPackage() {
        add(test.getPackage());
        return this;
    }
    
    /**
     * Add all types discovered in the same package as the specified type.
     * 
     * @param type any types
     * 
     * @return this builder
     */
    public DeploymentBuilder addPackageOf(Class<?> type) {
        add(type.getPackage());
        return this;
    }
    
    /**
     * Will add an empty {@code beans.xml} to the archive.<p>
     * 
     * The built archive is considered an "explicit bean archive" meaning that
     * all classes will be eligible for lookup.
     * 
     * @return this builder
     * 
     * @see com.martinandersson.javaee.cdi.packaging
     */
    public DeploymentBuilder addEmptyBeansXMLFile() {
        beans = true;
        return this;
    }
    
    /**
     * Will add a {@code persistence.xml} file and a Java DB driver to the
     * archive.<p>
     * 
     * The Java DB driver is only necessary for WildFly, not GlassFish.
     * GlassFish already has a Java DB driver, WildFly don't.
     * 
     * @param strategy which strategy to use
     * 
     * @return this builder
     */
    public DeploymentBuilder addPersistenceXMLFile(SchemaGenerationStrategy strategy) {
        persistence = Objects.requireNonNull(strategy);
        return this;
    }
    
    private <T> T[] merge(T first, T... more) {
        T[] arr = first.getClass() == Object.class ?
                (T[]) new Object[1 + more.length] :
                (T[]) Array.newInstance(first.getClass(), 1 + more.length);
        
        arr[0] = first;
        IntStream.range(1, more.length + 1).forEach(i -> arr[i] = more[i - 1]);
        
        return arr;
    }
    
    /**
     * Build an archive using types and settings as provided earlier to the
     * builder.
     * 
     * @return a web archive
     * 
     * @throws IllegalStateException if deployment has already been built
     */
    public WebArchive build() {
        if (built) {
            throw new IllegalStateException("Deployment already built.");
        }
        
        WebArchive war = ShrinkWrap.create(WebArchive.class, test.getSimpleName() + ".war");
        
        types.forEach(war::addClass);
        packages.forEach(war::addPackage);
        
        if (beans) {
            war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        }
        
        if (persistence != null) {
            
            /*
             * Instead of using ArquillianDS.class as a convenient way to add our
             * data source definition. We could have included a deployment
             * descriptor instead:
             * 
             *     war.addAsWebInfResource("web.xml");
             * 
             * A server specific configuration file can be added like so (note 2):
             * 
             *     war.addAsResource("wildfly-ds.xml", "META-INF/wildfly-ds.xml");
             */
            
            war.addClass(ArquillianDS.class)
               .addAsLibrary(DERBY_DRIVER)
               .addAsResource(persistence.getFilename(), "META-INF/persistence.xml");
        }
        
        LOGGER.info(() -> war.toString(true).replace("\n", "\n\t"));
        LOGGER.info(() -> "Built in (ms): " + Duration.between(then, Instant.now()).toMillis());
        
        types.clear();
        packages.clear();
        built = true;
        
        return war;
    }
}

/*
 * NOTES
 * -----
 * 
 * 1) In the Arquillian "Hello World" test, I argued that mocking should not be
 *    used in Arquillian. But mocking doesn't necessarily require the trade of a
 *    working implementation for a mock. One can also use mocking to try new and
 *    unwritten implementations of system components together with the rest of
 *    the application without having to write them first.
 * 
 * 2) Actually I would like to use the methods "addAsWebResource()" and
 *    "addAsManifestResource()" but they don't work properly, so the example
 *    provided go down a level and explicitly provide the path.
 */