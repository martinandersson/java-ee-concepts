package com.martinandersson.javaee.cdi.specializes.lib;

/**
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class DefaultUserSettings implements UserSettings {
    @Override
    public boolean allowMultipleDevices() {
        return false;
    }
}