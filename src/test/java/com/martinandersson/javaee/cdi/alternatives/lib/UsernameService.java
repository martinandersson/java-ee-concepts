package com.martinandersson.javaee.cdi.alternatives.lib;

/**
 * The username service currently only declare one method that says whether or
 * not a username is "reserved". A reserved username should not be accepted when
 * creating new users.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public interface UsernameService {
    /**
     * Returns whether or not the provided username is "reserved".<p>
     * 
     * A reserved username should not be accepted when creating new users.
     * 
     * @param username the username
     * @return {@code true} if username is reserved, otherwise {@code false}
     */
    boolean isReserved(String username);
}