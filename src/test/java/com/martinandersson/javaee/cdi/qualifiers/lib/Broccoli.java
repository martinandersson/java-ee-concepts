package com.martinandersson.javaee.cdi.qualifiers.lib;

/**
 * Broccoli is healthy for you!<p>
 * 
 * This class is annotated with {@code @Healthy}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Healthy
public class Broccoli implements Caloric
{    
    /**
     * A broccoli has exactly 2 calories.
     * 
     * @return {@code 2}
     */
    @Override
    public int getCalories() {
        return 2;
    }
}