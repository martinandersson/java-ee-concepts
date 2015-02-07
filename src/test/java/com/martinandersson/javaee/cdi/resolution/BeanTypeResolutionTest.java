package com.martinandersson.javaee.cdi.resolution;

import com.martinandersson.javaee.utils.DeploymentBuilder;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A common myth about CDI is that CDI can only inject interface types. Which
 * isn't true. Using the real class type at the injection point work flawlessly.
 * In fact, type is the number one "bean qualifier" that makes type safe
 * resolution possible.<p>
 * 
 * Note that in this deployment, there exists only one {@code SimpleCalculator}.
 * Had there also existed a subclass thereof, then the injection point in
 * {@code BeanTypeResolutionDriver} will represent an ambiguous dependency: two
 * bean targets match. Such deployment will almost always fail.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
public class BeanTypeResolutionTest {
    
    @Deployment
    public static WebArchive buildDeployment() {
        return new DeploymentBuilder(BeanTypeResolutionTest.class)
                .addEmptyBeansXMLFile()
                .add(BeanTypeResolutionDriver.class,
                     SimpleCalculator.class)
                .build();
    }
    
    /**
     * Will make a HTTP POST request to {@code TypeResolutionDriver}-servlet
     * which use a calculator that do not implement an interface. The calculator
     * is a POJO as much as a POJO can be.
     * 
     * @param url application url, provided by Arquillian
     * 
     * @throws MalformedURLException if things go to hell
     * @throws IOException if things go to hell
     */
    @Test
    @RunAsClient
    public void useSimpleCalculator(@ArquillianResource URL url) throws MalformedURLException, IOException {
        final URL testDriver = new URL(url, BeanTypeResolutionDriver.class.getSimpleName());
        final HttpURLConnection conn = (HttpURLConnection) testDriver.openConnection();
        
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "UTF-8");
        conn.setRequestProperty("Connection", "close");
        
        final long sum;
        
        try (OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8))
        {
            // Try sum 5 + 5
            out.write("5");
            out.write("\r\n");
            out.write("5");
            
            out.flush();
            sum = Long.parseLong(conn.getHeaderField("sum"));
        }
        
        assertEquals("Expected that 5 + 5 equals 10", 10, sum);
    }
}