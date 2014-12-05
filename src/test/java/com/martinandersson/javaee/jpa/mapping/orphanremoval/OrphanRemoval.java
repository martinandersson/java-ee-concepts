package com.martinandersson.javaee.jpa.mapping.orphanremoval;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 *
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Entity
@Table(name = "ORPHAN_REMOVAL", schema = OrphanRemovalTest.SCHEMA)
public class OrphanRemoval extends AbstractId
{
    // Entity
}