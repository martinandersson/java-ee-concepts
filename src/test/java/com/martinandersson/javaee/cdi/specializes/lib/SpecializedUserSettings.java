package com.martinandersson.javaee.cdi.specializes.lib;

import javax.enterprise.inject.Specializes;

/**
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Specializes
public class SpecializedUserSettings implements UserSettings {
    @Override
    public boolean allowMultipleDevices() {
        return true;
    }
}