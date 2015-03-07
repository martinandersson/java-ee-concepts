package com.martinandersson.javaee.ejb.sessionbeans.tests;

import com.martinandersson.javaee.ejb.sessionbeans.AbstractSessionBean;
import com.martinandersson.javaee.ejb.sessionbeans.testdriver.EJBType;
import com.martinandersson.javaee.ejb.sessionbeans.testdriver.ExecutionSettings;
import com.martinandersson.javaee.ejb.sessionbeans.testdriver.Operation;
import com.martinandersson.javaee.ejb.sessionbeans.testdriver.Report;
import com.martinandersson.javaee.ejb.sessionbeans.testdriver.TestDriver;
import com.martinandersson.javaee.utils.DeploymentBuilder;
import com.martinandersson.javaee.utils.HttpRequests;
import com.martinandersson.javaee.utils.PhasedExecutorService;
import java.net.URL;
import java.util.Objects;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.runner.RunWith;

/**
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
abstract class AbstractSessionTest
{
    @Deployment
    private static Archive<?> buildDeployment() {
        return new DeploymentBuilder(AbstractSessionTest.class)
                .add(true, AbstractSessionBean.class, TestDriver.class)
                .add(PhasedExecutorService.class)
                .build();
    }
    
    private final EJBType type;
    
    @ArquillianResource
    private URL url;
    
    AbstractSessionTest(EJBType type) {
        this.type = Objects.requireNonNull(type);
    }
    
    protected final Report run(Operation operation) {
        ExecutionSettings settings = new ExecutionSettings(operation, type);
        return HttpRequests.sendGetObject(url, null, settings);
    }
}