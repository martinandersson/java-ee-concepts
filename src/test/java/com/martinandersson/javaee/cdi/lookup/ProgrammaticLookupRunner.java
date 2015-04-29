package com.martinandersson.javaee.cdi.lookup;

import java.io.IOException;
import java.io.ObjectOutputStream;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("")
public class ProgrammaticLookupRunner extends HttpServlet
{
    private final RequestScopedBean requestScoped
            = CDI.current().select(RequestScopedBean.class).get();
    
    private final ApplicationScopedBean applicationScoped
            = CDI.current().select(ApplicationScopedBean.class).get();
    
    private final DependentBean dependentBean1
            = CDI.current().select(DependentBean.class).get();
    
    private final DependentBean dependentBean2
            = CDI.current().select(DependentBean.class).get();
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(resp.getOutputStream())) {
            out.writeObject(new long[]{
                requestScoped.getId(),
                applicationScoped.getId(),
                dependentBean1.getId(),
                dependentBean2.getId()});
        }
    }
}