package com.martinandersson.javaee.jpa.entitymanagers.lib;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

/**
 * Utility class with functional factory methods for all CRUD-like operations
 * expected from a {@code Product} repository.<p>
 * 
 * The produced functions are normally used as arguments to a {@code
 * EntityManagerExposer}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public final class Products
{
    private Products() {
        // Empty
    }
    
    public static Function<EntityManager, Product> create(String productName) {
        return em -> {
            Product p = new Product(productName);
            em.persist(p);
            return p;
        };
    }
    
    public static Function<EntityManager, List<Product>> findAll() {
        return em -> {
            CriteriaBuilder b = em.getCriteriaBuilder();
            CriteriaQuery<Product> q = b.createQuery(Product.class);
            
            Path<Product> root = q.from(Product.class);
            
            /*
             * There is no default SELECT clause in the spec (section 6.5.11).
             * EclipseLink default to selecting the FROM clause, or "root". Not
             * that sure what the hell Hibernate does. Best practice is to
             * always include this thing:
             * 
             * TODO: Examine.
             */
            q.select(root);
            
            return em.createQuery(q).getResultList();
        };
    }
    
    public static Function<EntityManager, Product> findById(long id) {
        return em -> {
            return em.find(Product.class, id);
        };
    }
    
    public static Function<EntityManager, Product> findByIdOf(Product product) {
        long id = product.getId();
        
        if (id <= 0) {
            throw new IllegalArgumentException("Product has no id.");
        }
        
        return findById(id);
    }
    
    /**
     * Returns a function that will search for one single product of the
     * provided name, returning {@code null} if none was found.
     * 
     * @param productName name of product
     * 
     * @return a function that will search for one single product of the
     *         provided name, returning {@code null} if none was found
     */
    public static Function<EntityManager, Product> findByUniqueName(String productName) {
        return em -> {
            CriteriaBuilder b = em.getCriteriaBuilder();
            CriteriaQuery<Product> q = b.createQuery(Product.class);

            Root<Product> from = q.from(Product.class);
            q.where(b.equal(from.get(Product.Fields.NAME), productName));
            q.select(from);

            try {
                return em.createQuery(q).getSingleResult();
            }
            catch (NoResultException e) {
                return null;
            }
        };
    }
    
    public static Consumer<EntityManager> remove(Product product) {
        return em -> em.remove(product);
    }
}