package com.martinandersson.javaee.ejb.sessionbeans.testdriver;

/**
 * Represent an Enterprise JavaBean session bean type. Used by {@linkplain
 * ExecutionSettings} to configure a test run on {@linkplain TestDriver}.<p>
 * 
 * @see Operation
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public enum EJBType {
    SINGLETON, STATEFUL, STATELESS
}