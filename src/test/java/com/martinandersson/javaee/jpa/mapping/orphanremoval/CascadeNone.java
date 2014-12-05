package com.martinandersson.javaee.jpa.mapping.orphanremoval;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 *
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Entity
@Table(name = "CASCADE_NONE", schema = OrphanRemovalTest.SCHEMA)
public class CascadeNone extends AbstractId
{
    // Empty
}
