package com.martinandersson.javaee.cdi.qualifiers.caloric;

/**
 * A {@code Caloric} class is a unit of container for any number of calories,
 * including negative values or zero.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public interface Caloric {
    /**
     * Returns the number of calories this {@code Caloric} entity stores.
     * 
     * @return the number of calories this {@code Caloric} entity stores
     */
    default int getCalories() { return 0; };
}