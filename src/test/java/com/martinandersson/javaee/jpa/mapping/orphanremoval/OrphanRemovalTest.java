package com.martinandersson.javaee.jpa.mapping.orphanremoval;

import com.martinandersson.javaee.resources.SchemaGenerationStrategy;
import com.martinandersson.javaee.utils.Deployments;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

/**
 * See:
 * <pre>{@code
 * 
 *     http://docs.oracle.com/javaee/7/tutorial/persistence-intro001.htm#JEETT00677
 * 
 * }</pre>
 * 
 * Currently, the example provided by the Java EE 7 tutorial and tested in
 * {@linkplain #removeFromRelationshipWithOrphanRemoval_entityAlsoRemoved() removeFromRelationshipWithOrphanRemoval_entityAlsoRemoved()}
 * does not work on WildFly/Hibernate.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
public class OrphanRemovalTest
{
    @Deployment
    private static Archive<?> buildDeployment() {
        return Deployments.buildPersistenceArchive(SchemaGenerationStrategy.UPDATE,
                OrphanRemovalTest.class,
                Repository.class,
                Owner.class,
                AbstractId.class,
                CascadeNone.class,
                CascadeRemove.class,
                OrphanRemoval.class);
    }
    
    
    
    static final String SCHEMA = "JPA_MAPPING_ORPHANREMOVAL";
    
    private static final Logger LOGGER = Logger.getLogger(OrphanRemovalTest.class.getName());
    
    private static long ownerId, noneId, removeId, orphanId;
    
    
    
    @EJB
    Repository repo;
    
    @Rule
    public TestName name = new TestName();
    
    
    
    /*
     *  ------------
     * | LIFE CYCLE |
     *  ------------
     */
    
    
    @Before
    public void persistTestees() {
        LOGGER.info(() -> "RUNNING: " + name.getMethodName());
        persistNewTestees();
    }
    
    private void persistNewTestees() {
        repo.applyWithEM(em -> {
            Owner owner = new Owner();
            em.persist(owner);
            
            CascadeNone none = new CascadeNone();
            em.persist(none);
            owner.addCascadeNone(none);
            
            CascadeRemove remove = new CascadeRemove();
            em.persist(remove);
            owner.addCascadeRemove(remove);
            
            OrphanRemoval orphan = new OrphanRemoval();
            em.persist(orphan);
            owner.addOrphanRemoval(orphan);
            
            ownerId = owner.getId();
            noneId = none.getId();
            removeId = remove.getId();
            orphanId = orphan.getId();
            
            LOGGER.info(() ->
                    "Persisted owner #" + ownerId  +
                    ", none #"          + noneId   +
                    ", remove #"        + removeId + 
                    " and orphan #"      + orphanId + ".");
        });
        
        assertNotNull(repo.find(Owner.class, ownerId));
        assertNotNull(repo.find(CascadeNone.class, noneId));
        assertNotNull(repo.find(CascadeRemove.class, removeId));
        assertNotNull(repo.find(OrphanRemoval.class, orphanId));
    }
    
    
    
    /*
     *  -------
     * | TESTS |
     *  -------
     */
    
    /**
     * Removing the owner remove the non-owning {@code CascadeRemove} entity
     * <strong>and</strong> the non-owning {@code OrphanRemoved} entity.<p>
     * 
     * JavaDoc of {@code @OneToMany} says that {@code orphanRemoval = true} also
     * "cascade the remove operation to those entities". Hence it is not
     * necessary to add {@code cascade = CascadeType.REMOVE} on a relationship
     * that says {@code orphanRemoval = true}.
     */
    @Test
    public void removingOwnerRemoveCascadeAndOrphanEntities() {
        repo.remove(Owner.class, ownerId);
        
        // Left behind:
        assertNotNull(repo.find(CascadeNone.class, noneId));
        
        // Removed:
        assertNull(repo.find(CascadeRemove.class, removeId));
        assertNull(repo.find(OrphanRemoval.class, orphanId));
    }
    
    /**
     * Can not remove a non-owning entity as long as he is the target of a
     * relationship.<p>
     * 
     * Somewhere in the stack, we expect to find a
     * {@code java.sql.SQLIntegrityConstraintViolationException}.
     */
    @Test
    public void cannotRemoveNonOwningEntityInUse_CascadeNone() {
        try {
            repo.remove(CascadeNone.class, noneId);
            fail("Must not be able to remove a non-owning entity in use!");
        }
        catch (EJBException e) {
            /*
            
            GF:
            
            javax.ejb.EJBException: Transaction aborted
              Caused by: javax.transaction.RollbackException: Transaction marked for rollback.
              Caused by: javax.persistence.PersistenceException: Exception [EclipseLink-4002] (Eclipse Persistence Services - 2.5.2.v20140319-9ad6abd): org.eclipse.persistence.exceptions.DatabaseException
              Caused by: Exception [EclipseLink-4002] (Eclipse Persistence Services - 2.5.2.v20140319-9ad6abd): org.eclipse.persistence.exceptions.DatabaseException
              Caused by: java.sql.SQLIntegrityConstraintViolationException: DELETE on table 'CASCADE_NONE' caused a violation of foreign key constraint 'FK_TFEMGL0JYRM7AJ2F5SO9X8L9W' for key (102).  The statement has been rolled back.
              Caused by: org.apache.derby.client.am.SqlException: DELETE on table 'CASCADE_NONE' caused a violation of foreign key constraint 'FK_TFEMGL0JYRM7AJ2F5SO9X8L9W' for key (102).  The statement has been rolled back.
            
            WF:
            
            Note that WildFly throw a specialized
            EJBTransactionRolledbackException. See:
            com.martinandersson.javaee.ejb.exceptions.EJBTransactionRolledbackExceptionTest
            
            javax.ejb.EJBTransactionRolledbackException: Transaction rolled back
              Caused by: javax.transaction.RollbackException: ARJUNA016053: Could not commit transaction.
              Caused by: javax.persistence.PersistenceException: org.hibernate.exception.ConstraintViolationException: could not execute statement
              Caused by: org.hibernate.exception.ConstraintViolationException: could not execute statement
              Caused by: java.sql.SQLIntegrityConstraintViolationException: DELETE on table 'CASCADE_NONE' caused a violation of foreign key constraint 'FK_TFEMGL0JYRM7AJ2F5SO9X8L9W' for key (76).  The statement has been rolled back.
              Caused by: org.apache.derby.client.am.SqlException: DELETE on table 'CASCADE_NONE' caused a violation of foreign key constraint 'FK_TFEMGL0JYRM7AJ2F5SO9X8L9W' for key (76).  The statement has been rolled back.
            
            */
            
            assertTrue(causedBy(e, SQLIntegrityConstraintViolationException.class));
        }
    }
    
    /**
     * Can not remove a non-owning entity as long as he is the target of a
     * relationship.<p>
     * 
     * Somewhere in the stack, we expect to find a
     * {@code java.sql.SQLIntegrityConstraintViolationException}.
     */
    @Test
    public void cannotRemoveNonOwningEntityInUse_CascadeRemove() {
        try {
            repo.remove(CascadeRemove.class, removeId);
            fail("Must not be able to remove a non-owning entity in use!");
        }
        catch (EJBException e) {
            assertTrue(causedBy(e, SQLIntegrityConstraintViolationException.class));
        }
    }
    
    /**
     * Can not remove a non-owning entity as long as he is the target of a
     * relationship.<p>
     * 
     * Somewhere in the stack, we expect to find a
     * {@code java.sql.SQLIntegrityConstraintViolationException}.
     */
    @Test
    public void cannotRemoveNonOwningEntityInUse_OrphanRemove() {
        try {
            repo.remove(OrphanRemoval.class, orphanId);
            fail("Must not be able to remove a non-owning entity in use!");
        }
        catch (EJBException e) {
            assertTrue(causedBy(e, SQLIntegrityConstraintViolationException.class));
        }
    }
    
    /**
     * Removing a non-owning entity from the relationship of his owner will
     * leave the entity untouched in database. Only {@code orphanRemoval = true}
     * will actually remove the entity too.
     */
    @Test
    public void removeFromNoCascadeRelationship_entityLeftBehind() {
        repo.applyWithEM(em -> {
            Owner owner = em.find(Owner.class, ownerId);
            CascadeNone none = em.find(CascadeNone.class, noneId);
            owner.removeCascadeNone(none);
        });
        
        // Non-owning entity left behind:
        assertNotNull(repo.find(CascadeNone.class, noneId));
        
        // But entity is not present in the relationship anymore:
        Owner owner = repo.find(Owner.class, ownerId);
        assertTrue(owner.getCascadeNones().isEmpty());
        
        // Everyone else is untouched:
        assertCascadeRemoveIsNotFound();
        assertOrphanRemovalIsNotFound();
    }
    
    /**
     * Removing a non-owning entity from the relationship of his owner will
     * leave the entity untouched in database. Only {@code orphanRemoval = true}
     * will actually remove the entity too.
     */
    @Test
    public void removeFromCascadeRelationship_entityLeftBehind() {
        repo.applyWithEM(em -> {
            Owner owner = em.find(Owner.class, ownerId);
            CascadeRemove remove = em.find(CascadeRemove.class, removeId);
            owner.removeCascadeRemove(remove);
        });
        
        // "CascadeType.REMOVE" does not remove the non-owning entity from db:
        assertNotNull(repo.find(CascadeRemove.class, removeId));
        
        // But entity is removed from relationship:
        Owner owner = repo.find(Owner.class, ownerId);
        assertTrue(owner.getCascadeRemoves().isEmpty());
        
        // Everyone else is untouched:
        assertCascadeNoneIsNotFound();
        assertOrphanRemovalIsNotFound();
    }
    
    /**
     * Removing a non-owning entity from a relationship that is marked
     * {@code orphanRemoval = true} will remove the entity from database too.<p>
     * 
     * GlassFish pass this test. WildFly/Hibernate don't. WildFly pass
     * <strong>only if</strong> the relationship of {@code Owner#orphans} is
     * also marked {@code CascadeType.ALL} or {@code CascadeType.PERSIST} (!).
     * Setting cascade to {@code CascadeType.REMOVE} does not help.<p>
     * 
     * Related Hibernate bugs:
     * <pre>{@code
     * 
     *     https://hibernate.atlassian.net/browse/HHH-6709
     *     https://hibernate.atlassian.net/browse/HHH-6037
     * }</pre>
     * 
     * Also see:
     * <pre>{@code
     * 
     *     http://stackoverflow.com/q/24579374/1268003
     * }</pre>
     */
    @Test
    public void removeFromRelationshipWithOrphanRemoval_entityAlsoRemoved() {
        repo.applyWithEM(em -> {
            Owner owner = em.find(Owner.class, ownerId);
            OrphanRemoval orphan = em.find(OrphanRemoval.class, orphanId);
            owner.removeOrphanRemoval(orphan);
        });
        
        // Orphan completely evaporated (for GlassFish at least, WildFly crash):
        assertNull(repo.find(OrphanRemoval.class, orphanId));
        
        Owner owner = repo.find(Owner.class, ownerId);
        assertTrue(owner.getOrphanRemovals().isEmpty());
        
        // Everyone else is untouched:
        assertCascadeNoneIsNotFound();
        assertCascadeRemoveIsNotFound();
    }
    
    
    
    /*
     *  --------------
     * | INTERNAL API |
     *  --------------
     */
    
    private boolean causedBy(Throwable searchIn, Class<? extends Throwable> lookingFor) {
        final Throwable cause = searchIn.getCause();
        
        return cause == null ? false :
                cause.getClass() == lookingFor || causedBy(cause, lookingFor);
    }
    
    private void assertCascadeNoneIsNotFound() {
        Owner owner = repo.find(Owner.class, ownerId);
        assertTrue(owner.getCascadeNones().size() == 1);
        assertTrue(owner.getCascadeNones().contains(repo.find(CascadeNone.class, noneId)));
    }
    
    private void assertCascadeRemoveIsNotFound() {
        Owner owner = repo.find(Owner.class, ownerId);
        assertTrue(owner.getCascadeRemoves().size() == 1);
        assertTrue(owner.getCascadeRemoves().contains(repo.find(CascadeRemove.class, removeId)));
    }
    
    private void assertOrphanRemovalIsNotFound() {
        Owner owner = repo.find(Owner.class, ownerId);
        assertTrue(owner.getOrphanRemovals().size() == 1);
        assertTrue(owner.getOrphanRemovals().contains(repo.find(OrphanRemoval.class, orphanId)));
    }
}