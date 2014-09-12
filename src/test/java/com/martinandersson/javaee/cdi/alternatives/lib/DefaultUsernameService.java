package com.martinandersson.javaee.cdi.alternatives.lib;

/**
 * A {@code @Default} username service.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
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