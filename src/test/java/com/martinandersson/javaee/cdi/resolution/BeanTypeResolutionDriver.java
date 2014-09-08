package com.martinandersson.javaee.cdi.resolution;

import java.io.IOException;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@WebServlet("/BeanTypeResolutionDriver")
public class BeanTypeResolutionDriver extends HttpServlet
{
    @Inject
    SimpleCalculator simpleCalculator;
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Convert all lines in POST body to longs:
        long[] values = req.getReader().lines().mapToLong(Long::parseLong).toArray();
        
        // Sum and return (using the response header):
        long sum = simpleCalculator.sum(values);
        resp.setHeader("sum", String.valueOf(sum));
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        throw new UnsupportedOperationException("Use a POST request.");
    }
}