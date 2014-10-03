package com.martinandersson.javaee.ejb.sessionbeans.testdriver;

/**
 * Defines how selected EJB references shall be invoked to fetch their bean id.<p>
 * 
 * Current values:
 * 
 * <ul>
 *   <li>{@linkplain #CALL_TWO_SERIALLY}</li>
 *   <li>{@linkplain #CALL_ONE_SERIALLY}</li>
 *   <li>{@linkplain #CALL_ONE_CONCURRENTLY}</li>
 *   <li>{@linkplain #SELF_INVOKING_PROXY}</li>
 * </ul>
 * 
 * @see EJBType
 * @see ExecutionSettings
 * @see TestDriver
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public enum Operation {
    
    /**
     * Makes the driver call his two injected session bean references serially
     * using the same thread.
     */
    CALL_TWO_SERIALLY,

    /**
     * Makes the driver call just one injected session bean reference serially
     * using the same thread.
     */
    CALL_ONE_SERIALLY,
    
    /**
     * Will make the driver call one of his session bean references concurrently
     * from two different threads.
     */
    CALL_ONE_CONCURRENTLY,
    
    /**
     * Will make the driver call one of his session bean references from within
     * the same bean.
     */
    SELF_INVOKING_PROXY
}