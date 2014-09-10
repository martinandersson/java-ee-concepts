package com.martinandersson.javaee.cdi.qualifiers;

import com.martinandersson.javaee.cdi.qualifiers.caloric.Healthy;
import com.martinandersson.javaee.cdi.qualifiers.caloric.Caloric;
import com.martinandersson.javaee.cdi.qualifiers.caloric.Unhealthy;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The interface a bean implement, is also part of the bean type. So what if we
 * have many {@code Caloric} beans? Clearly, the CDI container wouldn't know
 * exactly which bean we are looking for. A qualifier can be explicitly set on
 * the injection target (bean) and the injection point to further distinguish
 * between different beans.<p>
 * 
 * So why not just use the implementation type then, for example {@code @Inject
 * Broccoli broccoli}? In software development, we always prefer interface-based
 * programming. The code in this servlet do not depend on a specific
 * implementation. The code is implementation agnostic. What our code do is to
 * declared a semantic dependency of "healthy" and "unhealthy" foods. This
 * annotation may be used in many different places all over the software system.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@WebServlet("/QualifierDriver")
public class QualifierDriver extends HttpServlet
{
    @Inject
    // This injection point is implicitly also annotated: @javax.enterprise.inject.Default
    Caloric water;
    
    @Inject @Healthy
    Caloric broccoli;
    
    @Inject @Unhealthy
    Caloric meat;
    
    
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        
        Report<Class<? extends Caloric>> report = new Report(
                water.getClass(), broccoli.getClass(), meat.getClass());
        
        ObjectOutputStream out = new ObjectOutputStream(resp.getOutputStream());
        out.writeObject(report);
    }
    
    
    
    static class Report<T extends Class<? extends Caloric>> implements Serializable {
        
        final T defaultType, healthyType, unhealthyType;
        
        Report(T defaultType, T healthyType, T unhealthyType) {
            this.defaultType = defaultType;
            this.healthyType = healthyType;
            this.unhealthyType = unhealthyType;
        }
    }
}