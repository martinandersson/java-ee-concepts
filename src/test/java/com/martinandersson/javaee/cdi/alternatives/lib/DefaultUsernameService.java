package com.martinandersson.javaee.cdi.alternatives.lib;

/**
 * A {@code @Default} username service.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
// Is implicitly: @javax.enterprise.inject.Default
public class DefaultUsernameService implements UsernameService
{
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReserved(String username) {
        return !"admin".equalsIgnoreCase(username);
    }
}