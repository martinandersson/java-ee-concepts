package com.martinandersson.javaee.cdi.specializes;

import com.martinandersson.javaee.cdi.specializes.lib.StupidCalculator;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @see SpecializesTest
 * @see StupidCalculator
 * @see com.martinandersson.javaee.cdi.specializes.lib.SmartCalculator SmartCalculator
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@WebServlet("")
public class SpecializesDriver extends HttpServlet {

    @Inject
    StupidCalculator stupidCalculator;
    
    @Inject
    Event<String> stringEvents;
    
    
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        stringEvents.fire("Hello World");
        Report report = new Report(stupidCalculator.getClass());
        new ObjectOutputStream(resp.getOutputStream()).writeObject(report);
    }
    
    
    
    static class Report <T extends Class<? extends StupidCalculator>> implements Serializable {
        
        final T stupidCalculatorType;
        
        Report(T stupidCalculatorType) {
            this.stupidCalculatorType = stupidCalculatorType;
        }
    }
}