package com.martinandersson.javaee.jta.listeners.synchronizationregistry;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@WebServlet(urlPatterns = "/TestDriver")
public class TestDriver extends HttpServlet
{
    @Resource
    ManagedBean managedBean;
    
    @Resource
    TransactionalManagedBean txManagedBean;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final Report report = new Report();
        
        report.managedBeanTXStatus = managedBean.getTransactionStatus();
        report.txManagedBeanTXStatus = txManagedBean.getTransactionStatus();
        
        txManagedBean.registerListeners(
                beforeStatus -> report.txManagedBeanTXStatusBeforeCompletion = beforeStatus,
                afterStatus -> report.txManagedBeanTXStatusAfterCompletion = afterStatus);
        
        new ObjectOutputStream(resp.getOutputStream()).writeObject(report);
    }
    
    static class Report implements Serializable {
        int managedBeanTXStatus,
            txManagedBeanTXStatus,
            txManagedBeanTXStatusBeforeCompletion,
            txManagedBeanTXStatusAfterCompletion;
    }
}