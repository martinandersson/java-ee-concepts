package com.martinandersson.javaee.jpa.mapping.orphanremoval;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 *
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Entity
@Table(name = "CASCADE_REMOVE", schema = OrphanRemovalTest.SCHEMA)
public class CascadeRemove extends AbstractId
{
    // Empty
}