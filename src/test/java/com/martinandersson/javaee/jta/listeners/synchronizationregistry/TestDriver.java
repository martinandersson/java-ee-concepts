package com.martinandersson.javaee.jta.listeners.synchronizationregistry;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

/**
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@WebServlet("")
public class TestDriver extends HttpServlet
{
    @Resource
    ManagedBean managedBean;
    
    @Resource
    TransactionalManagedBean txManagedBean;
    
    @Inject
    UserTransaction tx;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        final Report report = new Report();
        Object response;
        
        report.managedBeanTXStatus = managedBean.getTransactionStatus();
        report.txManagedBeanTXStatus = txManagedBean.getTransactionStatus();
        
        txManagedBean.registerListeners(
                beforeStatus -> report.txManagedBeanTXStatusBeforeCompletion = beforeStatus,
                afterStatus -> report.txManagedBeanTXStatusAfterCompletion = afterStatus);
        
        try {
            tx.begin();
            report.txManagedBeanTxStatusAfterSuspension = txManagedBean.getTransactionStatusAfterSuspension();
            response = report;
        }
        catch (NotSupportedException | SystemException e) {
            response = e;
        }
        finally {
            try {
                tx.commit();
            }
            catch (RollbackException          |
                   HeuristicMixedException    |
                   HeuristicRollbackException |
                   SecurityException          |
                   IllegalStateException      |
                   SystemException e)
            {
                response = report;
            }
        }
        
        new ObjectOutputStream(resp.getOutputStream()).writeObject(response);
    }
    
    static class Report implements Serializable {
        int managedBeanTXStatus,
            txManagedBeanTXStatus,
            txManagedBeanTXStatusBeforeCompletion,
            txManagedBeanTXStatusAfterCompletion,
            txManagedBeanTxStatusAfterSuspension;
    }
}