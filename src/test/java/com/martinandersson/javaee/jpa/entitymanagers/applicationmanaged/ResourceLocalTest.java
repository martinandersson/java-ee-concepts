package com.martinandersson.javaee.jpa.entitymanagers.applicationmanaged;

/**
 * <h3>NOT FINISHED</h3>
 * 
 * All tests today inject a persistence unit or context without specifying the
 * unit's name as defined in the persistence.xml file. That is okay, as long as
 * there is just one unit in there. Adding another, resource-local, persistence
 * unit to the file would require a explicit unit name by all injection points.
 * Wouldn't want that!<p>
 * 
 * So it would be preferable to avoid using real persistence.xml files and
 * instead build them during deployment. User can always open the deployed
 * archive that is left behind in the deployments folder if he want to examine
 * the contents.<p>
 * 
 * Sooo to even begin writing this test class, the API to deploy a persistence
 * archive must be reworked to offer a builder that accept all configuration
 * parameters. One being whether or not the persistence unit should be JTA or
 * resource-local. Leaving that for the future! =)<p>
 * 
 * 
 * <h3>To Test</h3>
 * 
 * JPA 2.1, section "7.5.2 Resource-local EntityManagers":
 * <pre>{@code
 * 
 *     Resource-local entity managers may use server or local resources to
 *     connect to the database and are unaware of the presence of JTA
 *     transactions that may or may not be active.
 * 
 * }</pre>
 * 
 * Okay sure. However, JTA 1.2, section "3.4.7 Local and Global Transactions"
 * say:
 * <pre>{@code
 * 
 *    If a resource adapter does not support mixing local and global
 *    transactions within the same connection, the resource adapter should
 *    throw the resource specific exception. For example, java.sql.SQLException
 *    is thrown to the application if the resource manager for the underlying
 *    RDBMS does not support mixing local and global transactions within the
 *    same JDBC connection.
 * 
 * }</pre>
 * 
 * If I understood the first quote correctly, then a resource-local EM can be
 * used with JTA as well?? Yet JTA says that's a big no no. So examine, who's
 * rite.
 * 
 * 
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
//@RunWith(Arquillian.class)
public class ResourceLocalTest
{
    
}