package com.martinandersson.javaee.ejb.sessionbeans.testdriver;

import java.io.Serializable;

/**
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class Report implements Serializable {
    public final int beanId1, beanId2;
    public final Exception exception; // JDK 8's Optional is not Serializable.
    
    public Report(int beanId1, int beanId2) {
        this.beanId1 = beanId1;
        this.beanId2 = beanId2;
        this.exception = null;
    }
    
    public Report(Exception exception) {
        beanId1 = beanId2 = -1;
        this.exception = exception;
    }

    @Override
    public String toString() {
        return new StringBuilder(Report.class.getSimpleName())
                .append("[")
                  .append("beanId1=").append(beanId1)
                  .append(", beanId2=").append(beanId2)
                  .append(", exception=").append(exception)
                .append("]")
                .toString();
    }
}