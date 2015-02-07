package com.martinandersson.javaee.utils;

import com.martinandersson.javaee.resources.CDIInspector;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.container.LibraryContainer;
import org.jboss.shrinkwrap.api.container.ManifestContainer;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * Utility API for deployments.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public final class Deployments
{
    private static final Logger LOGGER = Logger.getLogger(Deployments.class.getName());
    
    private Deployments() {
        // Empty
    }
    
    
    
    /**
     * Will install {@linkplain CDIInspector} as a CDI extension in the provided
     * archive.<p>
     * 
     * Note that this will not screw up the bean archive's type definition
     * (otherwise, archives with a CDI extension is a not a bean archive). The
     * extension is installed as its own library.<p>
     * 
     * Note also that it is indeterministic what happens if an extension has
     * already been installed in the provided archive. Maybe Armageddon.<p>
     * 
     * Also note that the CDI inspector only work on WildFly. GlassFish doesn't
     * pick up the extension at all. Haven't looked into that yet.
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
            LOGGER.warning(() ->
                    "No \"beans.xml\" file in provided archive. Installing the CDI inspector might cause GlassFish 4.1 to crash.");
        }
        
        final Path temp;
        
        try {
             temp = Files.createTempFile(null, null);
             
             /*
              * Arquillian read the file contents lazily and NIO.2's
              * StandardOpenOption.DELETE_ON_CLOSE remove the temp file
              * immediately on stream close. Runtime hooks isn't that pleasant
              * but a necessary evil:
              */
             temp.toFile().deleteOnExit();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        
        try (
            // Files.newBufferedReader() wouldn't hurt but is still unnecessary for one line of content
            OutputStream raw = Files.newOutputStream(temp);
            
            // Java ServiceLoader require UTF-8
            OutputStreamWriter chars = new OutputStreamWriter(raw, StandardCharsets.UTF_8);) {
            
            // All the glorious content
            chars.write(CDIInspector.class.getName());
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        
        // Backup what has already been printed to log
        final Set<String> before = LOGGER.isLoggable(Level.INFO) ?
                toSet(archive) : null;
        
        // Put config file in provided archive's META-INF/services folder
        archive.addAsManifestResource(temp.toFile(),
                "services/" + javax.enterprise.inject.spi.Extension.class.getName());
        
        // Build CDIExtension library and put in provided archive
        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, CDIInspector.class.getSimpleName() + ".jar")
                .addClass(CDIInspector.class);
        
        archive.addAsLibrary(lib);
        
        // Fulfill JavaDoc log contract of Deployments
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
//    private static WebArchive createWAR(Class<?> testClass, Class<?>... include) {
//        return ShrinkWrap.create(WebArchive.class, testClass.getSimpleName() + ".war")
//                .addClasses(include);
//    }
    
    /**
     * Will log the contents of the provided archive using {@code Level.INFO}.
     * 
     * @param <T> type of archive
     * @param archive the archive to log
     * 
     * @return argument for chaining
     */
//    private static <T extends Archive> T log(T archive) {
//        LOGGER.info(() -> toString(archive));
//        return archive;
//    }
    
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
}