package com.martinandersson.javaee.cdi.alternatives.lib;

import javax.enterprise.inject.Alternative;

/**
 * Is a {@code @Alternative UsernameService}. Must be explicitly selected in the
 * {@code beans.xml} file or with a {@linkplain javax.annotation.Priority @Priority}
 * annotation. See {@linkplain PermissiveUsernameService} for an example of the
 * latter.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Alternative
public class UnselectedUsernameService implements UsernameService
{
    @Override
    public boolean isReserved(String username) {
        throw new UnsupportedOperationException("For demonstration purposes only.");
    }
}