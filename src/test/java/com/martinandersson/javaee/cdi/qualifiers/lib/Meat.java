package com.martinandersson.javaee.cdi.qualifiers.lib;

/**
 * Meat is murder. Or producer of life in large farms. Depends on how you look
 * at things. In this application, we assume that eating meat is bad for human
 * health.<p>
 * 
 * This class is annotated with {@code @Unhealthy}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Unhealthy
public class Meat implements Caloric {
    
    /**
     * Any piece of meat has exactly 1 000 calories.
     * 
     * @return {@code 1_000}
     */
    @Override
    public int getCalories() {
        return 1_000;
    }
}