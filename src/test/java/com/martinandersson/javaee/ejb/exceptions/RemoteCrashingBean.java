package com.martinandersson.javaee.ejb.exceptions;

import javax.ejb.Remote;

/**
 *
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Remote
public interface RemoteCrashingBean
{
    /**
     * This method will throw a {@code RuntimeException} with a message set:
     * "123456".
     */
    public void uncheckedSystemException();
    
    /**
     * This method will throw a {@code RuntimeException} with a message set:
     * "123456".
     */
    public void uncheckedSystemException_declared() throws RuntimeException;
    
    /**
     * This method will throw a {@code CustomApplicationException} with a
     * message set: "123456".
     */
    public void uncheckedApplicationException();
    
    /**
     * This method will throw a normal {@code Exception} with a message set:
     * "123456".
     * 
     * @throws Exception always
     */
    public void checkedException() throws Exception;
}