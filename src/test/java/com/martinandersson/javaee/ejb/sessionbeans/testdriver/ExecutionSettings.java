package com.martinandersson.javaee.ejb.sessionbeans.testdriver;

import java.io.Serializable;

/**
 * Configuration for a test executed by {@linkplain TestDriver}.
 * 
 * @see Operation
 * @see EJBType
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class ExecutionSettings implements Serializable {
    private final Operation operation;
    private final EJBType type;
    
    public ExecutionSettings(Operation operation, EJBType type) {
        this.operation = operation;
        this.type = type;
    }
    
    public Operation getOperation() {
        return operation;
    }
    
    public EJBType getEJBType() {
        return type;
    }

    @Override
    public String toString() {
        return new StringBuilder(ExecutionSettings.class.getSimpleName())
                .append("[")
                  .append("operation=").append(operation)
                  .append(", type=").append(type)
                .append("]")
                .toString();
    }
}