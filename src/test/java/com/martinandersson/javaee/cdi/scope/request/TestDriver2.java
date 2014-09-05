package com.martinandersson.javaee.cdi.scope.request;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.ExecutionException;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@WebServlet("/TestDriver2")
public class TestDriver2 extends HttpServlet {
    
    @Inject
    RequestScopedBean requestScopedBean;
    
    @EJB
    StatelessBean statelessBean;
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        
        final int $this = requestScopedBean.getId();
        
        final int nested;
        
        try {
            nested = statelessBean.getIdOfNestedRequestedScopedBeanAsynchronously().get();
        }
        catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        
        Report report = new Report($this, nested);
        
        ObjectOutputStream out = new ObjectOutputStream(resp.getOutputStream());
        out.writeObject(report);
    }
    
    public static final class Report implements Serializable {
    
        final int servletInjectedRequestScopedId,
                  statelessOwnedRequestScopedId;

        Report(int servletInjectedRequestScopedId,
               int statelessOwnedRequestScopedId)
        {
            this.servletInjectedRequestScopedId = servletInjectedRequestScopedId;
            this.statelessOwnedRequestScopedId = statelessOwnedRequestScopedId;
        }
    }
}