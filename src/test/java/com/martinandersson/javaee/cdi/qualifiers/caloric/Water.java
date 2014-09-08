package com.martinandersson.javaee.cdi.qualifiers.caloric;

/**
 * This bean has no explicitly put qualifiers, therefore, the water bean will
 * get a built-in default qualifier {@code @Default}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
// Implicitly: @javax.enterprise.inject.Default
public class Water implements Caloric {

    /**
     * Water has zero calories.
     * 
     * @return {@code 0}
     */
    @Override
    public int getCalories() {
        return 0;
        
        // Or:
        // return Caloric.super.getCalories();
        
        // Or:
        // Don't implement this method at all.
    }
}