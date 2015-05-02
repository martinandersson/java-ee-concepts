package com.martinandersson.javaee.utils;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.enterprise.inject.spi.Extension;
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
     * Install the specified extension as a CDI extension in the specified
     * archive.
     * 
     * @param <T> archive type
     * @param archive the target archive
     * @param extension what to install
     * 
     * @return same archive as specified
     */
    public static
        <T extends Archive<T> & ManifestContainer<T> & LibraryContainer<T>>
            T installCDIExtension(T archive, Class<? extends Extension> extension)
    {
        // Backup what has already been printed to log
        final Set<String> before = LOGGER.isLoggable(Level.INFO) ?
                toSet(archive) : null;
        
        archive.addAsServiceProvider(javax.enterprise.inject.spi.Extension.class, extension);
        
        // Build CDIExtension library and put in provided archive
        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, extension.getSimpleName() + ".jar")
                .addClass(extension);
        
        archive.addAsLibrary(lib);
        
        LOGGER.info(() -> {
            Set<String> after = toSet(archive);
            after.removeAll(before);
            
            return "Installed CDI extension in " + archive.getName() + ":\n" +
                    after.stream().sorted().collect(
                            Collectors.joining(System.lineSeparator()));
        });
        
        return archive;
    }
    
    
    
    /*
     *  --------------
     * | INTERNAL API |
     *  --------------
     */
    
    /**
     * Converts the contents of the provided archive to a Set of strings, paths
     * separated with newlines.
     * 
     * @param <T> type of archive
     * @param archive the archive to supposedly log
     * 
     * @return "setified" archive
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