package com.martinandersson.javaee.cdi.scope.request;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
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
@WebServlet("/TestDriver1")
public class TestDriver1 extends HttpServlet {
    
    @Inject
    RequestScopedBean requestScopedBean;
    
    @Inject
    ApplicationScopedBean applicationScopedBean;

    @EJB
    StatelessBean statelessBean;
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        
        // Used by concurrent tests:
        if ("true".equalsIgnoreCase(req.getParameter("sleep"))) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        
        Report report = new Report(
                requestScopedBean.getId(),
                requestScopedBean.getIdOfNestedRequestedScopedBean(),
                applicationScopedBean.getIdOfNestedRequestedScopedBean(),
                statelessBean.getIdOfNestedRequestedScopedBean());
        
        ObjectOutputStream out = new ObjectOutputStream(resp.getOutputStream());
        out.writeObject(report);
    }
    
    public static final class Report implements Serializable {
    
        final int servletInjectedRequestScopedId,
                  selfNestedRequestScopedId,
                  singletonOwnedRequestScopedId,
                  statelessOwnedRequestScopedId;

        Report(int servletInjectedRequestScopedId,
               int selfNestedRequestScopedId,
               int singletonOwnedRequestScopedId,
               int statelessOwnedRequestScopedId)
        {
            this.servletInjectedRequestScopedId = servletInjectedRequestScopedId;
            this.selfNestedRequestScopedId = selfNestedRequestScopedId;
            this.singletonOwnedRequestScopedId = singletonOwnedRequestScopedId;
            this.statelessOwnedRequestScopedId = statelessOwnedRequestScopedId;
        }
    }
}