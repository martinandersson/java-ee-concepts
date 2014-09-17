package com.martinandersson.javaee.utils;

import com.martinandersson.javaee.resources.ArquillianDS;
import com.martinandersson.javaee.resources.CDIInspector;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.container.LibraryContainer;
import org.jboss.shrinkwrap.api.container.ManifestContainer;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

/**
 * API for building and manipulating deployments.<p>
 * 
 * All factories in this class <strong>log the archive</strong> contents using
 * {@code Level.INFO}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public final class Deployments
{
    private static final Logger LOGGER = Logger.getLogger(Deployments.class.getName());
    
    
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
    
    
    
    private Deployments() {
        // Empty
    }
    
    
    
    /*
     *  -----------
     * | FACTORIES |
     *  -----------
     */
    
    /**
     * Will build an "explicit bean archive" that contains a {@code beans.xml}
     * file, albeit empty.
     * 
     * @param testClass calling test class
     * @param include which classes to include, may be none
     * 
     * @return the bean archive
     * 
     * @see com.martinandersson.javaee.cdi.packaging
     */
    public static WebArchive buildCDIBeanArchive(Class<?> testClass, Class<?>... include) {
        WebArchive war = createWAR(testClass, include)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        
        return log(war);
    }
    
    /**
     * Will build an archive that in addition to the provided classes, also
     * include {@code ArquillianDS.class} (data source definition), the {@code
     * persistence.xml} file (persistence unit configuration) and a Java DB
     * client diver which WildFly doesn't have.
     * 
     * @param testClass calling test class
     * @param include which classes to include, may be none
     * 
     * @return the archive
     */
    public static WebArchive buildPersistenceArchive(Class<?> testClass, Class<?>... include) {
        // Append datasource definition to the include list:
        include = Arrays.copyOf(include, include.length + 1);
        include[include.length - 1] = ArquillianDS.class;
        
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
        
        WebArchive war = createWAR(testClass, include)
                .addAsLibrary(DERBY_DRIVER)
                .addAsResource("persistence.xml", "META-INF/persistence.xml");
        
        return log(war);
    }
    
    /**
     * WIll build a "POJO" WAR file that optionally include the provided classes.
     * 
     * @param testClass calling test class
     * @param include which classes to include, may be none
     * 
     * @return the WAR archive
     */
    public static WebArchive buildWAR(Class<?> testClass, Class<?>... include) {
        return log(createWAR(testClass, include));
    }
    
    
    
    /*
     *  ------------
     * | EXTENSIONS |
     *  ------------
     */
    
    /**
     * Will install {@linkplain CDIInspector} as a CDI extension in the provided
     * archive.<p>
     * 
     * Note that this will not fuck up the bean archive's type definition
     * (otherwise, archives with a CDI extension is a not a bean archive). The
     * extension is installed as its own library.<p>
     * 
     * Note also that it is indeterministic what happens if an extension has
     * already been installed in the provided archive. Maybe Armageddon.<p>
     * 
     * Also note that the CDI inspector only work on WildFly, if "beans.xml" is
     * provided with the archive. GlassFish doesn't pick up the extension at
     * all. I guess that GlassFish want the extension deployed separately.
     * Haven't looked into that yet.
     * 
     * @param <T> type need to be an {@code Archive} that implements {@code
     *        ManifestContainer} and {@code LibraryContainer}
     * @param archive the archive
     * 
     * @throws UncheckedIOException when shit hit the fan
     * 
     * @return the archive for chaining
     */
    public static
        <T extends Archive<T> & ManifestContainer<T> & LibraryContainer<T>>
            T installCDIInspector(T archive)
    {
        if (!archive.contains("WEB-INF/beans.xml")) {
            LOGGER.warning("No \"beans.xml\" file in provided archive. The CDI inspector might not be picked up by WildFly.");
        }
        
        // 1. Make provider configuration file
        
        final File providerConfig;
        
        try {
             providerConfig = Files.createTempFile(null, null).toFile();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        
        // Not using FileWriter since Java ServiceLoader require UTF-8, so to be sure:
        try (
            FileOutputStream raw = new FileOutputStream(providerConfig);
            OutputStreamWriter chars = new OutputStreamWriter(raw, StandardCharsets.UTF_8);) {
            chars.write(CDIInspector.class.getName());
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        
        // 2. Backup what has already been printed to log
        
        final Set<String> before = LOGGER.isLoggable(Level.INFO) ?
                toSet(archive) : null;
        
        // 3. Put config file in provided archive's META-INF/services folder
        
        archive.addAsManifestResource(providerConfig,
                "services/" + javax.enterprise.inject.spi.Extension.class.getSimpleName());
        
        // 4. Make CDIExtension library and put in provided archive
        
        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, CDIInspector.class.getSimpleName() + ".jar")
                .addClass(CDIInspector.class);
        
        archive.addAsLibrary(lib);
        
        // 5. Log new contents in accordance with JavaDoc of Deployments
        
        LOGGER.info(() -> {
            Set<String> after = toSet(archive);
            after.removeAll(before);
            return "Added to " + archive.getName() + ":\n" + after.stream().sorted().collect(Collectors.joining("\n"));
        });
        
        return archive;
    }
    
    
    
    /*
     *  --------------
     * | INTERNAL API |
     *  --------------
     */
    
    /**
     * Will build a "POJO" WAR file that optionally include the provided
     * classes.<p>
     * 
     * This method do no logging. That is the responsibility of the real builder.
     * 
     * @param testClass calling test class
     * @param include which classes to include, may be none
     * 
     * @return the WAR archive
     */
    private static WebArchive createWAR(Class<?> testClass, Class<?>... include) {
        return ShrinkWrap.create(WebArchive.class, testClass.getSimpleName() + ".war")
                .addClasses(include);
    }
    
    /**
     * Will log the contents of the provided archive using {@code Level.INFO}.
     * 
     * @param <T> type of archive
     * @param archive the archive to log
     * 
     * @return argument for chaining
     */
    private static <T extends Archive> T log(T archive) {
        LOGGER.info(() -> toString(archive));
        return archive;
    }
    
    /**
     * Converts the contents of the provided archive to a String, paths
     * separated with newlines.
     * 
     * @param <T> type of archive
     * @param archive the archive to supposedly log
     * 
     * @return stringified archive
     */
    private static <T extends Archive<T>> String toString(T archive) {
        return archive.toString(true).replace("\n", "\n\t");
    }
    
    /**
     * Converts the contents of the provided archive to a Set, paths
     * separated with newlines.
     * 
     * @param <T> type of archive
     * @param archive the archive to supposedly log
     * 
     * @return "setified" archive
     * 
     * @see #toString(Archive)
     */
    private static <T extends Archive<T>> Set<String> toSet(T archive) {
        return Stream.of(toString(archive).split("\n")).collect(Collectors.toSet());
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