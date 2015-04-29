package com.martinandersson.javaee.cdi.lookup;

import java.io.IOException;
import java.io.ObjectOutputStream;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Provider;
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
public class InstanceVersusProviderRunner extends HttpServlet
{
    @Inject
    Instance<RequestScopedBean> instance;
    
    @Inject
    Provider<RequestScopedBean> provider;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        final long firstId,
                   secondId;
        
        switch (CallFirst.valueOf(req.getParameter("callFirst"))) {
            case INSTANCE:
                firstId = instance.get().getId();
                secondId = provider.get().getId();
                break;
            case PROVIDER:
                firstId = provider.get().getId();
                secondId = instance.get().getId();
                break;
            default:
                throw new IllegalArgumentException("callFirst");
        }
        
        new ObjectOutputStream(resp.getOutputStream())
                .writeObject(new long[]{ firstId, secondId });
    }
    
    enum CallFirst {
        INSTANCE, PROVIDER
    }
}