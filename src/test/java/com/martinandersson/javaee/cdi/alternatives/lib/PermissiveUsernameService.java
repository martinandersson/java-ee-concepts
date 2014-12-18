package com.martinandersson.javaee.cdi.alternatives.lib;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;

/**
 * An {@code @Alternative UsernameService} with {@code @Priority 1}.<p>
 * 
 * This service permit all usernames. Good to have as an alternative! For
 * example if we want to run some awesome cool tests that need to replace the
 * default username service. Given that this bean is selected (annotated with
 * {@code @Priority}), the bean should only be packaged and deployed if you
 * actually want this alternative to be used.<p>
 * 
 * Another way of selecting, and thus enabling, an @Alternative is to add his
 * class as an entry to the {@code <alternatives>} node of {@code beans.xml}.
 * {@linkplain com.martinandersson.javaee.cdi.alternatives.UnselectedAlternativeTest UnselectedAlternativeTest}
 * demonstrates that without the {@code @Priority} annotation, and without the
 * bean being listed in {@code beans.xml}, then the bean is safe to deploy
 * together with the rest of your files and will not be used, albeit this
 * practice would probably have no purpose.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Alternative
@Priority(1) // TODO: Add comment whether 1 is high or low.
public class PermissiveUsernameService implements UsernameService
{
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReserved(String username) {
        return false;
    }   
}