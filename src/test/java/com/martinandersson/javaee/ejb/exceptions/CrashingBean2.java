package com.martinandersson.javaee.ejb.exceptions;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;

/**
 *
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Stateless
@LocalBean
public class CrashingBean2 implements RemoteCrashingBean
{
    /** {@inheritDoc} */
    @Override
    public void uncheckedSystemException() {
        throw new RuntimeException("123456");
    }
    
    /** {@inheritDoc} */
    @Override
    public void uncheckedSystemException_declared() throws RuntimeException {
        throw new RuntimeException("123456");
    }
    
    /** {@inheritDoc} */
    @Override
    public void uncheckedApplicationException() {
        throw new CustomApplicationException("123456");
    }
    
    /** {@inheritDoc} */
    @Override
    public void checkedException() throws Exception {
        throw new Exception("123456");
    }
}