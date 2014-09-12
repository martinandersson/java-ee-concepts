package com.martinandersson.javaee.cdi.alternatives;

import com.martinandersson.javaee.cdi.alternatives.lib.UsernameService;
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
 * Will respond to all HTTP GET request with a report that contain the class of
 * the bean used as the default username service.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@WebServlet("/AlternativeDriver")
public class AlternativeDriver extends HttpServlet
{
    @Inject
    UsernameService usernameService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Report<?> report = new Report(usernameService.getClass());
        new ObjectOutputStream(resp.getOutputStream()).writeObject(report);
    }
    
    static class Report<T extends Class<? extends UsernameService>> implements Serializable {
        final T beanType;
        
        Report(T beanType) {
            this.beanType = beanType;
        }
    }
}