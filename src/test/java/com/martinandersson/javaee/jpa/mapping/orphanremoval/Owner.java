package com.martinandersson.javaee.jpa.mapping.orphanremoval;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 *
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Entity
@Table(schema = OrphanRemovalTest.SCHEMA)
public class Owner extends AbstractId
{
    @OneToMany(fetch = FetchType.EAGER)
    @JoinTable(schema = OrphanRemovalTest.SCHEMA)
    private Set<CascadeNone> nones = new HashSet<>();
    
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.REMOVE)
    @JoinTable(schema = OrphanRemovalTest.SCHEMA)
    private Set<CascadeRemove> removes = new HashSet<>();
    
    @OneToMany(fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinTable(schema = OrphanRemovalTest.SCHEMA)
    private Set<OrphanRemoval> orphans = new HashSet<>();
    
    
    
    public Set<CascadeNone> getCascadeNones() {
        return Collections.unmodifiableSet(nones);
    }
    
    public Set<CascadeRemove> getCascadeRemoves() {
        return Collections.unmodifiableSet(removes);
    }
    
    public Set<OrphanRemoval> getOrphanRemovals() {
        return Collections.unmodifiableSet(orphans);
    }
    
    
    
    public boolean addCascadeNone(CascadeNone none) {
        return nones.add(Objects.requireNonNull(none));
    }
    
    public boolean addCascadeRemove(CascadeRemove remove) {
        return removes.add(Objects.requireNonNull(remove));
    }
    
    public boolean addOrphanRemoval(OrphanRemoval orphan) {
        return orphans.add(Objects.requireNonNull(orphan));
    }
    
    
    
    public boolean removeCascadeNone(CascadeNone none) {
        return nones.remove(none);
    }
    
    public boolean removeCascadeRemove(CascadeRemove remove) {
        return removes.remove(remove); // <-- lol wtf.
    }
    
    public boolean removeOrphanRemoval(OrphanRemoval orphan) {
        return orphans.remove(orphan);
    }

    @Override
    public String toString() {
        return new StringBuilder(Owner.class.getSimpleName())
                .append('[')
                  .append("nones=").append(nones)
                  .append(", removes=").append(removes)
                  .append(", orphans=").append(orphans)
                .append(']')
                .toString();
    }
}