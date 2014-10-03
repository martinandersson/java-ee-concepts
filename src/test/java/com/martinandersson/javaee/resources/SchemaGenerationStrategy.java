package com.martinandersson.javaee.resources;

/**
 * Strategy for database schema generation.<p>
 * 
 * Available strategies:
 * 
 * <ul>
 *   <li>{@linkplain #UPDATE}</li>
 *   <li>{@linkplain #DROP_CREATE}</li>
 * </ul>
 * 
 * @see com.martinandersson.javaee.utils.Deployments#buildPersistenceArchive(SchemaGenerationStrategy, Class, Class...) 
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public enum SchemaGenerationStrategy
{
    /**
     * All tables will be 1) created if not present or 2) used if already
     * present and 3) updated if need be.<p>
     * 
     * This strategy work homogenously on EclipseLink as well as Hibernate.<p>
     * 
     * Using this strategy will make file
     * {@code ./src/test/resources/persistence-update.xml} be deployed in the
     * test archive as {@code META-INF/persistence.xml}.
     */
    UPDATE("persistence-update.xml"),
    
    /**
     * In case of Hibernate, will drop-create-drop: meaning that the tables
     * won't be left behind after the test is run.
     * 
     * All tables will be 1) dropped and then 2) created.<p>
     * 
     * WildFly that uses Hibernate has a third side-effect: Hibernate drop the
     * tables after the test. Meaning that you cannot explore the tables and
     * data in them after the test is done. If that is what you want, then use
     * GlassFish/EclipseLink.<p>
     * 
     * Using this strategy will make file
     * {@code ./src/test/resources/persistence-dropcreate.xml} be deployed in
     * the test archive as {@code META-INF/persistence.xml}.
     */
    DROP_CREATE("persistence-dropcreate.xml");
    
    private final String filename;
    
    SchemaGenerationStrategy(String filename) {
        this.filename = filename;
    }
    
    /**
     * Returns the filename of the persistence unit configuration file.
     * 
     * @return the filename of the persistence unit configuration file
     */
    public String getFilename() {
        return filename;
    }
}