package com.martinandersson.javaee.arquillian.clientserver;

import com.martinandersson.javaee.arquillian.helloworld.HelloWorldEJB;
import java.io.IOException;
import java.util.concurrent.atomic.LongAdder;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A client accessible server interface, kind of.<p>
 * 
 * The Servlet accept only GET requests with a "hello" parameter whose value
 * will be returned in upper case.<p>
 * 
 * There exists a non-academic tradition of defining a remote interface to
 * automatically be a "facade" just because it is "remote". However, a remote
 * interface is a remote interface. The interface is in no way required to be a
 * facade for something else. A facade is by definition an aggregation of other
 * interfaces/API:s into one simplified interface; a higher level abstraction if
 * you will. A facade may or may not be remote.<p>
 * 
 * This class has nothing to do with EJB remote interfaces, nor is it a facade.
 * I wouldn't actually call this class anything. At worst, one could describe
 * the {@code ServerAPI} as a HTTP-based adapter for the {@code HelloWorldEJB}
 * bean. However, name calling in this context serves little purpose and should
 * be regarded as blasphemi.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@WebServlet(urlPatterns = "/ServerAPI")
public class ServerAPI extends HttpServlet
{
    /*
     * Only one ServerAPI Servlet exist in the server and it is not thread-safe.
     * Therefore we use LongAdder which is a concurrent and lock-free counter (note 1).
     */
    
    private static final LongAdder requestCounter = new LongAdder();
    
    public static long getRequestCount() {
        return requestCounter.sum();
    }
    
    
    /*
     * All calls to EJB:s are serialized, the only exception is @Singleton with
     * bean managed concurrency, or @Singleton with container managed
     * concurrency and @Lock(LockType.READ). See EJB 3.2 specification, section
     * 3.4.9.
     */
    
    @EJB
    HelloWorldEJB helloWorldEJB;
    
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        requestCounter.increment();
        super.service(req, resp);
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        final String world = req.getParameter("hello");
        
        if (world == null) {
            reply("Missing the \"hello\" parameter :'(", HttpServletResponse.SC_BAD_REQUEST, resp);
        }
        else {
            String WORLD = helloWorldEJB.toUpperCase(world);
            reply(WORLD, HttpServletResponse.SC_OK, resp);
        }
    }
    
    private void reply(String message, int status, HttpServletResponse resp) throws IOException
    {
        /*
         * If SC_BAD_REQUEST, one would perhaps like to use sendError() instead
         * of setStatus(). But that would allow the server to also output a
         * vendor-specific HTML error page (!).
         */
        
        resp.setStatus(status);
        
        resp.setContentType("text/plain"); // or "text/html;charset=UTF-8" and skip next statement
        resp.setCharacterEncoding("UTF-8");
        resp.setContentLength(message.length()); // if the Servlet do buffering, which most do, they can infer the output length without this call
        
        resp.getWriter().write(message);
    }
}



/*
 * NOTE 1: LongAdder was added in JDK 8. Earlier JDK:s must find a sharded
 *         counter from a library or write their own. In worst case, use
 *         AtomicLong.
 */