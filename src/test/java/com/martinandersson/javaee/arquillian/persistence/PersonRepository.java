package com.martinandersson.javaee.arquillian.persistence;

import java.util.List;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 * World's most simplest repository with some Criteria API examples.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Stateless
// Is implicitly: @TransactionAttribute(TransactionAttributeType.REQUIRED)
public class PersonRepository
{
    @PersistenceContext // <-- we only need to set the unitName attribute if we have more than one unit defined in persistence.xml
    EntityManager em;
    
    
    
    @TransactionAttribute(TransactionAttributeType.SUPPORTS) // <-- SELECT doesn't have to be executed in a transaction
    public boolean exists(String name) {
        CriteriaQuery<Long> queryForLong = em.getCriteriaBuilder().createQuery(Long.class);
        Root<Person> personTable = queryForLong.from(Person.class);
        
        Predicate nameEquality = createPredicateForNameEquality(personTable, name);
        queryForLong.where(nameEquality);
        
        queryForLong.select(em.getCriteriaBuilder().count(personTable));
        
        return em.createQuery(queryForLong).getSingleResult() > 0;
    }
    
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Person findById(long id) {
        return em.find(Person.class, id);
    }
    
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Person> findByName(String name) {
        PersonQuery person = createQueryForPersons();
        Predicate nameEquality = createPredicateForNameEquality(person.root, name);
        person.query.where(nameEquality);
        
        return em.createQuery(person.query).getResultList();
    }
    
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Person> findByAddress(Address address) {
        PersonQuery person = createQueryForPersons();
        Predicate addressEquality = createPredicateForAddressEquality(person, address);
        person.query.where(addressEquality);
        
        return em.createQuery(person.query).getResultList();
    }
    
    public void persist(Person person) {
        em.persist(person); // <-- all operations cascade for embeddables (Person#address)
    }
    
    public void merge(Person person) {
        em.merge(person); // <-- merge actually support persist too
    }
    
    public void delete(Person person) {
        Person managedRef = em.contains(person) ? person : em.merge(person); // note 1
        em.remove(managedRef);
    }
    
    
    
    /*
     *  --------------
     * | INTERNAL API |
     *  --------------
     */
    
    private PersonQuery createQueryForPersons() {
        CriteriaQuery<Person> query = em.getCriteriaBuilder().createQuery(Person.class);
        Root<Person> root = query.from(Person.class);
        
        query.select(root); // <-- select() is optional in EclipseLink (who will assume the Person root)
        
        return new PersonQuery(query, root);
    }
    
    private Predicate createPredicateForNameEquality(Root<Person> personTable, String name) {
        Path<String> column = personTable.get("name");
        return em.getCriteriaBuilder().equal(column, name);
    }
    
    private Predicate createPredicateForAddressEquality(PersonQuery query, Address address) {
        Path<String> column = query.root.get("address");
        return em.getCriteriaBuilder().equal(column, address);
    }
    
    /**
     * Wrapper for a {@code CriteriaQuery<Person>} and the root table.<p>
     * 
     * Purpose of this 2-field class is to make abstraction for criteria query
     * creation possible (see {@code createQueryForPersons()}). We want client
     * code to be able to easily get at both the query and the associated
     * root.<p>
     * 
     * 
     * <i>All</i> roots can be extracted from a criteria query using {@code
     * CriteriaQuery.getRoots()}. That returns a {@code Set} though and require
     * a bit of boilerplate client code to get and use it right.
     */
    private static class PersonQuery {
        final CriteriaQuery<Person> query;
        final Root<Person> root;
        
        PersonQuery(CriteriaQuery<Person> query, Root<Person> root) {
            this.query = query; this.root = root;
        }
    }
}


/*
 * NOTE 1:
 * -------
 * 
 * Receiving an entity from the outside world is always dangerous. When we ask
 * the entity manager to remove a person, the entity need to be "managed" or
 * known by the entity manager.  Meaning that the entity instance need to be in
 * the cache of the entity manager (the "persistence context"). If the entity
 * instance received is not known by the entity manager, the remove call will
 * crash.
 * 
 * We use a container-managed entity manager, which is hinted by the fact that
 * we inject the entity manager using @PersistenceContext (an
 * application-managed entity manager must be fetched from a @PersistenceUnit
 * EntityManagerFactory).
 * 
 * In our PersistenceTest.java file that uses this repository, the calling
 * method that call delete(Person) is not in a transaction. One is begun when
 * delete(Person) is invoked and the same transaction will commit at the end of
 * the method call.
 * 
 * Same goes for all other methods except those explicitly marked
 * @TransactionAttribute(TransactionAttributeType.SUPPORTS).
 * 
 * When that happens (commit), all entities in the current cache will become
 * detached and not managed anymore. Thus even if the Person has participated in
 * another transaction prior to the delete(Person) call, the cache or
 * persistence context within the entity manager will still not know about the
 * provided entity instance. That is the reason we must merge the person, making
 * him managed, before removing him. Merge is an expensive call though and we
 * prefer asking for the entity status before merging.
 * 
 * Go into the source code of @PersistenceContext and you'll see another hint.
 * The entity manager is scoped to the transaction
 * ("PersistenceContextType.TRANSACTION"), kind of just what I said. Can't we
 * just change the scope then to PersistenceContextType.EXTENDED and when the
 * persistence context live beyond the transaction, all problems are solved?
 * Well no. Difference between @Stateless and @Stateful EJB:s is that one is ..
 * stateless and one is not. Using fields of a stateless bean is a big no no
 * because the next time the client invoke a stateless method, the call might be
 * routed to another bean instance. Using an extended entity manager in a
 * stateless bean wouldn't make much sense and is only allowed for @Stateful
 * EJB:s.
 */