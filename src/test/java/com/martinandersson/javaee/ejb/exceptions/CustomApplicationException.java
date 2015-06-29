package com.martinandersson.javaee.ejb.exceptions;

import javax.ejb.ApplicationException;

/**
 *
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@ApplicationException
public class CustomApplicationException extends RuntimeException
{
    public CustomApplicationException(String msg) {
        super(msg);
    }
}