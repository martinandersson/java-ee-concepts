package com.martinandersson.javaee.cdi.qualifiers;

import com.martinandersson.javaee.cdi.qualifiers.caloric.Healthy;
import com.martinandersson.javaee.cdi.qualifiers.caloric.Caloric;
import com.martinandersson.javaee.cdi.qualifiers.caloric.Broccoli;
import com.martinandersson.javaee.cdi.qualifiers.caloric.Unhealthy;
import com.martinandersson.javaee.cdi.qualifiers.caloric.Meat;
import com.martinandersson.javaee.cdi.qualifiers.caloric.Water;
import com.martinandersson.javaee.cdi.qualifiers.QualifierDriver.Report;
import com.martinandersson.javaee.utils.Deployments;
import com.martinandersson.javaee.utils.HttpRequests;
import java.net.URL;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * There are many 
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
public class QualifierTest {
    
    @Deployment
    public static WebArchive buildDeployment() {
        return Deployments.buildCDIBeanArchive(
                
                // Test class and driver
                QualifierTest.class,
                QualifierDriver.class,
                
                // Qualifiers
                Healthy.class,
                Unhealthy.class,
                
                // Common bean type
                Caloric.class,
                
                // Carolic implementations
                Water.class,
                Broccoli.class,
                Meat.class);
    }
    
    @Test
    @RunAsClient
    public void qualifierTest(@ArquillianResource URL url) {
        Report<Class<? extends Caloric>> report = HttpRequests.getObject(url, QualifierDriver.class);
        
        assertEquals("Expected that Water is the only @Default Caloric bean.",
                Water.class, report.defaultType);
        
        assertEquals("Expected that Broccoli is the only @Healthy Caloric bean.",
                Broccoli.class, report.healthyType);
        
        
        assertEquals("Expected that Meat is the only @Unhealthy Caloric bean.",
                Meat.class, report.unhealthyType);
    }
}