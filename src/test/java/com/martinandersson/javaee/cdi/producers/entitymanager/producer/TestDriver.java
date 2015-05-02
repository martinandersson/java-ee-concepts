package com.martinandersson.javaee.cdi.producers.entitymanager.producer;

import static com.martinandersson.javaee.cdi.producers.entitymanager.producer.Scope.Type.DEPENDENT;
import static com.martinandersson.javaee.cdi.producers.entitymanager.producer.Scope.Type.REQUEST;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.AnnotationLiteral;
import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@WebServlet("")
public class TestDriver extends HttpServlet
{
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        System.out.println("Dependent proof: " + lookup(DEPENDENT));
        System.out.println("Request proof: " + lookup(REQUEST));
        
        Map<Scope.Type, Integer> instances = new EnumMap<>(Scope.Type.class);
        instances.put(DEPENDENT, DependentEMProducer.INSTANCE_COUNTER.get());
        instances.put(REQUEST, RequestScopedEMProducer.INSTANCE_COUNTER.get());
        
        Map<Scope.Type, List<RuntimeException>> exceptions = new EnumMap<>(Scope.Type.class);
        exceptions.put(DEPENDENT, new ArrayList<>(DependentEMProducer.CLOSE_EXCEPTIONS));
        exceptions.put(REQUEST, new ArrayList<>(RequestScopedEMProducer.CLOSE_EXCEPTIONS));
        
        new ObjectOutputStream(resp.getOutputStream())
                .writeObject(new Report(instances, exceptions));
    }
    
    private EntityManager lookup(Scope.Type type) {
        return CDI.current()
                .select(EntityManager.class, ScopeAnnotation.of(type))
                .get();
    }
    
    static class Report implements Serializable {
        final Map<Scope.Type, Integer> instancesCreated;
        final Map<Scope.Type, List<RuntimeException>> closeExceptions;
        
        Report(Map<Scope.Type, Integer> instancesCreated,
               Map<Scope.Type, List<RuntimeException>> closeExceptions)
        {
            this.instancesCreated = instancesCreated;
            this.closeExceptions = closeExceptions;
        }
    }
    
    static abstract class ScopeAnnotation extends AnnotationLiteral<Scope> implements Scope {
        public static AnnotationLiteral<Scope> of(Scope.Type type) {
            return new ScopeAnnotation() {
                @Override public Type value() {
                    return type;
                }
            };
        }
    }
}